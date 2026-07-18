/**
 * Bot quest scenario for SimpleQuest integration tests.
 *
 * This script runs a bot through the full quest lifecycle:
 *   1. Connect to server
 *   2. Open quest selection GUI
 *   3. Interact with quest items
 *   4. Attempt to complete objectives (break blocks)
 *   5. Verify quest completion via chat messages
 *
 * Usage: npx tsx src/quest-scenario.ts [port] [questType]
 *
 * Environment:
 *   SQS_PORT=25565          - Server port (default: 25565)
 *   SQS_QUEST_TYPE=BotQuest - Quest type key (default: BotQuest)
 *   SQS_BOT_USERNAME=QuestBot - Bot username (default: QuestBot_<random>)
 */
import { createBot, type Bot } from "mineflayer";

// Window type from mineflayer internals (prismarine-windows)
interface BotWindow {
  title: unknown;
  slots: Record<number, unknown>;
}

// ---- Types ----

interface WindowSlotItem {
	name: string;
	count: number;
	displayName?: string;
	nbt?: unknown;
}

interface WindowInfo {
	title: string;
	slotCount: number;
	slots: Record<number, WindowSlotItem>;
}

interface QuestScenarioResult {
	server: string;
	questGuiOpened: boolean;
	questGuiTitle: string;
	questItemCount: number;
	questItems: string[];
	chatMessages: string[];
	blockBroken: number;
	completed: boolean;
	duration: number;
}

// ---- Config ----

const PORT = parseInt(process.env.SQS_PORT ?? "25565", 10);
const BOT_USERNAME =
	process.env.SQS_BOT_USERNAME ?? `QuestBot_${Date.now() % 10000}`;

// ---- Logger ----

const log = (label: string, msg: string) => {
	const ts = new Date().toISOString().split("T")[1].slice(0, 12);
	console.log(`[${ts}][${label}] ${msg}`);
};

// ---- Bot Connection ----

async function connectBot(): Promise<Bot> {
	return new Promise<Bot>((resolve, reject) => {
		const bot = createBot({
			host: "localhost",
			port: PORT,
			username: BOT_USERNAME,
			auth: "offline",
		});

		const timeout = setTimeout(() => {
			reject(new Error(`Bot connection timeout after 30s`));
		}, 30000);

		bot.once("spawn", () => {
			clearTimeout(timeout);
			log("CONNECT", `Bot spawned as '${bot.username}' on port ${PORT}`);
			resolve(bot);
		});

		bot.on("error", (err: Error) => {
			clearTimeout(timeout);
			console.error(`[ERROR] ${err.message}`);
			reject(err);
		});
	});
}

// ---- GUI helpers ----

async function waitForWindow(bot: Bot, timeoutMs = 15000): Promise<WindowInfo> {
	return new Promise((resolve, reject) => {
		const timeout = setTimeout(() => {
			reject(new Error(`GUI window did not open within ${timeoutMs}ms`));
		}, timeoutMs);

		bot.once("windowOpen", (window) => {
			clearTimeout(timeout);
			const slots: Record<number, WindowSlotItem> = {};
			for (const [slotIdx, item] of Object.entries(window.slots)) {
				const idx = parseInt(slotIdx, 10);
				if (item != null && typeof item === "object" && "name" in item) {
					const typed = item as {
						name: string;
						count: number;
						displayName?: unknown;
						nbt?: unknown;
					};
					slots[idx] = {
						name: typed.name,
						count: typed.count,
						displayName:
							typed.displayName == null
								? undefined
								: String(typed.displayName),
						nbt: typed.nbt,
					};
				}
			}
			const title = String(window.title ?? "");
			log(
				"GUI",
				`Opened "${title}" (${Object.keys(slots).length} items)`,
			);

			// Log all items
			for (const [slot, item] of Object.entries(slots)) {
				const dn = item.displayName ? ` [${item.displayName}]` : "";
				log("GUI", `  Slot ${slot}: ${item.name} x${item.count}${dn}`);
			}

			resolve({
				title,
				slotCount: Object.keys(slots).length,
				slots,
			});
		});
	});
}

