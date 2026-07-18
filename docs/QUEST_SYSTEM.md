# クエストシステム

## QuestType (クエスト種別)

QuestType は**クエストの種別**を表す不変オブジェクト。YAML からロードされ、`QuestTypes` レジストリに Key で登録される。

### ドメイン層 (案)

```kotlin
class QuestType(
    override val key: Key,
    private val data: QuestTypeData  // YAML由来
) : Keyed {
    val title: Component
    val icon: Icon
    val description: List<Component>
    val giver: Component?              // 依頼人名 (新規)
    val category: QuestCategory
    val location: org.bukkit.Location
    val playLimits: PlayLimits         // Weekly/Monthly/Yearly/Lifetime
    val deathLimit: Int?               // 死亡可能回数 (新規)
    val acceptConditions: AcceptConditions  // 受注条件 (新規)
    val maxPlayers: Int?
    val minPlayers: Int?
    val guides: List<Guide>
    val requirements: Map<String, QuestRequirement>
    val actions: ActionSet             // アクション定義 (新規)
    val scripts: List<Script>
}
```

### YAML 定義例

```yaml
title: "&a森の探索"
icon:
  type: "minecraft:iron_sword"
  custom-model-data: 5001
  aura: true
description:
  - "&7森に潜む魔物を討伐せよ"
giver: "&e村長"
category: "lq:general"

# 受注条件
accept-conditions:
  pve-level: 10
  # required-quests:
  #   - "lq:prologue"
  # permissions:
  #   - "group.vip"
  party-mode: true
  death-limit: 3

# プレイ制限
play-limits:
  lifetime: 5
  weekly: 3

location:
  world: "minecraft:overworld"
  x: 100.0
  y: 64.0
  z: 200.0

maxPlayers: 4
minPlayers: 1

requirements:
  kill_zombie: 10
  collect_item: 3

actions:
  on-first-complete:
    - type: "mythic_item_give"
      item: "MMOre"
      amount: 5
  on-complete:
    - type: "command"
      command: "say %player% が森の探索を完了した！"
    - type: "pvelevel_exp"
      amount: 50

guides:
  - title: "&a森の入り口"
    location:
      world: "minecraft:overworld"
      x: 80
      y: 64
      y: 180
    requirements:
      kill_zombie: 1
```

## PlayLimits（受注可能回数制限）

プレイヤーがクエストを受注・完了できる回数を周期ごとに制限する。

| 周期 | 説明 | リセットタイミング |
|---|---|---|
| `weekly` | 週間制限 | 毎週月曜 0:00 |
| `monthly` | 月間制限 | 毎月1日 0:00 |
| `yearly` | 年間制限 | 毎年1月1日 0:00 |
| `lifetime` | 生涯制限 | なし |

- `maxPlays` は `lifetime` のエイリアスとして扱う
- DB の `quest_completions` テーブルで完了日時を記録し、SQL の集計で制限超過を判定

## AcceptConditions（受注条件）

プレイヤーがクエストを受注するための条件。

| 条件 | 型 | 説明 |
|---|---|---|
| `pve-level` | Int? | 必要 PvELevel |
| `required-quests` | List\<Key\>? | 完了必須の前提クエスト |
| `permissions` | List\<String\>? | 必要権限 (LuckPerms) |
| `party-mode` | Boolean | true の場合、パーティ必須 |
| `death-limit` | Int? | クエスト中の許容死亡回数 |

### バリデーション順序

1. **解放状態** — プレイヤーが QuestType を解放 (grant) 済みか
2. **プレイ制限** — 各周期の制限回数未満か
3. **受注条件** — 上記条件をすべて満たすか
4. **パーティ条件** — パーティサイズが min/max 範囲内か
5. **全メンバー** — 全メンバーが解放済みか

### 死亡回数管理

- `death-limit` が設定されている場合、クエスト中のプレイヤーの死亡回数を追跡
- `PlayerDeathEvent` でカウントし、制限超過時に `quest.removePlayer(player)` または Quest 失敗

## Actions（アクションシステム）

スクリプトシステムに加えて、構造化されたアクションシステムを導入する。

### アクション種別

| 種別 | 説明 | YAML 例 |
|---|---|---|
| `command` | コンソールコマンド実行 | `{ type: "command", command: "say hi" }` |
| `item_give` | Minecraft アイテム付与 | `{ type: "item_give", material: "minecraft:diamond", amount: 1 }` |
| `mythic_item_give` | MythicMobs アイテム付与 | `{ type: "mythic_item_give", item: "MMOre", amount: 3 }` |
| `pvelevel_exp` | PvELevel 経験値付与 | `{ type: "pvelevel_exp", amount: 100 }` |

### トリガータイミング

- `on-first-complete` — 各プレイヤーが初めてクエストを完了した時のみ実行
- `on-complete` — クエスト完了時に毎回実行
- 将来的に `on-start`, `on-progress` なども追加可能

### プレースホルダー

コマンド内の `%player%` / `%` は実行プレイヤー名に置換される。

## QuestCategory (クエストカテゴリ)

QuestType を分類するためのタブ。`QuestCategories` レジストリで管理。

### ビルドインカテゴリ

