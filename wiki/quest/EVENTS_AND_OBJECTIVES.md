# クエスト Objectives とイベントトリガー

SimpleQuest の Objectives（達成条件）は **プレイヤーの実際の行動に応じて自動的に進捗が更新** されます。

## 目次

1. [基本概念](#基本概念)
2. [Objective Key 命名規則](#objective-key-命名規則)
3. [対応イベント一覧](#対応イベント一覧)
4. [記法](#記法)
5. [具体例](#具体例)
6. [トラブルシューティング](#トラブルシューティング)

---

## 基本概念

YAML の `Objectives` セクションに定義されたキーは、**プレイヤーの行動と自動的に対応付け**られます。

```yaml
Objectives:
  BreakStone: 5        # プレイヤーが石を5個破壊すると達成
  KillZombie: 10       # ゾンビを10体倒すと達成
```

- キー名がイベントの種類と対象をエンコードします
- プレイヤーが対応する行動を行うたびに進捗が +1 されます
- すべての Objectives が達成されると、クエストは自動的に完了します

---

## Objective Key 命名規則

Objective Key は以下の命名規則に従います：

```
{ActionPrefix}{TargetName}
```

| 要素 | 説明 |
|---|---|
| `ActionPrefix` | イベントの種類を示すプレフィックス（下表参照） |
| `TargetName` | 対象の Bukkit Material 名または EntityType 名（大文字小文字不問） |

### ActionPrefix 一覧

| Prefix    | トリガーイベント       | Bukkit イベント         | 例の Key        | 意味                              |
|-----------|----------------------|------------------------|-----------------|-----------------------------------|
| `Break`   | ブロック破壊          | `BlockBreakEvent`      | `BreakStone`    | 石ブロックを破壊                   |
| `Place`   | ブロック設置          | `BlockPlaceEvent`      | `PlaceDirt`     | 土ブロックを設置                   |
| `Kill`    | エンティティ討伐      | `EntityDeathEvent`     | `KillZombie`    | ゾンビを倒す                       |
| `Collect` | アイテム収集          | `EntityPickupItemEvent`| `CollectDiamond`| ダイヤモンドを拾う                  |
| `Craft`   | アイテム作成          | `CraftItemEvent`       | `CraftStick`    | 棒を作成する                       |
| `Consume` | アイテム飲食          | `PlayerItemConsumeEvent`| `ConsumeApple` | リンゴを食べる                     |
| `Fish`    | 魚釣り               | `PlayerFishEvent`      | `FishCod`       | タラを釣る (EntityType)            |
| `Enchant` | エンチャント          | `EnchantItemEvent`     | `EnchantSword`  | 鉄の剣にエンチャント               |
| `Smelt`   | アイテム精錬          | `FurnaceExtractEvent`  | `SmeltIron`     | 鉄インゴットを精錬                 |
| `Breed`   | 動物繁殖             | `EntityBreedEvent`     | `BreedCow`      | 牛を繁殖させる                     |
| `Shear`   | 羊の毛刈り            | `PlayerShearEntityEvent`| `ShearSheep`   | 羊の毛を刈る                       |

### TargetName の指定方法

TargetName には **Bukkit Material または EntityType の enum 定数名** を指定します（大文字小文字不問、アンダースコア区切り）。

| 指定したい対象 | TargetName 表記 | 補足 |
|---|---|---|
| 石 (Stone) | `Stone` | Material.STONE |
| 樫の原木 (Oak Log) | `OakLog` | Material.OAK_LOG → `OAK_LOG` を PascalCase で |
| 土 (Dirt) | `Dirt` | Material.DIRT |
| ゾンビ (Zombie) | `Zombie` | EntityType.ZOMBIE |
| スケルトン (Skeleton) | `Skeleton` | EntityType.SKELETON |
| ダイヤモンド (Diamond) | `Diamond` | Material.DIAMOND |
| 鉄インゴット (Iron Ingot) | `IronIngot` | Material.IRON_INGOT → `IronIngot` |

> [!NOTE]
> Material/EntityType の正式な enum 名は [Paper Javadoc](https://jd.papermc.io/paper/1.21/org/bukkit/Material.html) で確認できます。
> `minecraft:stone` や `minecraft:oak_log` ではなく、`STONE`、`OAK_LOG` の enum 名を PascalCase で記述してください。

---

## 対応イベント一覧

### Break（ブロック破壊）

`BlockBreakEvent` にフックし、破壊されたブロックの Material が TargetName と一致するかチェックします。

```yaml
Objectives:
  BreakStone: 10       # 石ブロックを10個破壊
  BreakOakLog: 20      # 樫の丸太を20個破壊
  BreakDiamondOre: 3   # ダイヤモンド鉱石を3個破壊
  BreakCobblestone: 50 # 丸石を50個破壊
```

> [!NOTE]
> プレイヤーが実際にブロックを破壊した場合のみカウントされます（キャンセルされたイベントは無視）。

### Place（ブロック設置）

`BlockPlaceEvent` にフックし、設置されたブロックの Material が TargetName と一致するかチェックします。

```yaml
Objectives:
  PlaceDirt: 30        # 土ブロックを30個設置
  PlaceTorch: 5        # 松明を5個設置
  PlaceCobblestone: 10 # 丸石を10個設置
```

### Kill（エンティティ討伐）

`EntityDeathEvent` にフックし、プレイヤーが倒したエンティティの EntityType が TargetName と一致するかチェックします。

```yaml
Objectives:
  KillZombie: 10       # ゾンビを10体討伐
  KillSkeleton: 5      # スケルトンを5体討伐
  KillSpider: 8        # クモを8体討伐
  KillCreeper: 3       # クリーパーを3体討伐
```

> [!NOTE]
> プレイヤーが直接倒したエンティティのみカウントされます（環境ダメージや他 Mob によるキルは対象外）。

### Collect（アイテム収集）

`EntityPickupItemEvent` にフックし、拾ったアイテムの Material が TargetName と一致するかチェックします。

```yaml
Objectives:
  CollectDiamond: 3    # ダイヤモンドを3個拾う
  CollectIronIngot: 10 # 鉄インゴットを10個拾う
  CollectArrow: 20     # 矢を20個拾う
```

> [!NOTE]
> プレイヤーが地面から拾ったアイテムのみカウントされます。インベントリ直接追加やコマンド付与は対象外です。

### Craft（アイテム作成）

`CraftItemEvent` にフックし、作成されたアイテムの Material が TargetName と一致するかチェックします。

```yaml
Objectives:
  CraftStick: 10       # 棒を10個作成
  CraftChest: 3        # チェストを3個作成
  CraftIronSword: 1    # 鉄の剣を1個作成
```

> [!NOTE]
> 作業台でのクラフトのみカウントされます。かまど（精錬）は `Smelt` を使用してください。

### Consume（アイテム飲食）

`PlayerItemConsumeEvent` にフックし、飲食したアイテムの Material が TargetName と一致するかチェックします。

```yaml
Objectives:
  ConsumeApple: 5       # リンゴを5個食べる
  ConsumeBread: 10      # パンを10個食べる
  ConsumePotion: 3      # ポーションを3本飲む
  ConsumeMilkBucket: 1  # 牛乳を飲む (バケツは返却)
  ConsumeGoldenApple: 2 # 金のリンゴを2個食べる
```

> [!NOTE]
> 食べ物・ポーション・牛乳など、飲食アクション全般が対象です。
> 飲み終わった後の空容器（ガラス瓶・バケツ）はカウントされません。

### Fish（魚釣り）

`PlayerFishEvent` にフックし、釣り上げたエンティティの EntityType が TargetName と一致するかチェックします。

```yaml
Objectives:
  FishCod: 5           # タラを5匹釣る
  FishSalmon: 3        # サーモンを3匹釣る
  FishPufferfish: 1    # フグを1匹釣る
```

> [!NOTE]
> `CAUGHT_FISH` 状態（実際に釣り上げ成功）のみカウントされます。
> ゴミや宝 (Item) を釣った場合はカウントされません。EntityType マッチのみです。

### Enchant（エンチャント）

`EnchantItemEvent` にフックし、エンチャントしたアイテムの Material が TargetName と一致するかチェックします。

```yaml
Objectives:
  EnchantSword: 3      # 鉄の剣に3回エンチャント
  EnchantBow: 1        # 弓に1回エンチャント
  EnchantPickaxe: 5    # ツルハシに5回エンチャント
```

> [!NOTE]
> エンチャントテーブルでのエンチャントのみカウントされます（金床は対象外）。
> 1回のエンチャント操作で複数付与されても +1 です。

### Smelt（アイテム精錬）

`FurnaceExtractEvent` にフックし、かまどから取り出したアイテムの Material が TargetName と一致するかチェックします。

```yaml
Objectives:
  SmeltIron: 10        # 鉄インゴットを10個精錬
  SmeltGold: 5         # 金インゴットを5個精錬
  SmeltCookedBeef: 20  # ステーキを20個焼く
```

> [!NOTE]
> かまど・溶鉱炉・燻製器すべての「精錬結果取り出し」がカウントされます。

### Breed（動物繁殖）

`EntityBreedEvent` にフックし、繁殖させた動物の EntityType が TargetName と一致するかチェックします。

```yaml
Objectives:
  BreedCow: 5          # 牛を5回繁殖
  BreedSheep: 3        # 羊を3回繁殖
  BreedChicken: 10     # 鶏を10回繁殖
```

> [!NOTE]
> プレイヤーがエサを与えたことによる繁殖のみカウントされます。

### Shear（毛刈り）

`PlayerShearEntityEvent` にフックし、毛を刈ったエンティティの EntityType が TargetName と一致するかチェックします。

```yaml
Objectives:
  ShearSheep: 10       # 羊の毛を10回刈る
```

---

## 記法

### 基本形（数値のみ）

```yaml
Objectives:
  KillZombie: 10       # → QuestRequirement(key="KillZombie", amount=10)
  BreakStone: 5        # → QuestRequirement(key="BreakStone", amount=5)
```

### 拡張形（material*amount）

従来の `material*amount` 記法にも対応しています。この場合 `*` の左側はメタデータとして扱われますが、Objective Key の命名規則が優先されます。

```yaml
Objectives:
  CollectDiamond: "minecraft:diamond*5"  # → amount=5 (key=CollectDiamond)
```

> [!IMPORTANT]
> 新しい命名規則（`{ActionPrefix}{TargetName}`）の使用を推奨します。
> `material*amount` 記法は後方互換性のために残されていますが、キー名の命名規則が
> イベントトリガーの動作を決定するため、命名規則を守らないキーは自動進捗の対象になりません。

### 複数 Objectives の組み合わせ

複数の Objectives を組み合わせて複合クエストを作成できます。すべての Objective が達成されるとクエストが完了します。

```yaml
Objectives:
  KillZombie: 10
  BreakStone: 20
  CollectIronIngot: 5
  CraftIronSword: 1
```

---

## 具体例

### 例1: 石工クエスト

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
      - Type: PvELevel
        Params: "50"
```

### 例2: 討伐クエスト

```yaml
MonsterHunter:
  Title: "&cモンスターハンター"
  Description:
    - "&7夜のモンスターを討伐せよ"
  Icon: "IRON_SWORD"
  Category: "lq:daily"
  Location: "world,0,64,0"
  Options:
    Limits:
      Daily: 1
  Objectives:
    KillZombie: 5
    KillSkeleton: 5
    KillSpider: 3
  Actions:
    OnComplete:
      - Type: Item
        Params: "minecraft:diamond,3"
```

### 例3: 建築クエスト

```yaml
Builder:
  Title: "&e建築家の挑戦"
  Description:
    - "&7指定されたブロックを設置せよ"
  Icon: "OAK_PLANKS"
  Category: "lq:general"
  Location: "world,0,64,0"
  Objectives:
    PlaceOakPlanks: 64
    PlaceGlassPane: 16
    PlaceTorch: 10
  Actions:
    OnComplete:
      - Type: Command
        Params: "give % minecraft:golden_axe 1"
```

### 例4: 複合クエスト

```yaml
Adventurer:
  Title: "&a冒険者の試練"
  Description:
    - "&7採掘・討伐・収集をこなせ"
  Icon: "DIAMOND_SWORD"
  Aura: true
  Category: "lq:story"
  Location: "world,0,64,0"
  Options:
    DeathLimit: 3
  Objectives:
    BreakIronOre: 10
    KillZombie: 15
    CollectBone: 10
    CraftIronSword: 1
  Actions:
    OnFirstComplete:
      - Type: MythicItem
        Params: "AdventurerCertificate,1"
    OnComplete:
      - Type: PvELevel
        Params: "100"
```

---

## トラブルシューティング

### ブロックを破壊しても進捗が増えない

1. **キー名の確認**: `Objectives` のキーが正しい命名規則に従っているか確認してください。
   - ✅ `BreakStone` / `breakstone` / `break_stone` (内部的に `STONE` としてマッチ)
   - ❌ `break stone` / `Break_Stone` / `break-stone`
2. **Material 名の確認**: TargetName が有効な Material enum 名か確認してください。
   - ✅ `OakLog` (`OAK_LOG`)
   - ❌ `Log` / `oak_log` / `Oak Wood`
3. **イベントのキャンセル**: 他のプラグインが `BlockBreakEvent` をキャンセルしている可能性があります。

### エンティティを倒しても進捗が増えない

1. **プレイヤーが直接倒しているか**: 環境ダメージ（落下、溶岩）や他 Mob によるキルはカウントされません。
2. **キー名の確認**: `KillZombie` のように正しい命名規則に従っているか確認してください。

### アイテムを拾っても進捗が増えない

1. **地面から拾っているか**: コマンドやプラグインでインベントリに直接追加されたアイテムはカウントされません。
2. **Material 名の確認**: `CollectDiamond` のように正しい命名規則か確認してください。

### それでも動かない場合

サーバーログにエラーが出力されていないか確認してください。問題が解決しない場合は `/simplequest progress` コマンドで手動進捗も可能です。

```bash
/simplequest progress <player> <objectiveKey> <formula>
# 例: /simplequest progress @p BreakStone +5
```
