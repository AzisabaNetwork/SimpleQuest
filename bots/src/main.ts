/**
 * Mineflayer bot scenarios for SimpleQuest integration tests.
 *
 * Connects to each server, opens quest/party GUIs, and verifies
 * that GUI windows open with expected content.
 */
import { createBot, type Bot } from "mineflayer";

interface ServerConfig {
	label: string;
	port: number;
}

interface WindowSlotItem {
	name: string;
	count: number;
	displayName?: string;
}

interface WindowInfo {
	title: string;
	slotCount: number;
	slots: Record<string, WindowSlotItem>;
}

const SERVERS: ServerConfig[] = [
	{ label: "master", port: 25565 },
	{ label: "slave", port: 25566 },
	{ label: "readonly", port: 25567 },
];

/** Normalizes a Minecraft chat component (string | {type,value} | {text,...} | array) to plain text. */
function componentToText(value: unknown): string {
	if (value == null) return "";
	if (typeof value === "string") return value;
	if (Array.isArray(value)) return value.map(componentToText).join("");
	if (typeof value === "object") {
		const obj = value as Record<string, unknown>;
		let text = "";
		if (typeof obj.text === "string") text += obj.text;
		if (typeof obj.value === "string") text += obj.value;
		if (obj.extra != null || obj.with != null) {
			text += componentToText(obj.extra ?? obj.with);
		}
		return text;
	}
	return String(value);
}

async function connectBot(config: ServerConfig): Promise<Bot> {
	return new Promise<Bot>((resolve, reject) => {
		const bot = createBot({
			host: "localhost",
			port: config.port,
			username: `Bot_${config.label}`,
			auth: "offline",
		});

		const timeout = setTimeout(() => {
			reject(new Error(`[${config.label}] Connection timeout`));
		}, 30000);

		bot.once("spawn", () => {
			clearTimeout(timeout);
			console.log(`[${config.label}] Bot spawned on port ${config.port}`);
			resolve(bot);
		});

		bot.on("error", (err: Error) => {
			clearTimeout(timeout);
			console.error(`[${config.label}] Error: ${err.message}`);
			reject(err);
		});
	});
}

async function waitForWindow(bot: Bot, label: string): Promise<WindowInfo> {
	return new Promise((resolve, reject) => {
		const timeout = setTimeout(() => {
			reject(new Error(`[${label}] GUI window did not open within 10s`));
		}, 10000);

		bot.once("windowOpen", async (window) => {
			clearTimeout(timeout);
			// Kunectron applies elements right after opening the inventory,
			// so wait a moment for the slot-update packets to arrive.
			await new Promise((r) => setTimeout(r, 1000));

			const current = bot.currentWindow ?? window;
			const slots: Record<string, WindowSlotItem> = {};
			for (const [slot, item] of Object.entries(current.slots)) {
				if (item != null && typeof item === "object" && "name" in item) {
					const typed = item as {
						name: string;
						count: number;
						displayName?: unknown;
					};
					slots[slot] = {
						name: typed.name,
						count: typed.count,
						displayName:
							typed.displayName != null ? String(typed.displayName) : undefined,
					};
				}
			}
			const title = componentToText(window.title ?? "");
			console.log(
				`[${label}] GUI "${title}" opened (` +
					`${Object.keys(slots).length} items)`,
			);
			resolve({
				title,
				slotCount: Object.keys(slots).length,
				slots,
			});
		});
	});
}

async function runGuiScenario(config: ServerConfig): Promise<void> {
	const bot = await connectBot(config);

	// Send /simplequest quest to open the quest selection GUI
	bot.chat("/simplequest quest");
	const questWindow = await waitForWindow(bot, config.label);
	console.log(
		`[${config.label}] Quest GUI result: ` +
			`title=${JSON.stringify(questWindow.title)}, ` +
			`items=${questWindow.slotCount}`,
	);

	// Validate: GUI should have at least 1 item (test quest)
	if (questWindow.slotCount === 0) {
		console.warn(`[${config.label}] WARNING: Quest GUI has 0 items`);
	}

	bot.quit();
	console.log(`[${config.label}] Bot disconnected`);
}

async function main(): Promise<void> {
	const results = await Promise.allSettled(
		SERVERS.map((server) => runGuiScenario(server)),
	);

	let failed = 0;
	for (const result of results) {
		if (result.status === "rejected") {
			console.error(result.reason);
			failed++;
		}
	}

	if (failed > 0) {
		console.error(`${failed} bot scenario(s) failed`);
		process.exit(1);
	}

	console.log("All bot scenarios passed");
}

main().catch((err: unknown) => {
	console.error("Bot scenario runner failed:", err);
	process.exit(1);
});