| Key | 表示名 | アイコン | 説明 |
|---|---|---|---|
| `lq:general` | General | CHEST | 一般的なクエスト |
| `lq:daily` | Daily | CLOCK | デイリークエスト (抽選対象) |
| `lq:story` | Story | ENCHANTED_BOOK | ストーリークエスト |
| `lq:event` | Event | (variable) | イベントクエスト (新規) |

ビルドインカテゴリはリロード時に削除されない。

## QuestRequirement (達成条件)

```kotlin
data class QuestRequirement(
    val key: String,   // 要件の識別子 (例: "kill_zombie")
    val amount: Int    // 必要な値
)
```

## Progresses (進捗管理)

```kotlin
class Progresses internal constructor(private val quest: Quest)
```

- Quest インスタンスごとに生成される
- `quest.type.requirements` の各要件をキーにした `Map<QuestRequirement, Int>` で進捗を保持
- 全要件の進捗値が `amount` 以上になった時点で `quest.end(COMPLETE)` が自動実行される
- 進捗は MariaDB `quest_progress` テーブルに永続化 (中断後再接続時に復元)

### 進捗変更

```kotlin
progresses[requirement] = newValue
// セッター内で完了判定 + DB 書き込みを行う
```

## Quest ライフサイクル

### 状態遷移

```
[未開始] → START → [実行中] → COMPLETE / CANCEL / OTHER / PLUGIN / RELOAD → [終了]
```

### EndReason

| 値 | 意味 |
|---|---|
| `COMPLETE` | 全要件達成 |
| `CANCEL` | 未達成で終了 (死亡 limit 超過含む) |
| `DEATH_LIMIT` | 死亡回数上限超過 (新規) |
| `OTHER` | その他の理由 |
| `PLUGIN` | プラグイン停止 |
| `RELOAD` | `/simplequest reload` による強制終了 |

### 発火順序

```
end(COMPLETE):
  1. Script.Trigger.START (開始時)
  2. ... (クエスト進行中)
  3. END スクリプト
  4. COMPLETE スクリプト
  5. on-first-complete アクション (初回のみ)
  6. on-complete アクション
  7. プレイ回数 +1 (DB 記録)

end(CANCEL):
  1. END スクリプト
  2. CANCEL スクリプト
```

## Guide システム

Guide はクエスト中の進行度に応じて表示される目標を示す。

```kotlin
val guide: Guide?  // requirements がすべて満たされた最初の Guide
```

- `type.guides` リストの先頭から評価され、条件を満たした最初の Guide が採用される
- 5tick 間隔の BukkitRunnable で常に再評価
- 現在地から Guide 位置までのパーティクルナビゲーション
- ActionBar にガイドタイトル表示

## Grant / Revoke（解放管理）

プレイヤーがクエストを開始するには、その QuestType が**解放 (grant)** されている必要がある。

### 永続化 (DB)

PDC ではなく MariaDB `player_quest_types` テーブルで管理する。

```sql
CREATE TABLE player_quest_types (
    player_uuid BINARY(16) NOT NULL,
    quest_key VARCHAR(255) NOT NULL,
    plays INT NOT NULL DEFAULT 0,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (player_uuid, quest_key)
);
```

### 権限チェック

```kotlin
Player.hasPermission(type: QuestType): Boolean
// true = 解放済み かつ 全周期のプレイ制限未満
```

## Daily Quest（デイリー抽選）

`lq:daily` カテゴリに属する QuestType は、デイリー抽選の対象となる。

```kotlin
Player.dailyQuestTypes: Set<QuestType>
```

- 毎日最初のアクセス時に、解放済み daily クエストから抽選
- 抽選結果は DB または Redis に保持
- 日付変更は Calendar の YEAR + DAY_OF_YEAR で判定

## イベントクエスト連携

`lq:event` カテゴリのクエストは、イベントサーバーと本鯖の2パターンで運用可能。

### パターン A: イベントサーバー専用

- イベントサーバー上でのみ受注・進行可能
- 本鯖にはデータを引き継がない

### パターン B: 本鯖連携

- イベントサーバーでクエストを受注・進行
- 本鯖でも進捗の参照・継続が可能
- フロー: サーバー移動 → クエスト進行 (進捗は DB で共有)

### 実装案

- 進捗データは MariaDB で一元管理 (サーバー間で同一 DB を見る)
- Redis Pub/Sub で進捗変更イベントを通知
- グローバルな Quest インスタンス管理 (要検討)

## プレイヤーの離脱

| トリガー | 処理 |
|---|---|
| 死亡 (PlayerDeathEvent) | 死亡回数カウント → death-limit 超過時は離脱 |
| 退出 (PlayerQuitEvent) | `party.removeMember(player)` → `quest.removePlayer(player)` |
| 手動脱退 | GUI またはコマンド経由 |

`removePlayer` 後もクエスト自体は継続 (残りプレイヤーで進行可能)。全プレイヤーが離脱した場合もクエストは自動終了しない。

## UI 表示（Lore ベース）

クエストの詳細情報は Lore ベースで表示する（専用 GUI への遷移は行わない）。

- プレイヤーの 2x2 クラフトスロットを UI 呼び出しに使用 (要検討)
- 情報表示: Lore による説明 + 進捗状況
- 情報更新: 自動更新（負荷が高ければインベントリ開封時・クリック時に更新）
- アクション: 中断 (ConfirmGui), パーティー確認
- 詳細は [GUI.md](GUI.md) 参照
