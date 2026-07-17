# Plan: Copy Quem GUI System → LifeQuest

## ⚠️ 最重要: クレジット表記（絶対遵守）

**すべての移植ファイルの先頭に以下のコメントを必ず記載すること。**
1行目はパッケージ宣言より上に置く。

```kotlin
// Original: https://github.com/AzisabaNetwork/quem by tksimeji
// Adapted for LifeQuest
```

**チェックリスト（全ファイル完了前に確認）:**

- [ ] 新規作成する全 `.kt` ファイルにクレジットコメントがあるか
- [ ] 既存書き換えファイルにもクレジットコメントを追加したか
- [ ] README.md に `Original: AzisabaNetwork/quem by tksimeji` を記載したか
- [ ] `git diff --stat feat/gui-quem-like` で全ファイルを確認したか

クレジット漏れは許されない。コミット前に必ず全ファイル grep すること:

```bash
grep -rL "AzisabaNetwork/quem" src/main/kotlin/net/azisaba/lifequest/gui/
```

---

Quem（AzisabaNetwork/quem, by tksimeji）の GUI システムを LifeQuest に移植する。
Kunectron の annotation-based API（`@ChestGui`）を採用し、既存の GuiBuilder ベースの実装を置き換える。

## Quem → LifeQuest マッピング

### API スタイル変更

| 項目 | Quem (コピー元) | LifeQuest (現在) | 移行後 |
|------|----------------|-----------------|--------|
| GUI定義 | `@ChestGui` annotation | `GuiBuilder.chest()` | **`@ChestGui` annotation** |
| プレイヤー注入 | `@ChestGui.Player` | 引数で渡す | **`@ChestGui.Player`** |
| イベント | `@GuiHandler` | `.handler()` コールバック | **`@GuiHandler`** |
| 更新 | `UpdatableGui.update()` | 直接 open() | **`UpdatableGui` interface** |
| 検索 | `SearchableGui.search()` | なし | **`SearchableGui` interface** |

### ファイル単位の移行計画

| # | Quem ファイル | LifeQuest の対応 | 作業内容 |
|---|-------------|-----------------|---------|
| 1 | `ConfirmGui.kt` | `gui/ConfirmGui.kt` (既存) | 既存の GuiBuilder → `@ChestGui` に書き換え |
| 2 | `PartyCreateGui.kt` | **新規作成** | そのまま移植（パッケージ名変更のみ） |
| 3 | `PartyGui.kt` | **新規作成** | 抽象ベースクラスとして移植 |
| 4 | `PartyInviteGui.kt` | **新規作成** | ページネーション付き招待 GUI |
| 5 | `PartyMenuGui.kt` | `gui/PartyMenuGui.kt` (既存) | 既存の GuiBuilder → `@ChestGui` + Quem の機能追加 |
| 6 | `QuestGui.kt` | `gui/QuestGui.kt` (既存) | カテゴリタブ・検索・ページネーション追加 |
| 7 | `QuestMenuGui.kt` | **新規作成** | クエスト詳細/脱退メニュー |
| 8 | `QuestPanelGui.kt` | `gui/QuestPanelGui.kt` (既存) | `@ScoreboardGui` 化 + ステージ/HP表示追加 |
| 9 | `SearchGui.kt` | **新規作成** | Sign GUI 検索 |
| 10 | `SearchableGui.kt` | **新規作成** | 検索インターフェース |
| 11 | `UpdatableGui.kt` | **新規作成** | 更新インターフェース |

## 依存関係

### Quem クラス → LifeQuest クラス マッピング

GUI ファイルが参照する Quem の型と LifeQuest での対応：

| Quem クラス | LifeQuest での対応 |
|------------|------------------|
| `Party` (interface) | `net.azisaba.lifequest.domain.party.model.Party` |
| `Party.create()` | → `PartyImpl` 相当（DI 経由） |
| `Party.solo()` | → `SoloPartyImpl` 相当 |
| `Party.InvitationSetting` | `InvitationSetting` enum |
| `Quest` (class) | `net.azisaba.lifequest.quest.Quest`（or domain Quest） |
| `QuestType` (data) | `net.azisaba.lifequest.domain.quest.model.QuestType` |
| `QuestCategory` | `net.azisaba.lifequest.domain.quest.model.QuestCategory` |
| `QuestTypes` (registry) | `DomainQuestTypes` |
| `QuestCategories` | `QuestCategories` |
| `Stage` / `Stage.Queue` | `net.azisaba.lifequest.domain.stage.*` |
| `Progresses` | `net.azisaba.lifequest.domain.quest.model.Progresses` |
| `Quem.pluginConfig.panel` | `PanelConfig`（DI 注入済み） |
| `Quem.PLUGIN_ID` | `"lifequest"` |
| `player.party` (extension) | `PartyManager.getParty(player)` |
| `player.dailyQuestTypes` (extension) | （要実装 or 省略） |
| `player.hasPermission()` | `QuestService.hasPermission()` |

### 要調整・要実装

1. **`Party.create(player)`** — Quem では static factory。LifeQuest では PartyManager/DI 経由。
   → `PartyCreateGui` 内で `PartyManager` 相当を呼ぶように変更
2. **`player.party` extension** — LifeQuest にない。`PartyManager.getParty(player)` で代替
3. **`player.dailyQuestTypes`** — LifeQuest にない。Daily カテゴリフィルタは `QuestRepository` 経由で代替 or スキップ
4. **`Component.translatable()`** — LifeQuest は i18n 未対応。`Component.text()` に置換
5. **`ItemType`** (Paper 1.21.11) — Quem が使っている。LifeQuest の Paper API バージョンでも利用可能

## 実装順序（推奨）

```
Phase 1: インターフェース
  ├── SearchableGui.kt     （新規、依存なし）
  └── UpdatableGui.kt      （新規、依存なし）

Phase 2: 単純な GUI
  ├── ConfirmGui.kt        （既存を書き換え）
  ├── SearchGui.kt          （新規、SearchableGui に依存）
  └── QuestMenuGui.kt       （新規）

Phase 3: パーティ GUI
  ├── PartyGui.kt           （新規、抽象ベース）
  ├── PartyCreateGui.kt     （新規、PartyGui, PartyMenuGui に依存）
  ├── PartyInviteGui.kt     （新規、PartyGui, SearchableGui に依存）
  └── PartyMenuGui.kt       （既存を大幅書き換え）

Phase 4: クエスト GUI
  ├── QuestGui.kt           （既存を大幅書き換え、SearchableGui 依存）
  └── QuestPanelGui.kt      （既存を書き換え、ScoreboardGui 化）

Phase 5: 統合テスト & README
  ├── Kunectron での動作確認
  └── README.md にクレジット追記
```

## README.md 追記内容（Phase 5 で実施）

GUI セクションの先頭に以下を追記:

```markdown
## GUI System

> **Original: [AzisabaNetwork/quem](https://github.com/AzisabaNetwork/quem) by tksimeji**
>
> GUI システム全体は Quem の Kunectron ベース実装を移植・改変したものです。
> 個別ファイルにもソース元クレジットを記載しています。

（以下、既存の GUI 説明があれば続ける）
```

**このセクションの追記はブランチマージ前に必須。**

## 注意点

- Kunectron は `compileOnly` → サーバーに Kunectron プラグインが必須
- `@ChestGui` annotation は Kunectron の annotation processor で処理される
- テスト (`./gradlew test`) は Kunectron 非依存なので影響なし
- `Component.translatable()` → `Component.text()` に置換（i18n 未対応のため）
- ドメインモデルの差異（Quest, Party の interface 違い）はアダプタで吸収
