# データモデル

YAML からデシリアライズされる data class と、データベーススキーマの設計。

> **Note**: 以下は初期案です。
> MythicMobs ライクな記法への置き換えは [QUEST_FORMAT_DESIGN.md](QUEST_FORMAT_DESIGN.md) で検討中。

## Config (`data/Config.kt`)

`plugins/SimpleQuest/config.yml` に対応。

```yaml
# データベース
database:
  host: "localhost"
  port: 3306
  name: "simplequest"
  user: "root"
  password: ""

redis:
  host: "localhost"
  port: 6379
  password: ""

# パーティーの最大サイズ
maxPartySize: 8

# パーティー招待の有効期限 (tick数, default: 1200 = 60秒)
partyInviteLimit: 1200

# クエスト終了後にテレポートする場所 (任意)
lobby:
  world: 'minecraft:overworld'
  x: 0
  y: 0
  z: 0
  yaw: 0   # 任意
  pitch: 0 # 任意

# クエストパネルの設定 (スコアボード)
panel:
  title: '&dSimpleQuest'
  footer: '&7いますぐ &eazisaba.net&7 で遊べ！'
```

## QuestType (`data/QuestType.kt`)

`@namespace/types/...yml` に対応。

```yaml
title: "&aタイトル"
icon:
  type: "minecraft:diamond"
  model: "minecraft:..."    # String? (任意; ItemModel / CustomModelData)
  custom-model-data: 100    # Int? (任意; CustomModelData)
  aura: true                # Boolean (任意; エンチャントオーラ)
description:
  - "&7一行目"
giver: "&e依頼人"           # String? (任意; 依頼人名)
category: "lq:general"      # String (namespace:key)

# 受注条件
requirements:
  level:
    pve: 10                 # PvELevel 条件
    # quest: ["lq:prologue"] # 前提クエスト (TODO)
    # permission: "group.vip" # LuckPerms権限 (TODO)
  party-mode: true           # Boolean? (任意; パーティ必須)
  # death-limit: 3           # Int? (任意; 死亡可能回数) (TODO)

# 受注可能回数制限
play-limits:
  weekly: 3                  # Int? (任意)
  monthly: 10                # Int? (任意)
  yearly: 100                # Int? (任意)
  lifetime: 1                # Int? (任意; デフォルト)

# 位置・パーティ制限
location:
  world: "minecraft:overworld"
  x: 0.0
  y: 0.0
  z: 0.0
  yaw: 0.0
  pitch: 0.0

maxPlays: 6                  # Int? (任意; 旧互換 / lifetime と等価)
maxPlayers: 6                # Int? (任意; 最大パーティサイズ)
minPlayers: 3                # Int? (任意; 最小パーティサイズ)

# ガイド
guides:
  - title: "&aチェックポイント"
    location:
      world: "..."
      x: 0
      y: 0
      z: 0
    requirements:
      req1: 1

# 完了条件
requirements:
  requirement1: 3
  requirement2: 5

# 報酬アクション (新規)
actions:
  on-first-complete:         # 初回達成時のみ
    - type: "item_give"
      material: "minecraft:diamond"
      amount: 1
    - type: "mythic_item_give"
      item: "MMOre"
      amount: 3
  on-complete:               # 毎回の達成時
    - type: "command"
      command: "say %player% completed!"
    - type: "pvelevel_exp"
      amount: 100

# スクリプト
scripts:
  start:
    - "command1"
  start+20:
    - "command2"
  complete:
    - ":lq grant % namespace:reward"
```

### QuestType ドメインクラス (案)

```kotlin
class QuestType(
    override val key: Key,
    private val data: QuestTypeData  // YAML由来
) : Keyed {
    val title: Component
    val icon: Icon
    val description: List<Component>
    val giver: Component?              // 依頼人名
    val category: QuestCategory
    val location: org.bukkit.Location
    val playLimits: PlayLimits         // Weekly/Monthly/Yearly/Lifetime
    val deathLimit: Int?               // 死亡可能回数
    val acceptConditions: AcceptConditions  // 受注条件
    val maxPlayers: Int?
    val minPlayers: Int?
    val guides: List<Guide>
    val requirements: Map<String, QuestRequirement>
    val actions: ActionSet             // アクション定義
    val scripts: List<Script>
}
```

