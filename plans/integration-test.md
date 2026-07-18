# Integration Test 設計: Paper サーバー実起動テスト

> **トリガー**: `release` ブランチへの PR
> **実行環境**: GitHub Actions (ubuntu-latest)
> **ローカル開発時**: 実行しない（CI 専用）

---

## 1. アーキテクチャ概要

```
┌─────────────────────────────────────────────────────────────┐
│                   GitHub Actions Runner                      │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Services (Docker)                        │  │
│  │  ┌──────────┐  ┌──────────┐                          │  │
│  │  │ MariaDB  │  │  Redis   │                          │  │
│  │  │  11.x    │  │  7.x     │                          │  │
│  │  └────┬─────┘  └────┬─────┘                          │  │
│  └───────┼─────────────┼────────────────────────────────┘  │
│          │             │                                    │
│  ┌───────┴─────────────┴────────────────────────────────┐  │
│  │              Paper Servers                            │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐           │  │
│  │  │ Master   │  │  Slave   │  │ ReadOnly │           │  │
│  │  │ :25565   │  │ :25566   │  │ :25567   │           │  │
│  │  │ w→mysql  │  │ w→yaml   │  │  both f  │           │  │
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘           │  │
│  └───────┼─────────────┼─────────────┼──────────────────┘  │
│          │             │             │                      │
│  ┌───────┴─────────────┴─────────────┴──────────────────┐  │
│  │         Mineflayer Bots (Node.js)                     │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐           │  │
│  │  │  Bot A   │  │  Bot B   │  │  Bot C   │           │  │
│  │  │ :25565   │  │ :25566   │  │ :25567   │           │  │
│  │  └──────────┘  └──────────┘  └──────────┘           │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         Kotest Integration Tests (Gradle)             │  │
│  │  - サーバーログ検証                                    │  │
│  │  - MariaDB データ検証                                  │  │
│  │  - Redis データ検証                                    │  │
│  │  - RCON 経由でコマンド実行・結果検証                    │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Artifacts                                │  │
│  │  - サーバーログ (latest.log × 3)                       │  │
│  │  - GUI スクリーンショット (Puppeteer PNG)              │  │
│  │  - Test report (kotest HTML)                          │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 3 サーバーの役割

| サーバー | ポート | write-to-mysql | write-to-yaml | 役割 |
|---|---|---|---|---|
| Master | 25565 | `true` | `false` | クエスト定義編集元、MySQL へ書き込み |
| Slave | 25566 | `false` | `true` | MySQL → YAML 同期受信 |
| ReadOnly | 25567 | `false` | `false` | 読取専用、進捗表示のみ |

---

## 2. ファイル構成

```
project/
├── .github/workflows/
│   ├── build.yml                      # 既存: コンパイル + 単体テスト
│   └── integration-test.yml           # 新規: 本ドキュメントの実装
│
├── src/integrationTest/               # 新規 SourceSet
│   ├── kotlin/net/azisaba/simplequest/integration/
│   │   ├── SimpleQuestIntegrationTest.kt   # テスト本体
│   │   ├── PaperServerExtension.kt         # kotest Extension: サーバー制御
│   │   ├── MultiServerExtension.kt         # kotest Extension: 複数サーバー
│   │   ├── ServerProcess.kt               # Paper プロセス管理
│   │   ├── ServerAssertions.kt            # ログ/MariaDB/Redis 検証 DSL
│   │   └── fixture/
│   │       ├── master-config.yml
│   │       ├── slave-config.yml
│   │       └── readonly-config.yml
│   └── resources/
│       └── quests/                        # テスト用クエスト定義
│           └── test_quest.yml
│
├── bots/                                  # Mineflayer bot スクリプト
│   ├── package.json
│   ├── tsconfig.json
│   ├── src/
│   │   ├── main.ts                        # エントリポイント
│   │   ├── server.ts                      # サーバー接続・コマンド実行
│   │   ├── gui-capture.ts                 # GUI ウィンドウキャプチャ
│   │   └── screenshot.ts                  # Puppeteer スクリーンショット
│   └── tests/
│       └── scenarios.ts                   # テストシナリオ定義
│
├── build.gradle.kts                       # integrationTest SourceSet 追加
└── plans/
    └── integration-test.md                # 本ドキュメント
