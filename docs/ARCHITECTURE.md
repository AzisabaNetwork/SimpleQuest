# アーキテクチャ概要

## プロジェクト概要

**SimpleQuest** は [アジ鯖](https://www.azisaba.net) の Reincarnation PvE サーバー向けに開発された Paper プラグインのクエストマネージャーです。

- **言語**: Kotlin (JVM)
- **対象 API**: Paper 1.21.11
- **DI**: Dagger 2
- **コマンド**: cloud (Incendo) + Brigadier
- **ライセンス**: (未定)

## アーキテクチャスタイル

クリーンアーキテクチャを採用し、`domain` / `application` / `infrastructure` / `di` の4層に分離しています。

```
src/main/kotlin/net/azisaba/simplequest/
├── SimpleQuest.kt                 — プラグインエントリポイント (JavaPlugin)
├── SimpleQuestLoader.kt           — YAML → QuestType 変換・レジストリ登録
│
├── domain/                        — ドメイン層 (純粋なビジネスロジック)
│   ├── action/
│   │   ├── Action.kt              — アクション定義
│   │   ├── ActionSet.kt           — トリガー別アクションセット
│   │   ├── ActionType.kt          — アクション種別 enum
│   │   └── port/
│   │       ├── ActionDispatcher.kt   — アクション実行ポート
│   │       └── ActionRunner.kt       — アクション実行ランナーポート
│   ├── data/
│   │   ├── Icon.kt                — アイコン定義
│   │   └── Location.kt            — 座標定義
│   ├── party/model/
│   │   ├── InvitationSetting.kt   — 招待設定 enum
│   │   ├── Invite.kt              — 招待ドメインモデル
│   │   └── Party.kt               — パーティ集約
│   ├── quest/model/
│   │   ├── AcceptConditions.kt    — 受注条件
│   │   ├── EndReason.kt           — 終了理由 enum
│   │   ├── GameGuide.kt           — ナビゲーションガイド
│   │   ├── PlayLimits.kt          — プレイ制限
│   │   ├── Progresses.kt          — 進捗管理
│   │   ├── Quest.kt               — クエスト集約インターフェース
│   │   ├── QuestCategory.kt       — カテゴリ
│   │   ├── QuestRequirement.kt    — 達成条件
│   │   ├── QuestResult.kt         — クエスト開始結果 (Success/Failure)
│   │   ├── QuestState.kt          — 状態 enum
│   │   └── QuestType.kt           — クエスト定義 (YAML 由来)
│   ├── quest/port/
│   │   ├── QuestNotifier.kt       — 通知ポート
│   │   └── QuestRepository.kt     — リポジトリポート
│   ├── registry/
│   │   ├── Keyed.kt               — キー付きインターフェース
│   │   └── Registry.kt            — 汎用レジストリ
│   ├── script/
│   │   ├── Script.kt              — スクリプト定義 (data class)
│   │   └── port/ScriptRunner.kt   — スクリプト実行ポート
│   └── stage/
│       ├── Queue.kt               — ステージキュー
│       ├── Stage.kt               — ステージ集約
│       └── StageLike.kt           — ステージライクインターフェース
│
├── application/                   — アプリケーション層 (ユースケース)
│   └── quest/
│       └── QuestService.kt        — クエストサービスクラス
│
├── infrastructure/                — インフラ層 (実装詳細)
│   ├── bukkit/
│   │   ├── BukkitActionDispatcher.kt  — ActionDispatcher の Bukkit 実装
│   │   ├── BukkitQuestNotifier.kt     — QuestNotifier の Bukkit 実装
│   │   └── BukkitScriptRunner.kt      — ScriptRunner の Bukkit 実装
│   ├── di/
│   │   └── InfrastructureModule.kt    — Dagger モジュール
│   └── persistence/
│       └── QuestRepositoryImpl.kt     — QuestRepository の DB 実装
│
├── di/                            — DI 設定
│   ├── BukkitModule.kt            — Bukkit 依存提供
│   ├── ConfigModule.kt            — Config 提供
│   ├── DatabaseModule.kt          — DB/Redis 提供
│   ├── RegistryModule.kt          — レジストリ提供
│   └── SimpleQuestComponent.kt    — Dagger ルートコンポーネント
│
├── data/                          — データ・シリアライゼーション
│   ├── Config.kt                  — config.yml 定義
│   ├── QuestVersion.kt            — クエストバージョン管理
│   └── yaml/
│       ├── QuestConverter.kt      — YAML → QuestType 変換
│       └── QuestDef.kt            — YAML 中間表現
│
├── database/                      — データベース層
│   ├── BackupService.kt           — 定期バックアップ
│   ├── DatabaseHelper.kt          — DB ヘルパー
│   ├── DatabaseManager.kt         — HikariCP 接続管理
│   ├── DiscordWebhook.kt          — Discord Webhook 通知
│   ├── MigrationRunner.kt         — Flyway マイグレーション
│   ├── RedisManager.kt            — Redis 接続管理
│   ├── SyncService.kt             — マルチサーバー同期
│   ├── migration/
│   │   └── V1__InitialSetup.kt    — 初期テーブル作成
│   ├── repository/                — リポジトリ実装
│   └── table/                     — Exposed テーブル定義
│       ├── PlayerQuestTypes.kt
│       ├── QuestCompletions.kt
│       ├── QuestDefinitions.kt
│       └── QuestProgress.kt
│
├── gui/                           — Kunectron GUI
│   ├── ConfirmGui.kt
│   ├── PartyCreateGui.kt
│   ├── PartyGui.kt
│   ├── PartyInviteGui.kt
│   ├── PartyMenuGui.kt
│   ├── QuestDetailGui.kt
│   ├── QuestGui.kt
│   ├── QuestMenuGui.kt
│   ├── QuestPanelGui.kt
│   ├── SearchGui.kt
│   ├── SearchableGui.kt
│   └── UpdatableGui.kt
│
├── command/                       — コマンド定義
│   └── FormulaArgumentType.kt     — 進捗変更用の数式パーサー
│
├── listener/                      — イベントリスナー
│   ├── PlayerListener.kt          — プレイヤー Join/Quit 管理
│   └── QuestProgressListener.kt   — クエスト進捗イベントフック
│
├── party/                         — パーティ実装
│   ├── InvitationSetting.kt
│   ├── Invite.kt
│   ├── InviteManager.kt
│   ├── Party.kt                   — Party インターフェース
│   ├── PartyImpl.kt               — 通常パーティ実装
│   ├── PartyManager.kt            — パーティ管理
│   └── SoloPartyImpl.kt           — ソロパーティ実装
│
├── quest/                         — クエスト実装
│   ├── AcceptConditions.kt
│   ├── GameGuide.kt
│   ├── PlayLimits.kt
│   ├── Progresses.kt
│   ├── Quest.kt
│   ├── QuestCategory.kt
│   ├── QuestManager.kt            — クエストインスタンス管理
│   ├── QuestRequirement.kt
│   ├── QuestState.kt
│   └── QuestType.kt
│
└── registry/                      — レジストリ定義
    ├── DomainQuestTypes.kt        — QuestType レジストリ
    ├── Keyed.kt
    ├── QuestCategories.kt         — ビルドインカテゴリ
    ├── QuestTypes.kt
    ├── Registry.kt
    └── Stages.kt                  — Stage レジストリ
```

## 依存関係

| ライブラリ | 用途 |
|---|---|
| Paper API (compileOnly) | Bukkit/Paper サーバー API |
| Kunectron (compileOnly) | GUI フレームワーク |
| cloud (Incendo) | コマンドフレームワーク (Brigadier ラッパー) |
| Dagger 2 | DI コンテナ |
| kaml | YAML デシリアライズ |
| kotlinx.serialization | シリアライズアノテーション |
| Exposed | ORM / テーブル定義 |
| Flyway | DB マイグレーション |
| HikariCP | DB コネクションプール |
| MariaDB (JDBC) | 永続データベース |
| Redis (Jedis) | メッセージング・キャッシュ |
| ktor-client | HTTP クライアント (Webhook) |

## データストレージ設計

### 方針

- **静的データ (定義)**: YAML ファイルで管理。起動時 / リロード時に `SimpleQuestLoader` が読み込み、ドメインオブジェクトに変換して `DomainQuestTypes` レジストリに登録。
- **動的データ (プレイヤー状態)**: MariaDB で永続化 (Exposed ORM + Flyway マイグレーション)。
- **メッセージング**: Redis PubSub + キャッシュ。
- **書式**: MythicMobs 類似の YAML 書式を採用。

### フロー

```
YAML (定義)
  │ 起動時 / リロード時
  ▼
SimpleQuestLoader → QuestConverter → DomainQuestTypes レジストリ登録
                    │
                    ▼ (write-to-mysql = true の場合)
              quest_definitions テーブルに UPSERT (SyncService)
                                      │
プレイヤー操作  → クエスト進行        → MariaDB (進捗・完了履歴・解放状態)
                                      │
                                  Redis PubSub (通知・同期)
```

## 起動フロー

### onEnable (SimpleQuest)

1. `config.yml` 読み込み → `SimpleQuestConfig` にデシリアライズ
2. Dagger DI コンポーネント (`SimpleQuestComponent`) をビルド
3. 各種マネージャーを DI から取得
4. Flyway マイグレーション実行 (テーブル自動作成)
5. ビルドインカテゴリ登録 (`QuestCategories`)
6. `SimpleQuestLoader.loadAll()` で YAML → レジストリ登録
7. cloud コマンドマネージャーで Brigadier コマンド登録
8. Listener 登録
9. 定期バックアップ開始 (設定による)
10. Redis 接続確認

### リロード時 (`/simplequest reload`)

1. `DomainQuestTypes` レジストリをクリア
2. `--use-local` / `--use-mysql` があればコンフリクト解決
3. `SyncService.sync()` で同期
4. `SimpleQuestLoader.loadAll()` で再読み込み

## レジストリパターン

`Registry<T: Keyed>` は `LinkedHashMap<String, T>` によるインメモリレジストリ。

- `register(entry)` — エントリ追加
- `get(key: String)` — キーで取得
- `unregister(key: String)` — 削除
- `entries` — 全エントリコレクション
- `keys` / `size` — キーセット / サイズ

## アクションシステム (Actions)

```kotlin
// domain/action/ActionType.kt
enum class ActionType {
    COMMAND,           // コマンド実行
    ITEM_GIVE,         // Minecraft アイテム付与
    MYTHIC_ITEM_GIVE,  // MythicMobs アイテム付与 (未実装)
    PVELEVEL_EXP,      // PvELevel 経験値付与 (未実装)
}

// domain/action/Action.kt
data class Action(
    val type: ActionType,
    val material: String? = null,   // ITEM_GIVE 用
    val amount: Int? = null,
    val item: String? = null,       // MYTHIC_ITEM_GIVE 用
    val command: String? = null,    // COMMAND 用
)

// domain/action/ActionSet.kt
data class ActionSet(
    val onFirstComplete: List<Action> = emptyList(),
    val onComplete: List<Action> = emptyList(),
)
```

Action はクエスト完了時・初回完了時に `BukkitActionDispatcher` を介して実行される。

## Stage システム

Stage はクエストの段階的進行を管理する。

```kotlin
// domain/stage/StageLike.kt
interface StageLike {
    val key: String
    fun mount()
    fun unmount()
}

// domain/stage/Stage.kt
interface Stage : StageLike {
    val active: Boolean
    val tasks: List<StageTask>
}

data class StageTask(
    val key: String,
    val description: String,
    val isComplete: Boolean = false,
)
```

## マルチサーバー同期

`SyncService` が YAML ↔ MySQL 間の同期を管理。詳細は [MULTI_SERVER.md](MULTI_SERVER.md) 参照。
