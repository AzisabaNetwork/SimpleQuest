# 実装計画

> パーサー（YAML クエスト記法読み込み）を除く SimpleQuest の実装を段階的に進める。
> 各フェーズの詳細タスクは `implPlans/` 配下の個別ファイルを参照。

| Phase | ファイル | 内容 |
|---|---|---|
| 0 | [00-project-bootstrap.md](../implPlans/00-project-bootstrap.md) | Gradle, plugin.yml, エントリポイント |
| 1 | [01-infrastructure.md](../implPlans/01-infrastructure.md) | レジストリ, DB接続, migration, リポジトリ |
| 2 | [02-domain-models.md](../implPlans/02-domain-models.md) | Quest/Party/Stage/Action/Script ドメイン |
| 3 | [03-quest-engine.md](../implPlans/03-quest-engine.md) | ライフサイクル, 進捗, Action発火 |
| 4 | [04-commands.md](../implPlans/04-commands.md) | /simplequest, /party |
| 5 | [05-listeners.md](../implPlans/05-listeners.md) | 退出/死亡/エリア侵入 |
| 6 | [06-gui.md](../implPlans/06-gui.md) | Kunectron GUI |

---

## フェーズ構成

```
Phase 0: プロジェクト土台
  └─ Phase 1: インフラ (レジストリ / DB / Config)
        ├─ Phase 2A: ドメインモデル (quest / party / stage / action / script)
        ├─ Phase 2B: DB テーブル + リポジトリ
        │
        └─ Phase 3: クエストエンジン (ライフサイクル / 進捗管理)
              ├─ Phase 4: コマンド (/simplequest, /party)
              ├─ Phase 5: イベントリスナー
              │
              └─ Phase 6: GUI 基盤 (Kunectron 導入)
```

---

## Phase 0: プロジェクト土台

 Gradle セットアップ + プラグインエントリポイント

| # | タスク | ファイル | 内容 |
|---|---|---|---|
| 0.1 | Gradle ビルド定義 | `build.gradle.kts` | Kotlin JVM, paperweight-userdev, kotlinx-serialization |
| 0.2 | Version Catalog | `gradle/libs.versions.toml` | 全依存バージョン一元管理 |
| 0.3 | 設定ファイル | `settings.gradle.kts` | プロジェクト名、リポジトリ |
| 0.4 | Plugin エントリポイント | `SimpleQuest.kt` | JavaPlugin 継承。onEnable / onDisable |
| 0.5 | Bootstrap | `SimpleQuestBootstrap.kt` | PluginBootstrap でコマンド登録 |
| 0.6 | plugin.yml | `src/main/resources/plugin.yml` | プラグイン定義 |
| 0.7 | Config スキーマ | `Config.kt` | config.yml 対応 data class |
| 0.8 | デフォルト config | `src/main/resources/config.yml` | 配布用 config |

**依存**: なし

---

## Phase 1: インフラ基盤

### 1A: レジストリパターン

| # | タスク | ファイル | 内容 |
|---|---|---|---|
| 1.1 | Keyed インターフェース | `registry/Keyed.kt` | Adventure Key + NamespacedKey 統合 |
| 1.2 | Registry クラス | `registry/Registry.kt` | LinkedHashMap ベースのジェネリックレジストリ |
| 1.3 | ビルドインカテゴリ | `registry/QuestCategories.kt` | general / daily / story / event |

### 1B: データベース基盤

| # | タスク | ファイル | 内容 |
|---|---|---|---|
| 1.4 | Database マネージャー | `database/Database.kt` | HikariCP 接続プール管理。Exposed 初期化 |
| 1.5 | Migration: quest_progress | `database/migration/...` | Exposed Migration クラス |
| 1.6 | Migration: quest_completions | 同上 | |
| 1.7 | Migration: player_quest_types | 同上 | |
| 1.8 | Migration ランナー | 同上 | 起動時に全 migration を実行 |
| 1.9 | QuestProgressRepository | `database/repository/...` | 進捗 CRUD |
| 1.10 | QuestCompletionRepository | 同上 | 完了履歴 + 集計 |
| 1.11 | PlayerQuestTypeRepository | 同上 | 解放管理 CRUD |

**依存**: Phase 0