```

---

## 3. CI ワークフロー設計

### トリガー

```yaml
on:
  pull_request:
    branches: [release]
```

### Job 構成（単一 job、逐次実行）

```yaml
jobs:
  integration-test:
    runs-on: ubuntu-latest
    timeout-minutes: 25

    services:
      mariadb:
        image: mariadb:11
        env:
          MARIADB_ROOT_PASSWORD: test
          MARIADB_DATABASE: simplequest
        ports:
          - 3306:3306
        options: >-
          --health-cmd="healthcheck.sh --connect --innodb_initialized"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=5

      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd="redis-cli ping"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=5
```

### Steps

```
Step 1: Checkout + Setup Java 21 + Setup Gradle
Step 2: Setup Node.js 22 (for Mineflayer bots)
Step 3: ./gradlew shadowJar
Step 4: Setup Paper servers (3 instances)
Step 5: Start all 3 servers (FIFO stdin control, wait for "Done")
Step 6: npm install (bots/)
Step 7: Run Mineflayer bot scenarios → screenshots
Step 8: Run kotest integration tests (./gradlew integrationTest)
Step 9: Stop servers (send "stop" via FIFO)
Step 10: Collect artifacts (logs, screenshots)
```

### Step 4 詳細: サーバーセットアップ

```bash
# ディレクトリ作成
for i in master slave readonly; do
  mkdir -p servers/$i/plugins/SimpleQuest
  echo "eula=true" > servers/$i/eula.txt
  echo "online-mode=false" > servers/$i/server.properties
  # ポート設定
  case $i in
    master)   PORT=25565; RCON=25575 ;;
    slave)    PORT=25566; RCON=25576 ;;
    readonly) PORT=25567; RCON=25577 ;;
  esac
  echo "server-port=$PORT" >> servers/$i/server.properties
  echo "enable-rcon=true" >> servers/$i/server.properties
  echo "rcon.port=$RCON" >> servers/$i/server.properties
  echo "rcon.password=test" >> servers/$i/server.properties
  
  # プラグインコピー
  cp build/libs/*.jar servers/$i/plugins/
  
  # config 配置
  cp src/integrationTest/resources/$i-config.yml servers/$i/plugins/SimpleQuest/config.yml
done

# Paper ダウンロード (1.21.11)
for i in master slave readonly; do
  cp paper.jar servers/$i/
done
```

### Step 5 詳細: サーバー起動（FIFO 方式）

```bash
start_server() {
  local name=$1
  local dir="servers/$name"
  local log="$dir/server.log"
  
  mkfifo "$dir/stdin"
  tail -f "$dir/stdin" | java -Xmx1G -jar "$dir/paper.jar" --nogui > "$log" 2>&1 &
  echo $! > "$dir/pid"
  
  # "Done" が出るまで待つ (max 120s)
  timeout 120 bash -c "until grep -q 'Done' \"$log\" 2>/dev/null; do sleep 2; done"
  
  echo "[$name] Server started on port $(grep 'server-port' $dir/server.properties | cut -d= -f2)"
}

start_server master
start_server slave
start_server readonly
```

### Step 9 詳細: サーバー停止

```bash
stop_server() {
  local name=$1
  local dir="servers/$name"
  
  echo "stop" > "$dir/stdin"
  
  # 正常終了を待つ (max 30s)
  timeout 30 bash -c "while kill -0 \$(cat $dir/pid) 2>/dev/null; do sleep 1; done"
  
  echo "[$name] Server stopped"
}

stop_server master
stop_server slave
stop_server readonly
```

---

## 4. Gradle 設定: integrationTest SourceSet

```kotlin
// build.gradle.kts に追加
sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
        runtimeClasspath += output + compileClasspath
    }
}

val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

val integrationTest by tasks.registering(Test::class) {
    description = "Runs integration tests (requires running Paper servers)"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    
    // CI でのみ実行
    onlyIf { System.getenv("CI") == "true" }
    
    useJUnitPlatform()
    
    // 環境変数でサーバー接続先を受け取る
    environment("MASTER_PORT", "25565")
    environment("SLAVE_PORT", "25566")
    environment("READONLY_PORT", "25567")
    environment("RCON_PASSWORD", "test")
    environment("MARIADB_URL", "jdbc:mariadb://localhost:3306/simplequest")
    environment("MARIADB_USER", "root")
    environment("MARIADB_PASSWORD", "test")
    environment("REDIS_HOST", "localhost")
    environment("REDIS_PORT", "6379")
}

dependencies {
    integrationTestImplementation(libs.kotest.runner)
    integrationTestImplementation(libs.kotest.assertions)
    integrationTestImplementation(libs.mariadb)  // DB 直接検証用
    integrationTestImplementation(libs.lettuce.core)  // Redis 直接検証用
}
```

---

## 5. Kotest Extension 設計

### PaperServerExtension

サーバープロセスを管理するのではなく、**起動済みサーバーに接続する** Extension。
サーバーの起動/停止は CI ワークフローが担当。

```kotlin
/**
 * 起動済みの Paper サーバーに接続する kotest Extension。
 * サーバーのライフサイクル管理は CI (シェルスクリプト) が担当。
 */
