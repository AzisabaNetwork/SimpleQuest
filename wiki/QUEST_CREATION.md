# クエスト定義の作成

SimpleQuest のクエストは **YAML ファイル** で定義します。MythicMobs ライクな記法を採用しており、直感的に記述できます。

## 目次

1. [ファイルの配置](#ファイルの配置)
2. [YAML 基本構造](#yaml-基本構造)
3. [各セクションの詳細](#各セクションの詳細)
4. [具体例](#具体例)
5. [Tips](#tips)

---

## ファイルの配置

```
plugins/SimpleQuest/
└── @namespace/         ← 名前空間（@ 必須）
    └── types/          ← ここに .yml ファイルを配置
        ├── wolf_slayer.yml
        └── supply_procurement.yml
```

- ファイル拡張子は **`.yml`** または **`.yaml`**
- 文字コードは **UTF-8**（BOM なし）
- 1 ファイルに複数のクエスト定義を含めることが可能
- ディレクトリ名は必ず `@` で始めてください（`@lq`、`@custom` など）

---

## YAML 基本構造

```yaml
QuestName:
  Title: "&aクエストタイトル"           # 必須
  Description:                          # 必須
    - "&7説明文1行目"
    - "&7説明文2行目"
  Icon: "MATERIAL"                      # 必須、または "MATERIAL:CMD"
  Aura: true                            # 任意（エンチャント光沢）
  Giver: "&e依頼人"                     # 任意
  Category: "lq:general"                # 必須
  Location: "world,x,y,z"               # 必須

  Options:                              # 任意
    MaxParty: "1-4"                     # "min-max" または "max"
    Limits:
      Daily: 3
      Weekly: 10
      Monthly: 30
      Yearly: 365
      Lifetime: 100
    DeathLimit: 3

  Requirements:                         # 任意（受注条件）
    PvELevel: 10
    Money: 100.0
    PartyMode: true

  Objectives:                           # 必須（達成条件）
    BreakStone: 10
    KillZombie: 5

  Actions:                              # 任意（完了時報酬）
    OnFirstComplete:
      - Type: Command
        Params: "give % minecraft:diamond"
    OnComplete:
      - Type: Item
        Params: "minecraft:diamond,5"
      - Type: MythicItem
        Params: "MMOre,3"
      - Type: PvELevel
        Params: "100"

  Guides:                               # 任意（ナビゲーション）
    - Title: "&a目的地"
      Location: "x,y,z"
      Condition: "req=1"

  Scripts:                              # 任意（イベントスクリプト）
    OnStart: ["say start"]
    OnComplete: ["say done"]
    OnCancel: ["say cancelled"]

  Unlock:                               # 任意（自動解放条件）
    - EnterArea: "world,100,64,200,10"
```

---

## 各セクションの詳細

### Title（必須）

クエストの表示名。`&` カラーコード使用可。

```yaml
Title: "&c&l緊急クエスト"
```

### Description（必須）

クエストの説明文。リスト形式。

```yaml
Description:
  - "&7森に潜む魔物を討伐せよ"
  - "&7残り: 5 体"
```

### Icon（必須）

クエスト選択 GUI に表示するアイコン。

```yaml
# 基本
Icon: "IRON_SWORD"

# カスタムモデルデータ付き
Icon: "IRON_SWORD:5001"

# プレイヤーヘッド
Icon: "PLAYER_HEAD:base64string"
```

### Aura（任意）

`true` でエンチャント光沢エフェクト。デフォルトは `false`。

```yaml
Aura: true
```

### Giver（任意）

クエストの依頼人名。

```yaml
Giver: "&e村長"
```

### Category（必須）

ビルトインカテゴリ:

| キー | 表示名 | 説明 |
|---|---|---|
| `lq:general` | General | 一般クエスト |
| `lq:daily` | Daily | デイリークエスト |
| `lq:story` | Story | ストーリークエスト |
| `lq:event` | Event | イベントクエスト |

```yaml
Category: "lq:general"
```

### Location（必須）

クエストの開始地点。`world,x,y,z` 形式。

```yaml
Location: "world,100,64,200"
```

### Options（任意）

#### MaxParty

```yaml
Options:
  MaxParty: "1-4"    # 1〜4人（ソロ可）
  MaxParty: "4"      # 4人固定
```

#### Limits

受注・完了可能回数の周期制限。

| 周期 | リセット |
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

#### DeathLimit

許容死亡回数。超過でクエスト失敗。

```yaml
Options:
  DeathLimit: 3
```

### Requirements（任意）

クエスト受注に必要な条件。

```yaml
Requirements:
  PvELevel: 10       # PvE レベル
  Money: 100.0        # 所持金
  PartyMode: true     # パーティ必須
```

### Objectives（必須）

クエスト完了に必要な達成条件。Objective Key の命名規則に従うことで、プレイヤーの行動に応じて**自動的に進捗が更新**されます。

> [!IMPORTANT]
> 命名規則と全イベント一覧は **[Objectives とイベントトリガー](quest/EVENTS_AND_OBJECTIVES.md)** を参照してください。

```yaml
Objectives:
  BreakStone: 10       # 石を10個破壊
  KillZombie: 5        # ゾンビを5体討伐
  CollectDiamond: 3    # ダイヤモンドを3個拾う
  CraftStick: 10       # 棒を10個作成
  ConsumeApple: 5      # リンゴを5個食べる
```

| プレフィックス | 意味 | 例 |
|---|---|---|
| `Break` | ブロック破壊 | `BreakStone` |
| `Place` | ブロック設置 | `PlaceDirt` |
| `Kill` | エンティティ討伐 | `KillZombie` |
| `Collect` | アイテム収集 | `CollectDiamond` |
| `Craft` | アイテム作成 | `CraftStick` |
| `Consume` | 飲食 | `ConsumeApple` |
| `Fish` | 釣り | `FishCod` |
| `Enchant` | エンチャント | `EnchantSword` |
| `Smelt` | 精錬 | `SmeltIron` |
| `Breed` | 繁殖 | `BreedCow` |
| `Shear` | 毛刈り | `ShearSheep` |

### Actions（任意）

クエスト完了時の報酬。詳細は **[完了時アクション](quest/ACTIONS.md)** を参照。

```yaml
Actions:
  OnFirstComplete:                  # 初回クリア時のみ
    - Type: Command
      Params: "give % minecraft:diamond 5"
  OnComplete:                       # 毎回
    - Type: Item
      Params: "minecraft:emerald,3"
```

| Type | 説明 | Params | 実装 |
|---|---|---|---|
| `Command` | コンソールコマンド | `"コマンド文字列"` | ✅ |
| `Item` | アイテム付与 | `"material,amount"` | ✅ |
| `MythicItem` | MythicMobs アイテム | `"ItemName,amount"` | ⚠️ 未実装 |
| `PvELevel` | PvE 経験値 | `"amount"` | ⚠️ 未実装 |

プレースホルダー: コマンド内の `%` はプレイヤー名に置換されます。

### Guides（任意）

ナビゲーションガイド。上から順に評価され、最初に条件を満たしたガイドが表示されます。

```yaml
Guides:
  - Title: "&a森の入り口"
    Location: "80,64,180"
    Condition: "req=1"       # 条件付き（optional）
  - Title: "&cボス部屋"
    Location: "150,64,200"
```

### Scripts（任意）

クエストイベント時のスクリプト実行。

```yaml
Scripts:
  OnStart: ["say クエスト開始！"]
  OnStart+20: ["say 1秒後に実行"]    # +tick で遅延
  OnComplete: ["say クリア！"]
  OnCancel: ["say 失敗..."]
```

### Unlock（任意）

クエストの自動解放条件。詳細は **[クエスト解放条件](quest/UNLOCK.md)** を参照。

```yaml
Unlock:
  - EnterArea: "world,100,64,200,10"    # 半径10ブロック進入で解放
```

> ⚠️ `Unlock` は YAML パースのみ対応。実際の解放処理は未実装です。

---

## 具体例

### 討伐クエスト

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

### 採掘クエスト

```yaml
Stonemason:
  Title: "&7石工の修行"
  Description:
    - "&7石を採掘し、加工せよ"
  Icon: "STONE_PICKAXE"
  Category: "lq:general"
  Location: "world,0,64,0"
  Objectives:
    BreakStone: 30
    BreakCobblestone: 50
    CraftStonePickaxe: 3
  Actions:
    OnComplete:
      - Type: Item
        Params: "minecraft:iron_pickaxe,1"
```

### 飲食クエスト

```yaml
HungryChef:
  Title: "&6腹ペコ料理人"
  Description:
    - "&7美味しいものをたくさん食べよう！"
  Icon: "APPLE"
  Category: "lq:daily"
  Location: "world,0,64,0"
  Options:
    Limits:
      Daily: 1
  Objectives:
    ConsumeApple: 5
    ConsumeBread: 5
    ConsumeCookedBeef: 3
  Actions:
    OnComplete:
      - Type: Item
        Params: "minecraft:golden_apple,1"
```

### 初回特別報酬付き

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
        Params: "give % minecraft:netherite_sword 1"
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

```bash
mkdir -p plugins/SimpleQuest/@custom/types
# → @custom/types/ 以下に .yml を配置
```

### リロードで反映

```bash
/simplequest reload
```

### よくある間違い

| 問題 | 原因 | 対策 |
|---|---|---|
| 読み込まれない | ディレクトリ名に `@` がない | `@namespace` にリネーム |
| 同上 | インデントがずれている | スペース2個で統一。タブ禁止 |
| アイコン表示されない | Material 名が間違い | [Paper Javadoc](https://jd.papermc.io/paper/1.21/org/bukkit/Material.html) で確認 |
| 進捗が更新されない | Objective Key の命名規則違反 | `BreakStone` のように正しいプレフィックスを使う |
| アクションが実行されない | Type/Params の綴りミス | `Command`/`Item`/`MythicItem`/`PvELevel` のいずれか |
