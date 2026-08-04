import { type Bot, createBot } from "mineflayer";

export interface BotOptions {
  host: string;
  port: number;
  username: string;
}

/**
 * Creates and connects a mineflayer bot to the target Paper server.
 * Returns the bot once it has spawned.
 */
export function createTestBot(options: BotOptions): Promise<Bot> {
  const bot = createBot({
    host: options.host,
    port: options.port,
    username: options.username,
  });

  return new Promise<Bot>((resolve, reject) => {
    const timeout = setTimeout(() => {
      reject(new Error(`Bot "${options.username}" spawn timeout`));
    }, 30000);

    bot.once("spawn", () => {
      clearTimeout(timeout);
      resolve(bot);
    });

    bot.once("error", (err: Error) => {
      clearTimeout(timeout);
      reject(err);
    });

    bot.once("kicked", (reason: string) => {
      clearTimeout(timeout);
      reject(new Error(`Bot kicked: ${reason}`));
    });
  });
}

/**
 * Makes the bot execute a command via chat.
 * Accounts for the leading slash and chat delay.
 */
export function executeCommand(bot: Bot, command: string): void {
  bot.chat(`/${command}`);
}

/**
 * Blocks until a window (inventory GUI) is opened on the bot,
 * or until timeout. Returns the window.
 */
export function waitForWindow(bot: Bot, timeoutMs = 10000): Promise<unknown> {
  return new Promise((resolve, reject) => {
    // If already open, resolve immediately
    if (bot.currentWindow) {
      resolve(bot.currentWindow);
      return;
    }

    const timer = setTimeout(() => {
      reject(new Error("Window did not open within timeout"));
    }, timeoutMs);

    function onOpen(window: unknown) {
      clearTimeout(timer);
      bot.removeListener("windowOpen", onOpen);
      resolve(window);
    }

    bot.once("windowOpen", onOpen);
  });
}

/**
 * Disconnects the bot gracefully.
 */
export function disconnectBot(bot: Bot): void {
  bot.end();
}