class PaperServerExtension(
    private val name: String,
    private val host: String = "localhost",
    private val port: Int,
    private val rconPort: Int,
    private val rconPassword: String = "test",
    private val logPath: Path,
) : BeforeTest, AfterTest, TestListener {

    // RCON 経由でコマンド実行
    suspend fun executeCommand(command: String): String
    
    // サーバーログの検索
    fun logContains(pattern: Regex): Boolean
    fun logLines(): List<String>
    
    // プレイヤーとして参加 (Mineflayer)
    fun expectPlayer(name: String): PlayerExpectation
}
```

### MultiServerExtension

```kotlin
class MultiServerExtension(
    val master: PaperServerExtension,
    val slave: PaperServerExtension,
    val readOnly: PaperServerExtension,
) : TestListener {
    // 全サーバーのログを横断検索
    // サーバー間の同期検証ヘルパー
}
```

### テスト DSL イメージ

```kotlin
class SimpleQuestIntegrationTest : FunSpec({
    val master = PaperServerExtension("master", 25565, 25575, logPath = Path("servers/master/server.log"))
    val slave = PaperServerExtension("slave", 25566, 25576, logPath = Path("servers/slave/server.log"))
    val readonly = PaperServerExtension("readonly", 25567, 25577, logPath = Path("servers/readonly/server.log"))
    
    extensions(master, slave, readonly)
    
    context("Basic lifecycle") {
        test("all 3 servers enable cleanly") {
            master.logContains("SimpleQuest enabled".toRegex()) shouldBe true
            slave.logContains("SimpleQuest enabled".toRegex()) shouldBe true
            readonly.logContains("SimpleQuest enabled".toRegex()) shouldBe true
        }
        
        test("no ERROR in server logs") {
            master.logLines().none { it.contains("ERROR") || it.contains("FATAL") } shouldBe true
            slave.logLines().none { it.contains("ERROR") || it.contains("FATAL") } shouldBe true
            readonly.logLines().none { it.contains("ERROR") || it.contains("FATAL") } shouldBe true
        }
    }
    
    context("Multi-server sync") {
        test("master writes quest definition to MySQL") {
            // master の reload で MySQL に書き込み
            val result = master.executeCommand("simplequest reload")
            result shouldContain "reloaded"
            
            // MariaDB に直接クエリして検証
            val db = Database.connect(/* ... */)
            val rows = db.exec("SELECT COUNT(*) FROM quest_definitions") { /* ... */ }
            rows shouldBeGreaterThan 0
        }
        
        test("slave receives quest definition from MySQL") {
            val result = slave.executeCommand("simplequest reload")
            result shouldContain "reloaded"
            slave.logContains("sync.*completed".toRegex()) shouldBe true
        }
    }
    
    context("GUI verification") {
        test("quest GUI opens with expected items") {
            // このテストは Mineflayer 側 (TypeScript) で実施し、
            // スクリーンショットを artifact として出力する
        }
    }
})
```

---

## 6. Mineflayer Bot 設計

### 技術スタック

| 項目 | 選択 |
|---|---|
| ランタイム | Node.js 22 |
| 言語 | TypeScript |
| Bot ライブラリ | `mineflayer` |
| GUI 描画 | `prismarine-viewer` (WebGL → headless browser) |
| スクリーンショット | `puppeteer` (headless Chromium) |
| テストフレームワーク | `vitest` またはプレーン TypeScript |
| パッケージ管理 | npm |

### パッケージ構成

```json
{
  "name": "simplequest-integration-bots",
  "private": true,
  "scripts": {
    "test": "npx tsx src/main.ts",
    "screenshot": "npx tsx src/screenshot.ts"
  },
  "dependencies": {
    "mineflayer": "^4.x",
    "prismarine-viewer": "^1.x"
  },
  "devDependencies": {
    "puppeteer": "^22.x",
    "typescript": "^5.x",
    "tsx": "^4.x"
  }
}
```

### Bot シナリオの流れ

```typescript
// bots/src/main.ts
import { createBot } from 'mineflayer';
import { mineflayer as mineflayerViewer } from 'prismarine-viewer';

