# クエストシステム

SimpleQuest のクエストの内部設計とライフサイクルに関する設計ドキュメントです。

> ユーザー向けのクエスト記法リファレンスは **[wiki/QUEST_CREATION.md](../wiki/QUEST_CREATION.md)** を参照してください。

## QuestType (クエスト種別)

`QuestType` は YAML からロードされ `DomainQuestTypes` レジストリに登録される不変オブジェクト。

### データモデル

```kotlin
data class QuestType(
    val key: String,
    val title: String,
    val icon: Icon,                    // "MATERIAL:CMD" 形式
    val description: List<String>,
    val category: String,              // "lq:general" など
    val location: Location?,           // "world,x,y,z[,yaw,pitch]" 形式
    val giver: String?,
    val playLimits: PlayLimits,
    val acceptConditions: AcceptConditions,
    val maxPlayers: Int?,
    val minPlayers: Int?,
    val deathLimit: Int?,
    val guides: List<GameGuide>,
    val requirements: Map<String, QuestRequirement>,
    val actions: ActionSet?,
    val scripts: List<Script>,
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

### YAML 完全例

```yaml
QuestName:
  Title: "&aクエストタイトル"
  Description:
    - "&7説明文"
  Icon: "DIAMOND:5001"
  Aura: true
  Giver: "&e依頼人"
  Category: "lq:general"
  Location: "world,100,64,200"

  Options:
    MaxParty: "2-4"
    Limits:
      Daily: 3
      Weekly: 10
      Monthly: 30
      Yearly: 365
      Lifetime: 100
    DeathLimit: 3

  Requirements:
    PvELevel: 10
    Money: 100.0
    PartyMode: true

  Objectives:
    KillZombie: 10
    BreakStone: 5
    CollectDiamond: "minecraft:diamond*3"

  Actions:
    OnFirstComplete:
      - Type: Command
        Params: "give % minecraft:diamond 5"
    OnComplete:
      - Type: Item
        Params: "minecraft:emerald,3"

  Guides:
    - Title: "&a目的地"
      Location: "80,64,180"
      Condition: "req=1"

  Scripts:
    OnStart: ["say start"]
    OnComplete: ["say done"]
    OnCancel: ["say cancelled"]

  Unlock:
    - EnterArea: "world,100,64,200,10"
```

## PlayLimits（受注可能回数制限）

各周期の制限回数。

| 周期 | 説明 | リセットタイミング | 実装状況 |
|---|---|---|---|
| `Daily` | 日間制限 | 毎日 0:00 | ✅ |
| `Weekly` | 週間制限 | 毎週月曜 0:00 | ✅ |
| `Monthly` | 月間制限 | 毎月1日 0:00 | ✅ |
| `Yearly` | 年間制限 | 毎年1月1日 0:00 | ✅ |
| `Lifetime` | 生涯制限 | なし | ✅ |

- DB の `quest_completions` テーブルで完了日時を記録し、SQL の集計で制限超過を判定

## AcceptConditions（受注条件）

`Requirements` セクションで定義。

| 条件 | YAML | 型 | 実装 |
|---|---|---|---|
| PvE レベル | `Requirements.PvELevel` | Int? | ✅ (チェックのみ) |
| 所持金 | `Requirements.Money` | Double? | ✅ (チェックのみ) |
| パーティ必須 | `Requirements.PartyMode` | Boolean | ✅ |

### バリデーション順序

1. **解放状態** — プレイヤーが QuestType を解放 (grant) 済みか
2. **プレイ制限** — 各周期の制限回数未満か
3. **受注条件** — 上記条件をすべて満たすか
4. **パーティ条件** — パーティサイズが min/max 範囲内か
5. **全メンバー** — 全メンバーが解放済みか

## QuestRequirement（達成条件）

`Objectives` セクションで定義。

```kotlin
data class QuestRequirement(
    val key: String,   // 例: "KillZombie", "BreakStone"
    val amount: Int,   // 必要数
)
```

Objective Key の命名規則により、対応する Bukkit イベントが自動的にトリガーされます。詳細は **[wiki/quest/EVENTS_AND_OBJECTIVES.md](../wiki/quest/EVENTS_AND_OBJECTIVES.md)** を参照。

## Progresses（進捗管理）

```kotlin
class Progresses(requirements: Map<String, QuestRequirement>)
```

- Quest インスタンスごとに生成
- 全要件が達成されると `quest.end(COMPLETE)` が自動実行
- 進捗は MariaDB `quest_progress` テーブルに永続化

## Actions（アクションシステム）

`Actions` セクションで定義。詳細は **[wiki/quest/ACTIONS.md](../wiki/quest/ACTIONS.md)** を参照。

### アクション種別

| Type | 説明 | 実装 |
|---|---|---|
| `Command` | コンソールコマンド実行 | ✅ |
| `Item` | Minecraft アイテム付与 | ✅ |
| `MythicItem` | MythicMobs アイテム付与 | ⚠️ 未実装 |
| `PvELevel` | PvELevel 経験値付与 | ⚠️ 未実装 |

### トリガータイミング

| トリガー | 実行タイミング |
|---|---|
| `OnFirstComplete` | 各プレイヤーが初めてクエストをクリアした時 |
| `OnComplete` | クエストクリアのたび毎回 |

## Quest ライフサイクル

### 状態遷移

```
[未開始] → START → [実行中] → COMPLETE / CANCEL / DEATH_LIMIT / PLUGIN / RELOAD → [終了]
```

### EndReason

| 値 | 意味 |
|---|---|
| `COMPLETE` | 全要件達成 |
| `CANCEL` | プレイヤーによる中断 |
| `DEATH_LIMIT` | 死亡回数上限超過 |
| `PLUGIN` | プラグイン停止 |
| `RELOAD` | `/simplequest reload` による強制終了 |

### 発火順序

```
end(COMPLETE):
  1. Scripts.OnComplete 実行
  2. Actions.OnFirstComplete 実行（初回のみ）
  3. Actions.OnComplete 実行

end(CANCEL):
  1. Scripts.OnCancel 実行
```

## Guides（ナビゲーションガイド）

Guide は進行中の目標地点を示す。`Guides` セクションで定義。

- `type.guides` リストの先頭から評価され、条件を満たした最初の Guide が採用
- 5 tick 間隔で再評価（QuestDetailGui の 2x2 クラフト枠 UI とは別）
- パーティクルナビゲーション + ActionBar 表示

## Grant / Revoke（解放管理）

プレイヤーがクエストを開始するには grant が必要。

- `/simplequest grant <player> <questType>` で解放
- `/simplequest revoke <player> <questType>` で剥奪
- MariaDB `player_quest_types` テーブルで永続化

## UI

- **クエスト選択**: QuestGui（54 スロットチェスト GUI）
- **クエスト詳細・受注**: Accept/Cancel ボタン付き詳細画面
- **クエスト進行中**: QuestPanelGui（スコアボード）+ QuestDetailGui（2x2 クラフト枠）
- **パーティ管理**: PartyMenuGui（チェスト GUI）
- 詳細は [GUI.md](GUI.md) 参照
