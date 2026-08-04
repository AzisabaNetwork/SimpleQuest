import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    testTimeout: 30000,
    hookTimeout: 30000,
    // CI 環境でのみ実行
    ...(process.env.CI !== "true" && {
      include: [], // ローカルでは実行しない
    }),
  },
});
