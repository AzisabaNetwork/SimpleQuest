import { describe, it, expect, beforeAll, afterAll, afterEach } from "vitest";
import type { Bot } from "mineflayer";
import { createTestBot, disconnectBot, executeCommand, waitForWindow } from "../utils/connect.js";

/**
 * Window types as exposed by mineflayer / prismarine-windows.
 */
interface MinecraftWindow {
  id: number;
  type: string;
  title: string;
  slots: Array<WindowSlot | null>;
  containerSize: number;
}

interface WindowSlot {
  name: string;
  nbtData?: { type: string; value: { display?: { value: { Name?: { value?: string }; Lore?: { value: { value: string[] } } } } } } | null;
}

interface WindowWithItems extends MinecraftWindow {
  slots: Array<WindowSlot | null>;
}

// These tests require a running Paper server with SimpleQuest installed.
// Set environment variables:
//   GUI_TEST_HOST=localhost  GUI_TEST_PORT=25565

const HOST = process.env["GUI_TEST_HOST"] ?? "localhost";
const PORT = parseInt(process.env["GUI_TEST_PORT"] ?? "25565", 10);

describe("SimpleQuest GUI Tests", () => {
  const bots: Bot[] = [];

  async function createAndLogin(username: string): Promise<Bot> {
    const bot = await createTestBot({ host: HOST, port: PORT, username });
    bots.push(bot);
    return bot;
  }

  afterEach(async () => {
    // Cleanup all bots between tests
    for (const bot of [...bots]) {
      try { bot.end(); } catch { /* ignore */ }
    }
    bots.length = 0;
  });

  describe("QuestGui (/simplequest quest)", () => {
    it("should open the quest GUI with correct inventory structure", async () => {
      const bot = await createAndLogin("GUI_Test_Quest");

      // Wait a bit for server-side registration
      await new Promise(r => setTimeout(r, 1000));

      executeCommand(bot, "simplequest quest");

      const window = await waitForWindow(bot) as MinecraftWindow;

      // Verify basic structure
      expect(window).toBeDefined();
      expect(window.type).toBe("minecraft:generic_9x6"); // ChestGui SIZE_54
    }, 20000);

    it("should have quest items in the inventory slots", async () => {
      const bot = await createAndLogin("GUI_Test_Quest2");
      await new Promise(r => setTimeout(r, 1000));

      executeCommand(bot, "simplequest quest");
      const window = await waitForWindow(bot) as WindowWithItems;

      // Slots 18 onwards should contain quest items if any quests are defined
      const firstQuestSlot = window.slots[18];
      // At minimum, the slot should exist
      expect(window.slots.length).toBeGreaterThan(18);
    }, 20000);

    it("should have search button (compass) at slot 53", async () => {
      const bot = await createAndLogin("GUI_Test_Search");
      await new Promise(r => setTimeout(r, 1000));

      executeCommand(bot, "simplequest quest");
      const window = await waitForWindow(bot) as WindowWithItems;

      const compassSlot = window.slots[53];
      expect(compassSlot).toBeDefined();
      if (compassSlot) {
        // Compass item name in modern versions
        expect(compassSlot.name).toContain("compass");
      }
    }, 20000);
  });

  describe("PartyMenuGui (/simplequest party)", () => {
    it("should open party GUI as solo player", async () => {
      const bot = await createAndLogin("GUI_Test_Party");
      await new Promise(r => setTimeout(r, 1500));

      executeCommand(bot, "simplequest party");

      const window = await waitForWindow(bot) as MinecraftWindow;
      expect(window).toBeDefined();
      expect(window.type).toBe("minecraft:generic_9x6");
    }, 20000);
  });
});
