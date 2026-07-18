# クエスト定義の作成

LifeQuest のクエストは **YAML ファイル** で定義します。MythicMobs ライクな記法を採用しており、
直感的に記述できます。

## 目次

1. [ファイルの配置](#ファイルの配置)
2. [YAML 基本構造](#yaml-基本構造)
3. [各セクションの詳細](#各セクションの詳細)
4. [具体例](#具体例)
5. [Tips](#tips)

---

## ファイルの配置

```
plugins/LifeQuest/
└── @namespace/         ← 任意の名前空間（例: @lq）
    └── types/          ← ここに .yml ファイルを配置
        ├── wolf_slayer.yml
        └── supply_procurement.yml
```

- ファイル拡張子は **`.yml`** である必要があります
- 文字コードは **UTF-8**（BOM なし）で保存してください
- 1 ファイルに複数のクエスト定義を含めることができます

---

## YAML 基本構造

```yaml
# QuestName が YAML のトップレベルキー（クエスト ID）
QuestName:
  Title: "&aクエストタイトル"
  Description:
    - "&7説明文1行目"
    - "&7説明文2行目"
  Icon: "MATERIAL:CUSTOM_MODEL_DATA"   # または MATERIAL のみ
  Aura: true                            # エンチャント光沢（任意）
  Giver: "&e依頼人"                     # 任意
  Category: "lq:general"                # カテゴリキー
  Location: "world,x,y,z"               # ワールド,座標

  Options:
    MaxParty: "1-4"                     # "min-max" または "max"
    Limits:                             # 受注・完了可能回数制限
      Daily: 3
      Weekly: 10
      Monthly: 30
      Yearly: 365
      Lifetime: 100
    DeathLimit: 3                       # クエスト中の許容死亡回数

  Requirements:                         # 受注条件
    PvELevel: 10
    Money: 100.0

  Objectives:                           # 達成条件
    KillZombie: 10                      # 数値
    # またはアイテム収集:
    # CollectDiamond: "minecraft:diamond*5"

  Actions:                              # 完了時アクション
    OnFirstComplete:                    # 初回クリア時のみ
      - Type: Command
        Params: "give % minecraft:diamond"
    OnComplete:                         # 毎回
      - Type: Item
        Params: "minecraft:diamond,5"
      - Type: MythicItem
        Params: "MMOre,3"
      - Type: PvELevel
        Params: "100"

  Guides:                               # ナビゲーションガイド
    - Title: "&a目的地"
      Location: "x,y,z"
      Condition: "req=1"               # 条件（任意）

  Scripts:                              # スクリプト（任意）
    OnStart: ["say start"]
    OnStart+20: ["say delayed"]        # 遅延実行（tick）
    OnComplete: ["say done"]
    OnCancel: ["say cancelled"]
```

---

## 各セクションの詳細

### Title（必須）

クエストの表示名です。`&` カラーコードが使用可能です。

```yaml
Title: "&c&l緊急クエスト"
```

### Description（必須）

クエストの説明文です。リスト形式で複数行記述できます。

```yaml
Description:
  - "&7森に潜む魔物を討伐せよ"
  - "&7残り: %progress%"
```

### Icon（必須）

クエスト選択 GUI に表示するアイコンです。

```yaml
# 基本: MATERIAL のみ
Icon: "IRON_SWORD"

# カスタムモデルデータ付き
Icon: "IRON_SWORD:5001"

# プレイヤーヘッド
Icon: "PLAYER_HEAD:base64string"
```

### Aura（任意）

`true` にするとエンチャント光沢エフェクトが付きます。デフォルトは `false`。

```yaml
Aura: true
```

### Giver（任意）

クエストの依頼人名です。

```yaml
Giver: "&e村長"
```

### Category（必須）

クエストのカテゴリです。以下のビルトインカテゴリが利用可能です：

| キー | 表示名 | 説明 |
|---|---|---|
| `lq:general` | General | 一般クエスト |
| `lq:daily` | Daily | デイリークエスト（抽選対象） |
| `lq:story` | Story | ストーリークエスト |
| `lq:event` | Event | イベントクエスト |

```yaml
Category: "lq:general"
```

### Location（必須）

クエストの開始地点です。`world,x,y,z` 形式で指定します。

```yaml
Location: "world,100,64,200"
```

### Options

#### MaxParty

パーティの最小・最大人数です。

```yaml
Options:
  MaxParty: "1-4"    # 1〜4人（ソロ可）
  MaxParty: "4"      # 4人固定（ソロ不可）
```

#### Limits

クエストの受注・完了可能回数を周期ごとに制限します。

| 周期 | リセットタイミング |
|---|---|
| `Daily` | 毎日 0:00 |
| `Weekly` | 毎週月曜 0:00 |
| `Monthly` | 毎月1日 0:00 |
| `Yearly` | 毎年1月1日 0:00 |
| `Lifetime` | なし |

```yaml
Options:
  Limits:
    Daily: 3
    Weekly: 10
    Lifetime: 100
```

> [!NOTE]
> すべての制限を設定する必要はありません。必要な周期のみ指定してください。

#### DeathLimit

クエスト中の許容死亡回数です。超過するとクエスト失敗になります。

```yaml
Options:
  DeathLimit: 3      # 3回死亡可能
  # DeathLimit を省略 → 死亡回数制限なし
```

### Requirements（受注条件）

クエストを受注するために必要な条件です。

```yaml
Requirements:
  PvELevel: 10       # PvE レベル
  Money: 100.0        # 所持金
  PartyMode: true     # パーティ必須
```

### Objectives（達成条件）

クエスト完了に必要な目標です。

```yaml
# 数値のみ: キー名がそのまま Mob 名/アイテム名として解釈される
Objectives:
  KillZombie: 10

# アイテム収集: "material*amount" 形式
Objectives:
  CollectDiamond: "minecraft:diamond*5"
```

### Actions（アクション）

クエスト完了時に実行される報酬アクションです。

#### アクション種別

| Type | 説明 | Params 形式 |
|---|---|---|
| `Command` | コンソールコマンド実行 | `"コマンド文字列"` |
| `Item` | Minecraft アイテム付与 | `"material,amount"` |
| `MythicItem` | MythicMobs アイテム付与 | `"ItemName,amount"` |
| `PvELevel` | PvE 経験値付与 | `"amount"` |

#### トリガー

| トリガー | 実行タイミング |
|---|---|
| `OnFirstComplete` | 各プレイヤーが初めてクリアした時のみ |
| `OnComplete` | クリアするたび毎回 |

```yaml
Actions:
  OnFirstComplete:
    - Type: Command
      Params: "give % minecraft:diamond 5"
    - Type: MythicItem
      Params: "LegendarySword,1"
  OnComplete:
    - Type: Item
      Params: "minecraft:emerald,3"
    - Type: PvELevel
      Params: "50"
```

> [!NOTE]
> コマンド内の `%` は実行プレイヤー名に置換されます。

### Guides（任意）

プレイヤーを目的地まで誘導するナビゲーションガイドです。
パーティクルと ActionBar で表示されます。

```yaml
Guides:
  - Title: "&a森の入り口"
    Location: "80,64,180"
    Condition: "req=1"       # 条件付き表示（任意）
  - Title: "&cボス部屋"
    Location: "150,64,200"
```

### Scripts（任意）

クエストの各イベントで実行されるスクリプトです。

```yaml
Scripts:
  OnStart: ["say クエスト開始！"]
  OnStart+20: ["say 開始から1秒後"]    # +tick で遅延実行
  OnComplete: ["say クエストクリア！"]
  OnCancel: ["say クエスト失敗..."]
```

---

## 具体例

### 例1: シンプルな討伐クエスト

```yaml
WolfSlayer:
  Title: "&cWolf Slayer"
  Description:
    - "&7森に出没する狼を討伐せよ"
  Icon: "BONE"
  Category: "lq:general"
  Location: "world,100,64,200"
  Options:
    MaxParty: "1-4"
    Limits:
      Daily: 5
    DeathLimit: 3
  Requirements:
    PvELevel: 5
  Objectives:
    KillWolf: 10
  Actions:
    OnComplete:
      - Type: Item
        Params: "minecraft:bone,5"
      - Type: PvELevel
        Params: "50"
```

### 例2: アイテム収集クエスト

```yaml
SupplyProcurement:
  Title: "&e物資調達"
  Description:
    - "&7指定された物資を集めよ"
    - "&7ダイヤモンド: 0/5"
    - "&7鉄インゴット: 0/20"
  Icon: "CHEST"
  Giver: "&e商人"
  Category: "lq:daily"
  Location: "world,50,64,50"
  Options:
    Limits:
      Daily: 1
  Objectives:
    CollectDiamond: "minecraft:diamond*5"
    CollectIron: "minecraft:iron_ingot*20"
  Actions:
    OnComplete:
      - Type: Command
        Params: "give % minecraft:emerald 10"
      - Type: PvELevel
        Params: "30"
```

### 例3: 初回クリア報酬付きクエスト

```yaml
DragonSlayer:
  Title: "&5&lDragon Slayer"
  Description:
    - "&dエンダードラゴンを討伐せよ"
    - "&7※初回クリアで特別報酬あり"
  Icon: "DRAGON_HEAD"
  Aura: true
  Category: "lq:story"
  Location: "world_the_end,0,70,0"
  Options:
    MaxParty: "1-8"
    Limits:
      Lifetime: 1
    DeathLimit: 5
  Requirements:
    PvELevel: 50
  Objectives:
    KillDragon: 1
  Actions:
    OnFirstComplete:
      - Type: Command
        Params: "give % minecraft:netherite_sword{display:'{Name:\"&dDragonbane\"}'}"
      - Type: MythicItem
        Params: "DragonScale,5"
    OnComplete:
      - Type: PvELevel
        Params: "500"
  Scripts:
    OnStart: ["title % title \"&5Dragon Slayer\" \"&dエンダードラゴンを討伐せよ！\""]
    OnComplete: ["title % title \"&aClear!\" \"&fおめでとう！\""]
    OnCancel: ["title % title \"&cFailed...\" \"\""]
```

---

## Tips

### 名前空間の追加

新しい名前空間を追加するには、`plugins/LifeQuest/` 以下にディレクトリを作成します：

```
plugins/LifeQuest/
├── @lq/           ← デフォルト
│   └── types/
│       ├── quest_a.yml
│       └── quest_b.yml
└── @custom/       ← 追加した名前空間
    └── types/
        └── event_quest.yml
```

### リロードで反映

YAML ファイルを編集したら必ずリロードしてください：

```bash
/lifequest reload
```

### よくある間違い

| 問題 | 原因 | 対策 |
|---|---|---|
| YAML が読み込まれない | インデントがずれている | スペース2個で統一。タブ禁止 |
| アイコンが表示されない | Material 名が間違っている | [Minecraft Material 一覧](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html) を確認 |
| カテゴリが認識されない | 誤ったカテゴリキー | `lq:general` 等のビルトインのみ使用可能 |
| アクションが実行されない | Type / Params の綴りミス | `Command`, `Item`, `MythicItem`, `PvELevel` のいずれか |
