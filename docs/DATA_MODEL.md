# データモデル

YAML からデシリアライズされる data class と、データベーススキーマの設計。

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

# Redis
redis:
  host: "localhost"
  port: 6379
  password: ""
  user: ""                # Redis 6+ ACL ユーザー名 (default ユーザー利用時は空文字)

# パーティーの最大サイズ
maxPartySize: 8

# パーティー招待の有効期限 (tick数, default: 1200 = 60秒)
partyInviteLimit: 1200

# クエストパネルの設定 (スコアボード)
panel:
  title: '&dSimpleQuest'
  footer: '&7azisaba.net'

# マルチサーバー同期設定
multi-server:
  write-to-mysql: false
  write-to-yaml: false
  conflict-mode: "LOCAL"
  backup:
    enabled: false
    interval-hours: 24
    retention-days: 30
    directory: "plugins/SimpleQuest/backups/"

# Discord Webhook 通知 (コンフリクト通知用)
discord:
  webhook-url: ""
```

## QuestType (`domain/quest/model/QuestType.kt`)

`@namespace/types/...yml` に対応。YAML から `QuestConverter` を介して変換される。

```kotlin
data class QuestType(
    val key: String,
    val title: String,
    val icon: Icon,
    val description: List<String> = emptyList(),
    val category: String = "lq:general",
    val location: Location? = null,
    val giver: String? = null,
    val playLimits: PlayLimits = PlayLimits(),
    val acceptConditions: AcceptConditions = AcceptConditions(),
    val maxPlayers: Int? = null,
    val minPlayers: Int? = null,
    val deathLimit: Int? = null,
    val guides: List<GameGuide> = emptyList(),
    val requirements: Map<String, QuestRequirement> = emptyMap(),
    val actions: ActionSet? = null,
    val scripts: List<Script> = emptyList(),
)
```

### YAML ↔ コード マッピング

| YAML キー | Kotlin フィールド | 形式 |
|---|---|---|
| `Title` | `title` | String |
| `Description` | `description` | List\<String\> |
| `Icon` | `icon` | `"MATERIAL"` または `"MATERIAL:CMD"` |
| `Aura` | `icon.aura` | Boolean |
| `Giver` | `giver` | String? |
| `Category` | `category` | `"lq:general"` など |
| `Location` | `location` | `"world,x,y,z[,yaw,pitch]"` |
| `Options.MaxParty` | `maxPlayers` / `minPlayers` | `"min-max"` または `"max"` |
| `Options.Limits.*` | `playLimits` | PlayLimits |
| `Options.DeathLimit` | `deathLimit` | Int? |
| `Requirements.PvELevel` | `acceptConditions.pveLevel` | Int? |
| `Requirements.Money` | `acceptConditions.money` | Double? |
| `Requirements.PartyMode` | `acceptConditions.partyMode` | Boolean |
| `Objectives.*` | `requirements` | Map\<String, QuestRequirement\> |
| `Actions.OnFirstComplete` | `actions.onFirstComplete` | List\<Action\> |
| `Actions.OnComplete` | `actions.onComplete` | List\<Action\> |
| `Guides` | `guides` | List\<GameGuide\> |
| `Scripts.*` | `scripts` | List\<Script\> |
| `Unlock` | (パースのみ、未使用) | List\<UnlockDef\> |

## PlayLimits（受注可能回数制限）

```kotlin
data class PlayLimits(
    val daily: Int? = null,
    val weekly: Int? = null,
    val monthly: Int? = null,
    val yearly: Int? = null,
    val lifetime: Int? = null,
)
```

## AcceptConditions（受注条件）

```kotlin
data class AcceptConditions(
    val pveLevel: Int? = null,
    val money: Double? = null,
    val requiredQuests: List<String>? = null,
    val permissions: List<String>? = null,
    val partyMode: Boolean = false,
)
```

## QuestRequirement（達成条件）

```kotlin
data class QuestRequirement(
    val key: String,
    val amount: Int,
)
```

## ActionSet / Action（アクション定義）

```kotlin
// domain/action/ActionType.kt
enum class ActionType {
    COMMAND,
    ITEM_GIVE,
    MYTHIC_ITEM_GIVE,
    PVELEVEL_EXP,
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

## Script（スクリプト定義）

```kotlin
// domain/script/Script.kt
data class Script(
    val trigger: Trigger,
    val delay: Long = 0L,
    val commands: List<String> = emptyList(),
) {
    enum class Trigger {
        START,
        END,
        COMPLETE,
        CANCEL,
    }
}
```

## Icon (`domain/data/Icon.kt`)

```kotlin
data class Icon(
    val type: String,
    val customModelData: Int? = null,
    val aura: Boolean = false,
    val model: String? = null,
)
```

## GameGuide (`domain/quest/model/GameGuide.kt`)

```kotlin
data class GameGuide(
    val title: String? = null,
    val location: Location,
    val requirements: Map<String, Int> = emptyMap(),
)
```

## Location (`domain/data/Location.kt`)

```kotlin
data class Location(
    val world: String = "world",
    val x: Double = 0.0,
    val y: Double = 64.0,
    val z: Double = 0.0,
    val yaw: Float = 0.0f,
    val pitch: Float = 0.0f,
)
```

## EndReason / QuestState

```kotlin
enum class EndReason {
    COMPLETE, CANCEL, DEATH_LIMIT, PLUGIN, RELOAD, OTHER
}

enum class QuestState {
    ACTIVE, COMPLETED, CANCELLED, FAILED
}
```

## データベーススキーマ

Flyway + Exposed で管理。全テーブルは `V1__InitialSetup` マイグレーションで自動生成される。

### テーブル一覧

| テーブル | 用途 |
|---|---|
| `quest_progress` | プレイヤーのクエスト進捗 |
| `quest_completions` | クエスト完了履歴 (プレイ制限計算用) |
| `player_quest_types` | プレイヤーの解放済み QuestType |
| `quest_definitions` | マルチサーバー同期用のクエスト定義キャッシュ |

### quest_progress

```sql
CREATE TABLE quest_progress (
    player_uuid BINARY(16) NOT NULL,
    quest_key   VARCHAR(255) NOT NULL,
    req_key     VARCHAR(255) NOT NULL,
    progress    INT NOT NULL DEFAULT 0,
    PRIMARY KEY (player_uuid, quest_key, req_key)
);
```

### quest_completions

```sql
CREATE TABLE quest_completions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_uuid  BINARY(16) NOT NULL,
    quest_key    VARCHAR(255) NOT NULL,
    completed_at VARCHAR(32) NOT NULL
);
```

`completed_at` は文字列形式のタイムスタンプ (推奨フォーマット: ISO 8601)。

### player_quest_types

```sql
CREATE TABLE player_quest_types (
    player_uuid BINARY(16) NOT NULL,
    quest_key   VARCHAR(255) NOT NULL,
    plays       INT NOT NULL DEFAULT 0,
    PRIMARY KEY (player_uuid, quest_key)
);
```

### quest_definitions (マルチサーバー同期用)

```sql
CREATE TABLE quest_definitions (
    quest_key  VARCHAR(255) PRIMARY KEY,
    yaml_text  MEDIUMTEXT NOT NULL,
    checksum   VARCHAR(64) NOT NULL,       -- SHA-256
    updated_at VARCHAR(32) NOT NULL,
    updated_by VARCHAR(36) NOT NULL,       -- サーバーID (UUID)
    conflict   BOOLEAN NOT NULL DEFAULT FALSE
);
```