## PlayLimits（受注可能回数制限）

```kotlin
data class PlayLimits(
    val weekly: Int? = null,
    val monthly: Int? = null,
    val yearly: Int? = null,
    val lifetime: Int? = null
)
```

プレイヤーのプレイ回数を各周期で管理し、制限に達したクエストは受注不可。

## AcceptConditions（受注条件）

```kotlin
data class AcceptConditions(
    val pveLevel: Int? = null,            // 必要 PvELevel
    val requiredQuests: List<Key>?,       // 前提クエスト
    val permissions: List<String>?,       // 必要権限 (LuckPerms)
    val partyMode: Boolean = false        // パーティ必須
)
```

## ActionSet / Action（アクション定義）

```kotlin
@Serializable
data class Action(
    val type: ActionType,
    val material: String? = null,   // ITEM_GIVE 用
    val amount: Int? = null,
    val item: String? = null,       // MYTHIC_ITEM_GIVE / コマンド用
    val command: String? = null     // COMMAND 用
)

enum class ActionType {
    COMMAND,
    ITEM_GIVE,
    MYTHIC_ITEM_GIVE,
    PVELEVEL_EXP
}

data class ActionSet(
    val onFirstComplete: List<Action> = emptyList(),
    val onComplete: List<Action> = emptyList(),
    // 将来的に onStart, onProgress など
)
```

## QuestCategory (`data/QuestCategory.kt`)

`@namespace/categories/...yml` に対応。

```yaml
title: "&a春イベント"
icon:
  type: "minecraft:cherry_sapling"
  aura: true
```

## Stage (`data/Stage.kt`)

`@namespace/stages/...yml` に対応。

```yaml
title: "&aボス戦"
location:
  world: "minecraft:overworld"
  x: 0.0
  y: 0.0
  z: 0.0
unmountLocation:
  world: "minecraft:overworld"
  x: 0.0
  y: 0.0
  z: 0.0
maxParties: 1
```

## Icon (`data/Icon.kt`)

```kotlin
@Serializable
data class Icon(
    val type: String,
    val model: String? = null,
    @SerialName("custom-model-data") val customModelData: Int? = null,  // 追加
    val aura: Boolean = false
)
```

## Guide (`data/Guide.kt`)

```kotlin
@Serializable
data class Guide(
    val title: String? = null,
    val location: Location,
    val requirements: Map<String, Int> = emptyMap()
)
```

## Location (`data/Location.kt`)

```kotlin
@Serializable
data class Location(
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float = 0.0f,
    val pitch: Float = 0.0f
)
```

## Panel (`data/Panel.kt`)

```kotlin
@Serializable
data class Panel(
    val title: String,
    val footer: String
)
```

## データベーススキーマ (案)

### テーブル一覧

| テーブル | 用途 |
|---|---|
| `quest_progress` | プレイヤーのクエスト進捗 |
| `quest_completions` | クエスト完了履歴 (プレイ制限計算用) |
| `player_quest_types` | プレイヤーの解放済み QuestType (旧 PDC) |
| `party_data` | パーティ情報 (永続化が必要な場合) |

### quest_progress (案)

```sql
CREATE TABLE quest_progress (
    player_uuid BINARY(16) NOT NULL,
    quest_key VARCHAR(255) NOT NULL,
    requirement_key VARCHAR(255) NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    PRIMARY KEY (player_uuid, quest_key, requirement_key)
);
```

### quest_completions (案)

```sql
CREATE TABLE quest_completions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_uuid BINARY(16) NOT NULL,
    quest_key VARCHAR(255) NOT NULL,
    completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_player_quest (player_uuid, quest_key),
    INDEX idx_completed_at (completed_at)
);
```