function clickSlot(bot: Bot, window: BotWindow, slot: number): void {
	log("GUI", `Clicking slot ${slot}`);
	// Use simpleClick for chest-style GUIs
	try {
		(bot as any).simpleClick?.(window, slot, 0, 0);
	} catch {
		// Fallback: try direct click
		try {
			bot.clickWindow(slot, 0, 0);
		} catch (e) {
			log("GUI", `Click failed: ${(e as Error).message}`);
		}
	}
}

// ---- Block Breaking (Objective) ----

/**
 * Finds the nearest block of the given type near the bot.
 */
function findNearbyBlock(
	bot: Bot,
	blockName: string,
	radius: number = 8,
): { x: number; y: number; z: number } | null {
	const pos = bot.entity.position;
	for (let dy = radius; dy >= -radius; dy--) {
		for (let dx = -radius; dx <= radius; dx++) {
			for (let dz = -radius; dz <= radius; dz++) {
				const bx = Math.floor(pos.x) + dx;
				const by = Math.floor(pos.y) + dy;
				const bz = Math.floor(pos.z) + dz;
				const Vec3 = (bot as any).registry?.Vec3;
				const blockPos = Vec3 ? new Vec3(bx, by, bz) : { x: bx, y: by, z: bz };
				const block = bot.blockAt(blockPos);
				if (block && block.name === blockName) {
					return { x: bx, y: by, z: bz };
				}
			}
		}
	}
	return null;
}

/**
 * Attempts to break one block of the given type.
 * Returns true if successful.
 */
async function breakOneBlock(
	bot: Bot,
	blockName: string,
): Promise<boolean> {
	const pos = findNearbyBlock(bot, blockName);
	if (!pos) {
		log("BREAK", `No ${blockName} found nearby`);
		return false;
	}

	const Vec3 = (bot as any).registry?.Vec3;
	const blockPos = Vec3 ? new Vec3(pos.x, pos.y, pos.z) : pos;
	const block = bot.blockAt(blockPos);

	if (!block) {
		log("BREAK", `Block at ${pos.x},${pos.y},${pos.z} is null`);
		return false;
	}

	// Check if we can harvest with current tool
	if (!bot.canDigBlock(block)) {
		log("BREAK", `Cannot break ${block.name} at ${pos.x},${pos.y},${pos.z} — need tool?`);
		return false;
	}

	log("BREAK", `Breaking ${block.name} at ${pos.x},${pos.y},${pos.z}`);
	try {
		await bot.dig(block, true);
		log("BREAK", `Successfully broke ${block.name}`);
		return true;
	} catch (e) {
		log("BREAK", `Failed to break: ${(e as Error).message}`);
		return false;
	}
}

/**
 * Breaks multiple blocks of the given type.
 */
async function breakBlocks(
	bot: Bot,
	blockName: string,
	count: number,
): Promise<number> {
	let broken = 0;
	for (let i = 0; i < count; i++) {
		// Re-scan nearby blocks after each break (position changes)
		const success = await breakOneBlock(bot, blockName);
		if (success) {
			broken++;
			// Small delay for server to process
			await sleep(500);
		} else {
			// Try moving sideways to find more blocks
			const dirs = [
				{ x: 3, z: 0 },
				{ x: -3, z: 0 },
				{ x: 0, z: 3 },
				{ x: 0, z: -3 },
				{ x: 3, z: 3 },
				{ x: -3, z: -3 },
			];
			const moved = false;
			for (const d of dirs) {
				const target = bot.entity.position.offset(d.x, 0, d.z);
				bot.lookAt(target, true);
				await sleep(300);
				const retry = await breakOneBlock(bot, blockName);
				if (retry) {
					broken++;
					await sleep(500);
					break;
				}
			}
			if (!moved) {
				log("BREAK", `Giving up after ${broken}/${count} breaks`);
				break;
			}
		}
	}
	return broken;
}

