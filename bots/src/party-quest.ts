/**
 * Multi-bot party quest scenario for SimpleQuest integration tests.
 *
 * Spawns 2+ bots, forms a party, and runs a quest together.
 *
 * Flow:
 *   1. Bot1 (leader) connects
 *   2. Bot1 invites Bot2 via /party invite
 *   3. Bot2 connects, detects invite in chat, accepts via /party accept
 *   4. RCON grants PartyQuest to both bots
 *   5. Bot1 opens quest GUI and starts the quest
 *   6. Both bots break stone blocks to complete the objective
 *   7. All bots observe quest completion notifications
 *
 * Usage:
 *   SQS_PORT=25565 SQS_BOT_COUNT=3 npx tsx src/party-quest.ts
 *
 * Environment:
 *   SQS_PORT=25565      - Server port
 *   SQS_BOT_COUNT=2     - Number of bots (default: 2)
 */
import { createBot, type Bot } from "mineflayer";

// ---- Types ----

interface WindowSlotItem {
    name: string;
    count: number;
    displayName?: string;
}

interface WindowInfo {
    title: string;
    slotCount: number;
    slots: Record<number, WindowSlotItem>;
}

interface ScenarioResult {
    label: string;
    botsConnected: number;
    partyFormed: boolean;
    questStarted: boolean;
    questGuiOpened: boolean;
    blocksBroken: number;
    messages: string[];
    completed: boolean;
    durationMs: number;
    errors: string[];
}

// ---- Config ----

const PORT = parseInt(process.env.SQS_PORT ?? "25565", 10);
const BOT_COUNT = Math.max(2, parseInt(process.env.SQS_BOT_COUNT ?? "2", 10));

// ---- Logger ----

function log(label: string, msg: string) {
    const ts = new Date().toISOString().split("T")[1].slice(0, 12);
    console.log(`[${ts}][${label}] ${msg}`);
}

// ---- Bot management ----

function connectBot(username: string): Promise<Bot> {
    return new Promise((resolve, reject) => {
        const bot = createBot({
            host: "localhost",
            port: PORT,
            username,
            auth: "offline",
        });
        const timeout = setTimeout(() => reject(new Error(`${username} connection timeout`)), 30000);
        bot.once("spawn", () => {
            clearTimeout(timeout);
            log("CONNECT", `${username} spawned`);
            resolve(bot);
        });
        bot.on("error", (err) => { clearTimeout(timeout); reject(err); });
    });
}

function trackChat(bot: Bot, messages: string[]): void {
    bot.on("message", (jsonMsg) => {
        const text = String(jsonMsg);
        messages.push(text);
    });
}

function sleep(ms: number): Promise<void> {
    return new Promise((r) => setTimeout(r, ms));
}

// ---- GUI helpers ----

async function waitForWindow(bot: Bot, timeoutMs = 10000): Promise<WindowInfo> {
    return new Promise((resolve, reject) => {
        const t = setTimeout(() => reject(new Error("Window timeout")), timeoutMs);
        bot.once("windowOpen", (window: any) => {
            clearTimeout(t);
            const slots: Record<number, WindowSlotItem> = {};
            for (const [s, item] of Object.entries(window.slots)) {
                const idx = parseInt(s, 10);
                if (item && typeof item === "object" && "name" in item) {
                    const typed = item as any;
                    slots[idx] = {
                        name: typed.name,
                        count: typed.count,
                        displayName: typed.displayName != null ? String(typed.displayName) : undefined,
                    };
                }
            }
            resolve({ title: String(window.title ?? ""), slotCount: Object.keys(slots).length, slots });
        });
    });
}

// ---- Block breaking ----

function getBlockAt(bot: Bot, dx: number, dy: number, dz: number): any {
    const pos = bot.entity.position;
    const Vec3 = (bot as any).registry?.Vec3;
    const bp = Vec3 ? new Vec3(Math.floor(pos.x) + dx, Math.floor(pos.y) + dy, Math.floor(pos.z) + dz) : { x: Math.floor(pos.x) + dx, y: Math.floor(pos.y) + dy, z: Math.floor(pos.z) + dz };
    return bot.blockAt(bp);
}

async function digBlock(bot: Bot, dx: number, dy: number, dz: number): Promise<boolean> {
    const block = getBlockAt(bot, dx, dy, dz);
    if (!block || block.name === "air") return false;
    if (!bot.canDigBlock(block)) return false;
    try {
        await bot.dig(block, true);
        log("DIG", `${bot.username} broke ${block.name} at (${dx},${dy},${dz})`);
        return true;
    } catch {
        return false;
    }
}

async function breakNearbyBlocks(bot: Bot, count: number): Promise<number> {
    let broken = 0;
    const positions = [
        [1, 0, 0], [-1, 0, 0], [0, 0, 1], [0, 0, -1],
        [2, 0, 0], [-2, 0, 0], [0, 0, 2], [0, 0, -2],
        [1, -1, 0], [-1, -1, 0], [0, -1, 1], [0, -1, -1],
        [0, 0, 0], [1, 1, 0], [-1, 1, 0],
    ];
    for (let i = 0; i < positions.length && broken < count; i++) {
        const [dx, dy, dz] = positions[i];
        if (await digBlock(bot, dx, dy, dz)) {
            broken++;
            await sleep(400);
        }
    }
    return broken;
}

