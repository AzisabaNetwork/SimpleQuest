# アーキテクチャ概要

## プロジェクト概要

**SimpleQuest** は [アジ鯖](https://www.azisaba.net) の Reincarnation PvE サーバー向けに開発された Paper プラグインのクエストマネージャーです。

- **言語**: Kotlin (JVM)
- **対象 API**: Paper 1.21.11
- **ライセンス**: (未定)

## 依存関係 (予定)

| ライブラリ | 用途 |
|---|---|
| Paper API (compileOnly) | Bukkit/Paper サーバー API |
| Kunectron (compileOnly) | GUI フレームワーク (@ChestGui, @ScoreboardGui, @SignGui) ※検討中 |
| kaml / snakeyaml | YAML デシリアライズ |
| kotlinx.serialization | シリアライズアノテーション |
| MariaDB (JDBC) / Exposed | 永続データベース |
| Redis (Jedis/Lettuce) | メッセージング・キャッシュ |
| Adventure API | メッセージ・コンポーネント |

## データストレージ設計

### 方針

- **静的データ (定義)**: YAML ファイルで管理。起動時 / リロード時にデータベースへ吸い上げ。
- **動的データ (プレイヤー状態)**: MariaDB で永続化。
- **メッセージング**: Redis を利用。
- **書式**: MythicMobs 類似の YAML 書式を採用予定。

### フロー

```
YAML (定義)
  │ 起動時 / リロード時
  ▼
QuemLoader 相当 → レジストリ登録 → 必要に応じて MariaDB にキャッシュ
                                      │
プレイヤー操作  → クエスト進行      → MariaDB (進捗・完了履歴)
                                      │
                                  Redis (通知・同期)
```

## アクションシステム (Actions)

スクリプトシステムに加えて、**Action** システムを導入する。

```kotlin
// 概念
data class Action(
    val type: ActionType,    // アクション種別
    val data: Map<String, String>  // 種別ごとのパラメータ
)

enum class ActionType {
    COMMAND,           // コマンド実行
    ITEM_GIVE,         // Minecraft アイテム付与
    MYTHIC_ITEM_GIVE,  // MythicMobs アイテム付与
    PVELEVEL_EXP,      // PvELevel 経験値付与
}
```

Action はクエスト完了時・初回完了時・特定トリガー時に実行される。

## パッケージ構成 (案)

```
net.azisaba.simplequest
├── SimpleQuest.kt               — プラグインエントリポイント
├── SimpleQuestBootstrap.kt      — PluginBootstrap (ライフサイクル)
├── SimpleQuestLoader.kt         — YAML 読み込み → レジストリ / DB 登録
│
├── quest/                     — クエストドメイン
│   ├── Quest.kt
│   ├── QuestType.kt
│   ├── QuestCategory.kt
│   ├── QuestRequirement.kt
│   └── Progresses.kt
│
├── party/                     — パーティ
│   ├── Party.kt
│   └── Invite.kt
│
├── stage/                     — ステージ
│   ├── Stage.kt
│   └── Queue.kt
│
├── script/                    — スクリプト
│   └── Script.kt
│
├── action/                    — アクションシステム
│   ├── Action.kt
│   ├── ActionType.kt
│   └── actions/
│       ├── CommandAction.kt
│       ├── ItemGiveAction.kt
│       ├── MythicItemGiveAction.kt
│       └── PvELevelExpAction.kt
│
├── command/                   — Brigadier コマンド
│   ├── SimpleQuestCommand.kt
│   └── PartyCommand.kt
│
├── data/                      — シリアライゼーション
│   ├── Config.kt
│   ├── QuestType.kt
│   ├── QuestCategory.kt
│   ├── Stage.kt
│   ├── Icon.kt
│   ├── Guide.kt
│   ├── Location.kt
│   ├── Action.kt
│   └── Requirement.kt
│
├── database/                  — DB 層
│   ├── Database.kt
│   ├── tables/
│   └── repository/
│
├── gui/                       — GUI 定義
│   ├── QuestGui.kt
│   ├── QuestDetailGui.kt     — 2x2 クラフト枠 UI (要検討)
│   └── ...
│
├── listener/
│   └── PlayerListener.kt
│
└── registry/                  — レジストリ
    ├── Registry.kt
    ├── Keyed.kt
    ├── QuestCategories.kt
    ├── QuestTypes.kt
    └── Stages.kt
```

## 起動フロー (案)

1. **PluginBootstrap** (SimpleQuestBootstrap)
   - LifecycleEvents.COMMANDS 購読
   - コマンド登録

2. **onEnable** (SimpleQuest)
   - プラグインディレクトリ作成
   - `config.yml` 読み込み
   - MariaDB 接続プール初期化 / テーブル自動作成
   - Redis 接続
   - YAML 定義 → レジストリ登録 → DB 同期
   - Listener 登録

3. **リロード時** (`/simplequest reload`)
   - 全レジストリクリア (ビルドイン除く)
   - YAML 再スキャン → DB 再同期
   - config.yml 再読み込み

## レジストリパターン

`Registry<T: Keyed>` は LinkedHashMap<Key, T> によるインメモリレジストリ。

- `register(entry)` — エントリ追加
- `get(key)` — Key で取得
- `unregister(key)` — 削除
- `entries` — 全エントリ set ビュー