async function runScenario(port: number, label: string) {
  const bot = createBot({
    host: 'localhost',
    port,
    username: `Bot_${label}`,
    auth: 'offline',  // CI では offline-mode
  });

  return new Promise<void>((resolve, reject) => {
    bot.once('spawn', async () => {
      console.log(`[${label}] Bot spawned on port ${port}`);
      
      // コマンド実行 → GUI を開く
      bot.chat('/simplequest quest');
      
      // GUI ウィンドウを待つ
      bot.once('windowOpen', (window) => {
        console.log(`[${label}] GUI opened: ${window.title}`);
        
        // ウィンドウ内容をダンプ
        for (const [slot, item] of Object.entries(window.slots)) {
          if (item) console.log(`  Slot ${slot}: ${item.name} x${item.count}`);
        }
        
        bot.quit();
        resolve();
      });
      
      // タイムアウト (10秒)
      setTimeout(() => reject(new Error('GUI did not open')), 10000);
    });
    
    bot.on('error', reject);
  });
}

// 全サーバーで実行
await Promise.all([
  runScenario(25565, 'master'),
  runScenario(25566, 'slave'),
  runScenario(25567, 'readonly'),
]);
```

### スクリーンショット取得（Puppeteer + prismarine-viewer）

```typescript
// bots/src/screenshot.ts
import puppeteer from 'puppeteer';
import { createBot } from 'mineflayer';
import { mineflayer as mineflayerViewer } from 'prismarine-viewer';

async function takeScreenshot(port: number, outputPath: string) {
  const bot = createBot({ host: 'localhost', port, username: 'ScreenshotBot', auth: 'offline' });
  
  // prismarine-viewer を起動 (WebSocket サーバー)
  mineflayerViewer(bot, { port: 3000, firstPerson: false });
  
  bot.once('spawn', async () => {
    bot.chat('/simplequest quest');
    
    // GUI が開くのを待つ
    await new Promise<void>(resolve => bot.once('windowOpen', () => resolve()));
    
    // Puppeteer でビューワーに接続してスクリーンショット
    const browser = await puppeteer.launch({ headless: true });
    const page = await browser.newPage();
    await page.goto('http://localhost:3000');
    await page.waitForTimeout(2000);
    await page.screenshot({ path: outputPath, fullPage: true });
    
    await browser.close();
    bot.quit();
  });
}

await takeScreenshot(25565, 'artifacts/quest-gui-master.png');
await takeScreenshot(25566, 'artifacts/quest-gui-slave.png');
await takeScreenshot(25567, 'artifacts/quest-gui-readonly.png');
```

### CI Step: Bot 実行

```yaml
- name: Run bot scenarios
  working-directory: bots
  run: |
    npm ci
    npx tsx src/main.ts 2>&1 | tee ../artifacts/bot-output.log
    
- name: Capture screenshots
  working-directory: bots
  run: |
    npx tsx src/screenshot.ts
    
