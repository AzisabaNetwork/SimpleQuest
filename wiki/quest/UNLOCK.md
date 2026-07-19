# クエスト解放条件 (Unlock)

`Unlock` セクションでは、クエストがプレイヤーに**自動解放**される条件を定義します。

> **実装状況**: ⚠️ スキーマ定義のみ。YAML のパースは可能ですが、実際の解放処理は未実装です。
> 現在は `/simplequest grant` コマンドで手動解放してください。

## 目次

1. [基本構造](#基本構造)
2. [解放トリガー](#解放トリガー)
3. [具体例](#具体例)

---

## 基本構造

```yaml
QuestName:
  Title: "&a森の探索"
  # ... その他の設定 ...
  Unlock:
    - EnterArea: "world,100,64,200,10"    # 特定エリア進入で解放
    - EnterArea: "world,200,70,300,5"     # 複数指定可（OR条件）
```

---

## 解放トリガー

### EnterArea（エリア進入）

プレイヤーが指定された座標から一定距離以内に入ると、クエストが自動解放されます。

| パラメータ | 説明 |
|---|---|
| `EnterArea` | `"world,x,y,z,radius"` 形式 |

```
"world,100,64,200,10"
  ↑     ↑   ↑  ↑   ↑
  |     |   |  |   └─ 半径（ブロック数）
  |     |   |  └─── Z 座標
  |     |   └────── Y 座標
  |     └────────── X 座標
  └──────────────── ワールド名
```

```yaml
Unlock:
  - EnterArea: "world,0,64,0,20"       # スポーン周辺 20 ブロック以内
  - EnterArea: "world_nether,0,70,0,5" # ネザーの特定座標 5 ブロック以内
```

- 半径で指定した範囲にプレイヤーが入ると解放
- 複数指定した場合は **OR 条件**（いずれか1つに入れば解放）
- 既に解放済みの場合は何もしない（重複解放なし）

> **実装状況**: ❌ 未実装。YAML の読み取りは可能ですが、実際のエリア進入検知と解放処理は実装されていません。

---

## 具体例

### 例1: 特定エリア進入で解放

```yaml
ForestQuest:
  Title: "&a森の探索"
  Description:
    - "&7森に入ると自動で受注可能になります"
  Icon: "OAK_SAPLING"
  Category: "lq:general"
  Location: "world,200,64,100"
  Unlock:
    - EnterArea: "world,200,64,100,30"
  Objectives:
    BreakOakLog: 20
    KillZombie: 5
  Actions:
    OnComplete:
      - Type: Item
        Params: "minecraft:emerald,3"
```

### 例2: 複数エリアで解放

```yaml
WorldExplorer:
  Title: "&e世界探索"
  Description:
    - "&7指定エリアに到達せよ"
  Icon: "COMPASS"
  Category: "lq:story"
  Unlock:
    - EnterArea: "world,500,64,500,20"
    - EnterArea: "world,-500,64,-500,20"
    - EnterArea: "world_nether,0,70,0,10"
  Objectives:
    BreakStone: 50
  Actions:
    OnFirstComplete:
      - Type: Command
        Params: "give % minecraft:diamond 10"
```

---

## 現在の制限と今後の予定

| 機能 | 状況 |
|---|---|
| `EnterArea` の YAML パース | ✅ 対応 |
| エリア進入検知 | ❌ 未実装 |
| 自動解放処理 | ❌ 未実装 |
| 前提クエスト完了 (`RequiredQuests`) | 📋 計画中 |
| 権限チェック (`Permission`) | 📋 計画中 |
| レベル条件 (`PvELevel`) | 📋 計画中 |

> **当面の回避策**: `/simplequest grant <player> <questType>` コマンドで手動解放してください。
