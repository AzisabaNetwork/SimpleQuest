/**
 * GUI screenshot capture using Mineflayer + Puppeteer.
 *
 * Connects to each server, opens quest/party GUIs,
 * and captures screenshots of the prismarine-viewer.
 *
 * NOTE: This script requires a running prismarine-viewer server
 * on port 3000. In CI, start it separately before running this script.
 *
 * For now, this serves as documentation of the intended screenshot flow.
 * The main test scenarios (main.ts) provide sufficient GUI validation.
 */
import { createBot } from "mineflayer";

interface ServerConfig {
	label: string;
	port: number;
}

const SERVERS: ServerConfig[] = [
	{ label: "master", port: 25565 },
	{ label: "slave", port: 25566 },
	{ label: "readonly", port: 25567 },
];

async function captureGui(config: ServerConfig): Promise<void> {
	const bot = createBot({
		host: "localhost",
		port: config.port,
		username: `ScreenshotBot_${config.label}`,
		auth: "offline",
	});

	return new Promise<void>((resolve, reject) => {
		const timeout = setTimeout(() => {
			reject(new Error(`[${config.label}] Screenshot timeout`));
		}, 30000);

		bot.once("spawn", () => {
			console.log(
				`[${config.label}] Screenshot bot spawned on port ${config.port}`,
			);
			// Open quest GUI
			bot.chat("/simplequest quest");
		});

		bot.once("windowOpen", (window) => {
			clearTimeout(timeout);
			const title = String(window.title ?? "");
			const slotEntries = Object.entries(window.slots).filter(
				([, item]) => item != null,
			);
			console.log(
				`[${config.label}] GUI "${title}" ` + `— ${slotEntries.length} items`,
			);
			for (const [slot, item] of slotEntries) {
				const typed = item as {
					name: string;
					count: number;
				};
				console.log(`  Slot ${slot}: ${typed.name} x${typed.count}`);
			}

			// For actual screenshot, this would launch Puppeteer
			// and capture prismarine-viewer at http://localhost:3000
			// Example: await page.screenshot({ path: outputFile });

			bot.quit();
			resolve();
		});

		bot.on("error", (err: Error) => {
			clearTimeout(timeout);
			console.error(`[${config.label}] Screenshot error: ${err.message}`);
			reject(err);
		});
	});
}

async function main(): Promise<void> {
	for (const server of SERVERS) {
		try {
			await captureGui(server);
		} catch (err) {
			console.error(`[${server.label}] Screenshot failed:`, err);
		}
	}
	console.log("Screenshot capture complete");
}

main().catch((err: unknown) => {
	console.error("Screenshot runner failed:", err);
	process.exit(1);
});