---

## Phase 2: ドメインモデル

### 2A: Quest ドメイン

| # | タスク | ファイル | 内容 |
|---|---|---|---|
| 2.1 | QuestType | `quest/QuestType.kt` | クエスト種別 (不変オブジェクト) |
| 2.2 | QuestCategory | `quest/QuestCategory.kt` | カテゴリ |
| 2.3 | QuestRequirement | `quest/QuestRequirement.kt` | 達成条件 (key, amount) |
| 2.4 | QuestState / EndReason | `quest/QuestState.kt` | 状態遷移と終了理由 enum |
| 2.5 | Quest インターフェース | `quest/Quest.kt` | 実行中クエストの契約 |
| 2.6 | Progresses | `quest/Progresses.kt` | 進捗管理 (Map<Requirement, Int>) |
| 2.7 | PlayLimits | `quest/PlayLimits.kt` | 周期制限 (Weekly/Monthly/Yearly/Lifetime) |
| 2.8 | AcceptConditions | `quest/AcceptConditions.kt` | 受注条件 (PvELevel/前提クエスト/権限) |
| 2.9 | Guide | `quest/Guide.kt` | ガイド (Nav パーティクル / ActionBar) |
| 2.10 | QuestImpl | `quest/QuestImpl.kt` | Quest 実装 |

### 2B: Party ドメイン

| # | タスク | ファイル | 内容 |
|---|---|---|---|
| 2.11 | Party インターフェース | `party/Party.kt` | パーティ契約 |
| 2.12 | PartyImpl | `party/PartyImpl.kt` | マルチプレイヤーパーティ |
| 2.13 | SoloPartyImpl | `party/SoloPartyImpl.kt` | ソロラッパー |
| 2.14 | Invite | `party/Invite.kt` | 招待管理 |
| 2.15 | InvitationSetting | `party/InvitationSetting.kt` | LEADER / ALL |

### 2C: Stage ドメイン

| # | タスク | ファイル | 内容 |
|---|---|---|---|
| 2.16 | Stage インターフェース | `stage/Stage.kt` | ステージ契約 |
| 2.17 | Queue | `stage/Queue.kt` | 待機キュー |
| 2.18 | StageLike | `stage/StageLike.kt` | Stage / Queue 共通インターフェース |
| 2.19 | StageImpl | `stage/StageImpl.kt` | Stage 実装 |

### 2D: Action ドメイン

| # | タスク | ファイル | 内容 |
|---|---|---|---|
| 2.20 | ActionType | `action/ActionType.kt` | アクション種別 enum |
| 2.21 | ActionSet | `action/ActionSet.kt` | onFirstComplete / onComplete |
| 2.22 | CommandAction | `action/impl/CommandAction.kt` | コマンド実行 |
| 2.23 | ItemGiveAction | `action/impl/ItemGiveAction.kt` | アイテム付与 |
| 2.24 | MythicItemGiveAction | `action/impl/MythicItemGiveAction.kt` | MM アイテム付与 (他プラグイン依存) |
| 2.25 | PvELevelExpAction | `action/impl/PvELevelExpAction.kt` | PvELevel Exp 付与 (他プラグイン依存) |

### 2E: Script ドメイン

| # | タスク | ファイル | 内容 |
|---|---|---|---|
| 2.26 | Script インターフェース | `script/Script.kt` | Trigger / delay / commands |
| 2.27 | ScriptImpl | `script/ScriptImpl.kt` | BukkitScheduler での遅延実行 |
| 2.28 | ScriptTrigger | `script/ScriptTrigger.kt` | START / END / COMPLETE / CANCEL enum |

### 2F: データクラス (YAML スキーマ対応、パーサー以外)

| # | タスク | ファイル | 内容 |
|---|---|---|---|
| 2.29 | Icon | `data/Icon.kt` | Material + CustomModelData + Aura |
| 2.30 | Location | `data/Location.kt` | ワールド + 座標 |
| 2.31 | Panel | `data/Panel.kt` | スコアボード設定 |

**依存**: Phase 1 (レジストリ, DB)

---

## Phase 3: クエストエンジン