function sleep(ms: number): Promise<void> {
	return new Promise((resolve) => setTimeout(resolve, ms));
}

// ---- Chat tracking ----

function trackChatMessages(bot: Bot, messages: string[]): void {
	bot.on("message", (jsonMsg) => {
		const text = String(jsonMsg);
		messages.push(text);
		log("CHAT", text.substring(0, 120));
	});
}

// ---- Main Scenario ----

async function runQuestScenario(): Promise<QuestScenarioResult> {
	const startTime = Date.now();
	const result: QuestScenarioResult = {
		server: `localhost:${PORT}`,
		questGuiOpened: false,
		questGuiTitle: "",
		questItemCount: 0,
		questItems: [],
		chatMessages: [],
		blockBroken: 0,
		completed: false,
		duration: 0,
	};

	const bot = await connectBot();
	trackChatMessages(bot, result.chatMessages);

	// ----- Step 1: Open quest selection GUI -----
	log("STEP", "1. Opening quest selection GUI...");
	bot.chat("/simplequest quest");

	try {
		const window = await waitForWindow(bot);
		result.questGuiOpened = true;
		result.questGuiTitle = window.title;
		result.questItemCount = window.slotCount;

		// Collect quest item names
		for (const item of Object.values(window.slots)) {
			const name = item.displayName ?? item.name;
			result.questItems.push(name);
		}

		log("STEP", `Quest GUI has ${window.slotCount} items: ${result.questItems.join(", ")}`);

		// ----- Step 2: Try to click the first quest item -----
		if (window.slotCount > 0) {
			log("STEP", "2. Clicking first quest item...");
			// Find the first slot with actual content (skip frame/decoration slots)
			const firstItemSlot = Object.keys(window.slots)
				.map(Number)
				.sort((a, b) => a - b)
				.find((s) => window.slots[s] != null) ?? 0;

			// Need to reference the actual window object from mineflayer
			// We use bot.currentWindow or bot.window
			const currentWindow = (bot as any).currentWindow;
			if (currentWindow) {
				clickSlot(bot, currentWindow, firstItemSlot);
			}

			// Wait for any subsequent window (confirmation/quest detail)
			await sleep(2000);
		}
	} catch (e) {
		log("ERROR", `GUI failed: ${(e as Error).message}`);
	}

	// ----- Step 3: Attempt to break stone blocks (quest objective) -----
	log("STEP", "3. Attempting to break stone blocks for quest objective...");
	const stoneBroken = await breakBlocks(bot, "stone", 5);
	result.blockBroken = stoneBroken;
	log("STEP", `Broke ${stoneBroken}/5 stone blocks`);

	// ----- Step 4: Check for completion messages -----
	await sleep(2000);
	const completedPatterns = [
		/completed.*quest/i,
		/quest completed/i,
		/Quest completed/i,
	];
	for (const msg of result.chatMessages) {
		if (completedPatterns.some((p) => p.test(msg))) {
			result.completed = true;
			log("COMPLETE", `Found completion message: ${msg}`);
			break;
		}
	}

	result.duration = Date.now() - startTime;

	// ----- Report -----
	log("RESULT", "========================================");
	log("RESULT", `Quest GUI: ${result.questGuiOpened ? "OPENED" : "FAILED"}`);
	log("RESULT", `GUI Title: "${result.questGuiTitle}"`);
	log("RESULT", `Quest items: ${result.questItems.length}`);
	log("RESULT", `Blocks broken: ${result.blockBroken}`);
	log("RESULT", `Quest completed: ${result.completed}`);
	log("RESULT", `Duration: ${result.duration}ms`);
	log("RESULT", "========================================");

	bot.quit();
	log("DISCONNECT", "Bot disconnected");
	return result;
}

// ---- Entry point ----

async function main(): Promise<void> {
	try {
		const result = await runQuestScenario();
		console.log(JSON.stringify({ result, status: "ok" }));
	} catch (err) {
		console.error(
			JSON.stringify({
				error: String(err),
				status: "error",
			}),
		);
		process.exit(1);
	}
}

main();