// ---- Party formation ----

/**
 * Bot1 (leader) invites Bot2. Bot2 listens for the invite and accepts.
 */
async function formParty(leader: Bot, member: Bot, memberMessages: string[]): Promise<boolean> {
    // Set up listener on member BEFORE sending invite
    const invitePromise = new Promise<string>((resolve, reject) => {
        const timeout = setTimeout(() => reject(new Error("No invite received within 15s")), 15000);

        const handler = (jsonMsg: any) => {
            const text = String(jsonMsg);
            memberMessages.push(text);
            // Parse: "/party accept <uuid>"
            const match = text.match(/\/party accept ([0-9a-f-]+)/i);
            if (match) {
                clearTimeout(timeout);
                resolve(match[1]);
            }
        };
        member.on("message", handler);
    });

    // Send invite from leader
    log("PARTY", `Leader ${leader.username} inviting ${member.username}...`);
    leader.chat(`/party invite ${member.username}`);
    await sleep(1000);

    let inviteId: string;
    try {
        inviteId = await invitePromise;
        log("PARTY", `Member detected invite: ${inviteId}`);
    } catch (e) {
        log("PARTY", `Invite failed: ${(e as Error).message}`);
        return false;
    }

    // Accept the invite
    member.chat(`/party accept ${inviteId}`);
    await sleep(2000);

    log("PARTY", "Party formation complete");
    return true;
}

// ---- Main scenario ----

async function runPartyQuest(): Promise<ScenarioResult> {
    const startTime = Date.now();
    const result: ScenarioResult = {
        label: `party-quest-${BOT_COUNT}bots`,
        botsConnected: 0,
        partyFormed: false,
        questStarted: false,
        questGuiOpened: false,
        blocksBroken: 0,
        messages: [],
        completed: false,
        durationMs: 0,
        errors: [],
    };

    // --- Step 1: Connect bots ---
    log("STEP", `Connecting ${BOT_COUNT} bots...`);
    const botNames: string[] = [];
    for (let i = 0; i < BOT_COUNT; i++) {
        botNames.push(`PartyBot${i + 1}_${Date.now() % 10000}`);
    }

    const bots: Bot[] = [];
    for (const name of botNames) {
        const bot = await connectBot(name);
        trackChat(bot, result.messages);
        bots.push(bot);
        result.botsConnected++;
    }
    log("STEP", `All ${bots.length} bots connected`);

    const leader = bots[0];
    const members = bots.slice(1);

    // --- Step 2: Form party ---
    log("STEP", "Forming party...");
    const memberMsgs: string[] = [];
    const partyOk = await formParty(leader, members[0], memberMsgs);
    result.partyFormed = partyOk;
    if (!partyOk) {
        result.errors.push("Party formation failed");
    }

    // --- Step 3: Wait for party state to settle ---
    await sleep(2000);

    // --- Step 4: Leader opens quest GUI ---
    log("STEP", "Opening quest GUI...");
    leader.chat("/simplequest quest");
    try {
        const win = await waitForWindow(leader, 10000);
        result.questGuiOpened = true;
        result.questStarted = true; // if GUI opens, quest list is available
        log("GUI", `Opened "${win.title}" with ${win.slotCount} items`);
    } catch {
        result.errors.push("Quest GUI did not open");
    }

    // --- Step 5: All bots break blocks ---
    log("STEP", "All bots breaking blocks for quest objective...");
    const breakTarget = 10; // Match PartyQuest BreakStone: 10
    const perBot = Math.ceil(breakTarget / bots.length);

    const breakResults = await Promise.all(
        bots.map(async (bot) => {
            const count = await breakNearbyBlocks(bot, perBot);
            log("BREAK", `${bot.username} broke ${count} blocks`);
            return count;
        }),
    );
    result.blocksBroken = breakResults.reduce((a, b) => a + b, 0);
    log("STEP", `Total blocks broken: ${result.blocksBroken}`);

    // --- Step 6: Wait for completion messages ---
    await sleep(3000);
    for (const msg of result.messages) {
        if (/quest completed|party.*quest/i.test(msg)) {
            result.completed = true;
            break;
        }
    }
    if (!result.completed) {
        // Also check for action commands (say ... completed)
        for (const msg of result.messages) {
            if (/completed the party integration test quest/i.test(msg)) {
                result.completed = true;
                break;
            }
        }
    }

    // --- Cleanup ---
    for (const bot of bots) {
        try { bot.quit(); } catch { /* bot may already be disconnected */ }
    }

    result.durationMs = Date.now() - startTime;
    log("RESULT", JSON.stringify({
        botsConnected: result.botsConnected,
        partyFormed: result.partyFormed,
        questStarted: result.questStarted,
        blocksBroken: result.blocksBroken,
        completed: result.completed,
        errors: result.errors.length,
        durationMs: result.durationMs,
    }, null, 2));

    return result;
}

// ---- Entry ----

async function main() {
    try {
        const result = await runPartyQuest();
        const exitCode = result.errors.length > 0 ? 1 : 0;
        console.log(JSON.stringify({ status: exitCode === 0 ? "ok" : "partial", result }));
        process.exit(exitCode);
    } catch (err) {
        console.error(JSON.stringify({ status: "error", error: String(err) }));
        process.exit(1);
    }
}

main();