| # | タスク | ファイル | 内容 |
|---|---|---|---|
| 3.1 | QuestManager | `quest/QuestManager.kt` | クエストライフサイクル管理 |
| 3.2 | startQuest() | `quest/QuestManager.kt` | クエスト開始 → Party 紐付け |
| 3.3 | endQuest() | `quest/QuestManager.kt` | 完了/中断 → Action/Script 発火 |
| 3.4 | progress 変更 | `quest/Progresses.kt` | 進捗更新 → 自動完了判定 |
| 3.5 | Daily 抽選 | `quest/DailyQuestManager.kt` | デイリー抽選ロジック |
| 3.6 | PlayLimit 判定 | `quest/PlayLimits.kt` | DB 集計による制限確認 |
| 3.7 | Guide Nav | `guide/GuideService.kt` | パーティクルナビ + ActionBar |
| 3.8 | ActionRunner | `action/ActionRunner.kt` | Action の実行ディスパッチャー |

**依存**: Phase 2

---

## Phase 4: コマンドシステム

| # | タスク | ファイル | 内容 |
|---|---|---|---|
| 4.1 | SimpleQuestCommand | `command/SimpleQuestCommand.kt` | /simplequest メインコマンド |
| 4.2 | grant / revoke | 同上 | 解放管理サブコマンド |
| 4.3 | progress (数式パーサ) | 同上 | 進捗変更 (FormulaArgumentType) |
| 4.4 | reload | 同上 | リロード |
| 4.5 | stage mount / unmount | 同上 | ステージ管理 |
| 4.6 | PartyCommand | `command/PartyCommand.kt` | /party invite / accept / kick |
| 4.7 | 権限チェック | 同上 | 権限ベースのアクセス制御 |

**依存**: Phase 3

---

## Phase 5: イベントリスナー

| # | タスク | ファイル | 内容 |
|---|---|---|---|
| 5.1 | PlayerQuitEvent | `listener/PlayerListener.kt` | 退出 → party / quest 離脱 |
| 5.2 | PlayerDeathEvent | 同上 | 死亡 → カウント / 離脱 |
| 5.3 | エリア侵入検知 | `listener/AreaListener.kt` | エリア侵入 → クエスト開放 (パーサー待ち) |

**依存**: Phase 3

---

## Phase 6: GUI 基盤

| # | タスク | ファイル | 内容 |
|---|---|---|---|
| 6.1 | Kunectron 依存追加 | build.gradle.kts | compileOnly |
| 6.2 | QuestGui | `gui/QuestGui.kt` | クエスト選択 ChestGui |
| 6.3 | QuestPanelGui | `gui/QuestPanelGui.kt` | スコアボード |
| 6.4 | PartyMenuGui | `gui/PartyMenuGui.kt` | パーティ管理 |
| 6.5 | ConfirmGui | `gui/ConfirmGui.kt` | 確認ダイアログ |
| 6.6 | SearchGui | `gui/SearchGui.kt` | 検索看板 |
| 6.7 | UpdatableGui | `gui/UpdatableGui.kt` | 動的更新インターフェース |

**依存**: Phase 3, 4

---

## マイルストーン

| マイルストーン | 完了条件 | 想定工数 |
|---|---|---|
| **M0: 起動する** | サーバーに突っ込んで SimpleQuest が有効化される。config.yml が生成される | 小 |
| **M1: DB につながる** | HikariCP + Exposed で MariaDB に接続、migration が流れる | 小 |
| **M2: コマンドが動く** | `/simplequest reload` 等がエラーなく実行できる | 中 |
| **M3: クエストが開始/完了できる** | 簡易テストでクエスト開始〜完了〜Action 実行まで通る | 大 |
| **M4: パーティシステム** | `paty invite/accept` でパーティが組める | 中 |
| **M5: GUI が出る** | QuestGui が表示される | 中 |

---

## 優先順位提案

Phase 0 → Phase 1 → Phase 2A/2D/2E/2F → Phase 3 → Phase 4 → Phase 2B/2C → Phase 5 → Phase 6

つまり:

```
土台 → DB → ドメインモデル → エンジン → コマンド → Party/Stage → リスナー → GUI
```

ドメインモデルと DB テーブルは密接に関連するので、**2A と 2B は並行** または **2A → 2B の順** が現実的です。