- name: Upload screenshots
  uses: actions/upload-artifact@v4
  with:
    name: integration-test-screenshots
    path: artifacts/*.png
    retention-days: 7
```

---

## 7. テストシナリオ一覧

### S1: 基本ライフサイクル (kotest)

- ✅ 全3サーバーが `SimpleQuest enabled` をログ出力
- ✅ 全3サーバーの起動ログに `ERROR` / `FATAL` が含まれない
- ✅ サーバー停止時に `SimpleQuest disabled` をログ出力

### S2: マルチサーバー同期 (kotest)

- ✅ Master: `/simplequest reload` → MariaDB に quest_definitions が UPSERT される
- ✅ Slave: `/simplequest reload` → ローカル YAML が MySQL の内容で更新される
- ✅ ReadOnly: `/simplequest quest` → クエスト一覧 GUI が表示される

### S3: GUI 表示 (Mineflayer)

- ✅ `/simplequest quest` → クエスト選択 GUI が開く
- ✅ GUI 内に正しいアイテム・タイトルが表示されている
- ✅ `/simplequest party` → パーティ GUI が開く

### S4: GUI スクリーンショット (Mineflayer + Puppeteer)

- ✅ クエスト選択 GUI のスクリーンショット (25565, 25566, 25567)
- ✅ パーティ GUI のスクリーンショット
- ✅ artifact としてアップロード

### S5: プレイヤーデータ永続化 (kotest)

- ✅ プレイヤーのクエスト進捗が MariaDB に保存される
- ✅ サーバー再起動後も進捗が復元される

### S6: 耐障害性 (kotest)

- ✅ DB 未接続状態でもプラグインが enable する（関連機能はスキップ）
- ✅ Redis 未接続状態でもクラッシュしない

---

## 8. 実装フェーズ

### Phase 1: 耐障害性の確保（先行必須）

```
- DatabaseManager.init で connect() 失敗時に例外捕捉
- MigrationRunner を DB 未接続時にスキップ
- SyncService を DB 未接続時にスキップ
- Redis 接続失敗時の fallback
- onDisable の lateinit ガード (既存修正済み)
```

### Phase 2: CI ワークフロー + サーバー制御

```
- integration-test.yml 作成
- FIFO 方式のサーバー起動/停止スクリプト
- 3 サーバー用 config.yml フィクスチャ作成
- Paper ダウンロード処理
- Services (MariaDB, Redis) の health check 待機
```

### Phase 3: Gradle integrationTest SourceSet

```
- build.gradle.kts に integrationTest SourceSet 追加
- kotest 依存追加
- ServerAssertions DSL 実装
- 環境変数経由の設定注入
```

### Phase 4: kotest テスト実装

```
- PaperServerExtension 実装
- MultiServerExtension 実装
- S1 (基本ライフサイクル) テスト
- S2 (マルチサーバー同期) テスト
- S5 (プレイヤーデータ) テスト
- S6 (耐障害性) テスト
```

### Phase 5: Mineflayer Bot

```
- bots/ パッケージセットアップ
- サーバー接続・コマンド実行ボット
- GUI ウィンドウキャプチャ
- Puppeteer + prismarine-viewer スクリーンショット
- S3, S4 テストシナリオ実装
```

---

## 9. ローカル実行（開発時）

通常の `./gradlew test` では integrationTest は実行されない。

```bash
# CI 環境変数をセットして手動実行する場合
CI=true ./gradlew integrationTest

# ただしサーバー・DB・Redis を手動で起動しておく必要がある
```

---

## 10. 懸念点・未解決項目

| 項目 | 状態 | 備考 |
|---|---|---|
| prismarine-viewer の 1.21.11 対応 | ✅ 対応済み | v1.21.4 以降対応 |
| headless Chromium の GitHub Actions 対応 | ✅ | `puppeteer` が自動で Chromium をダウンロード |
| 3 サーバー同時起動のメモリ | ⚠️ 要検証 | 各 `-Xmx1G` → 計 3GB +α。ubuntu-latest は 7GB RAM |
| RCON ライブラリ (Kotlin) | 🔍 調査中 | 軽量 RCON クライアントが必要 |
| Paper ダウンロードのキャッシュ | ⚠️ | Paper API から毎回 DL。キャッシュ戦略検討 |
| Minecraft EULA | 🔍 | `eula=true` を CI で書くのはライセンス的に問題ないか確認 |
| Kunectron (compileOnly) | ✅ | サーバーに jar を配置せず enable → GUI 機能のみ無効化されるべき |
