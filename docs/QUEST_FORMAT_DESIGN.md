# クエスト記法設計案 — MythicMobs ライクフォーマット

> 本ドキュメントは SimpleQuest のクエスト YAML 記法について、MythicMobs (MM) ライクな書式の設計案を提示する。
> 現行フォーマットからの移行を前提とし、拡張性・可読性・記述効率のバランスを検討する。

## MM 記法の特徴（参考）

MythicMobs の YAML スタイルの主な特徴:

```
# --- Mob 定義 ---
SkeletalKnight:                          # ① ID が YAML キー
  Type: WITHER_SKELETON                  # ② 直感的な名前のトップレベルキー
  Display: '&2Skeletal Knight'
  Health: 40
  Damage: 8
  Options:                               # ③ オプションはまとめてネスト
    MovementSpeed: 0.1
  Skills:                                # ④ スキルはリスト + インラインDSL文字列
  - skill{s=SmashAttack} @Target >0 0.2
  - message{m="<mob.name> Attack!"} @PlayersInRadius{r=40} ~onCombat >0 0.2
  Drops:                                 # ⑤ ドロップもシンプルなリスト書式
  - gold_nugget 2 0.5

# --- Skill 定義 ---
SmashAttack:                             # ID が YAML キー
  Cooldown: 8
  Conditions:
  - targetwithin{d=25}
  Skills:
  - damage{amount=5;ignorearmor=true} @PlayersInRadius{r=5}
```

### MM スタイルのメリット

- **ID = YAML キー**: ファイル内検索・参照が自然 (参照時 `SkeletalKnight` と書く)
- **Options ブロック**: 重要度の低い設定をひとまとめにして視認性向上
- **インライン DSL**: 短期間に収まる簡易スキルは一行で書ける (`@Target ~onTimer`)
- **命名センス**: `Type`, `Display`, `Health` など直感的

### MM スタイルのデメリット

- **インライン DSL の学習コスト**: `@Target ~onTimer:200 >0 0.5` の意味を覚える必要がある
- **複雑条件の表現限界**: 複雑なロジックは YAML 構造の方が読みやすい
- **パース実装が複雑**: 独自 DSL パーサーが必要

---

## 設計案一覧

| 案 | スタイル | DSL度 | YAML構造度 | 特徴 |
|---|---|---|---|---|
| **案 A** | MM-DSL 寄り | 強い | 弱い | インライン文字列でアクション/条件を記述 |
| **案 B** | MM-構造寄り | 弱い | 強い | MM命名に則した YAML ブロック、DSL最小限 |
| **案 C** | MM-バランス | 適度 | 適度 | 見やすさと MM らしさの妥協点 |

---

## 案 A: MM-DSL 寄り

MM のスキル DSL に近いインライン文字列形式を積極採用。
クエスト内のアクション・条件・トリガーをコンパクトな文字列で記述する。

```yaml
# @lq/types/forest_exploration.yml
ForestExploration:                         # ID = YAML キー
  Type: QUEST                              # 種別 (拡張時に DUNGEON/CHALLENGE 等)
  Title: "&a森の探索"
  Description:
  - "&7森に潜む魔物を討伐せよ"
  Icon: IRON_SWORD;cmd:5001;aura           # インライン: material;model;flag
  Giver: "&e村長"

  # 受注条件 — DSL: key:value;flag形式
  Requirements:
    PvELevel: 10
    Quests:
    - lq:prologue
    Permissions:
    - "group.vip"
    PartyMode: true
    DeathLimit: 3
    PlayLimits: "weekly:3, lifetime:5"     # DSL: カンマ区切り key:value

  Category: "lq:general"
  Location: "overworld,100,64,200"         # DSL: world,x,y,z[,yaw,pitch]

  MaxPlayers: 4
  MinPlayers: 1

  # 完了条件 — インライン DSL
  Objectives:
  - "kill_zombie:10"                       # key:amount
  - "collect_item:3"

  # アクション — MMスキルライクDSL
  Actions:
    OnFirstComplete:
    - "mythicitem{give=MMOre;amount=5}"
    - "command{cmd='say %player% 初クリア！'}"
    OnComplete:
    - "console{cmd='lp user %player% parent add veteran'}"
    - "pvelevel{exp=50}"

  # ガイド — DSL: title@world,x,y,z[?req1=n]
  Guides:
  - "&a森の入り口@overworld,80,64,180?kill_zombie=1"

  # スクリプト — MMトリガー形式
  Scripts:
    ~onStart:                               # ~onTrigger
    - "console{cmd='say quest started'}"
    ~onStart+20:                            # 遅延
    - "console{cmd='say 1秒経過'}"
    ~onComplete:
    - "mythicitem{give=MMOre;amount=3}"
    ~onCancel:
    - "console{cmd='say %player% failed'}"
```

### 評価

| 項目 | 評価 |
|---|---|
| **MM らしさ** | ◎ スキル DSL の雰囲気を強く継承 |
| **記述効率** | ◎ 一行で多くの情報を詰め込める |
| **可読性** | △ DSL 記法に慣れが必要。複雑な条件は見づらい |
| **学習コスト** | △ パラメータ・区切り文字のルールを覚える必要がある |
| **実装コスト** | △ インラインパーサーが必要。エラーメッセージが複雑化 |
| **型安全性** | △ 文字列パースのため IDE 補完・バリデーションが効かない |

---

## 案 B: MM-構造寄り

MM の「Options ブロックでまとめる」スタイルを継承しつつ、クエスト固有の設定は構造化 YAML で明確に表現する。
インライン DSL は極力使わず、すべて明示的なキーで記述。

```yaml
# @lq/types/forest_exploration.yml
ForestExploration:
  Type: QUEST
  Title: "&a森の探索"
  Description:
  - "&7森に潜む魔物を討伐せよ"
  Giver: "&e村長"

  Icon:
    Material: IRON_SWORD
    CustomModelData: 5001
    Aura: true

  Category: "lq:general"
  Location:
    World: "overworld"
    X: 100
    Y: 64
    Z: 200

  Options:                                 # MM の Options ブロック
    MaxPartySize: 4
    MinPartySize: 1
    PlayLimits:
      Weekly: 3
      Lifetime: 5
    DeathLimit: 3

  Requirements:                            # 受注条件 (AcceptConditions)
    PvELevel: 10
    RequiredQuests:
    - "lq:prologue"
    Permissions:
    - "group.vip"
    PartyMode: true

  Objectives:                              # 完了条件
    KillZombie:
      Amount: 10
      # 将来的に: Type: MOB_KILL, Mob: ZOMBIE
    CollectItem:
      Amount: 3

  Actions:
    OnFirstComplete:
    - Type: MythicItemGive
      Item: MMOre
      Amount: 5
    - Type: Command
      Command: "say %player% 初クリア！"
    OnComplete:
    - Type: PvELevelExp
      Amount: 50
    - Type: Command
      Command: "lp user %player% parent add veteran"

  Guides:
  - Title: "&a森の入り口"
    Location:
      World: "overworld"
      X: 80
      Y: 64
      Z: 180
    Conditions:
      KillZombie: 1

  Scripts:
    OnStart:                               # Trigger 名をキーに
    - "console: say Quest started"
    OnStart+20:
    - "console: say 1秒経過"
    OnComplete:
    - "give %player% minecraft:diamond 1"
    OnCancel:
    - "say %player% has failed the quest"
```

### 評価

| 項目 | 評価 |
|---|---|
| **MM らしさ** | ○ Options ブロック、命名規則、構造の雰囲気は MM 風 |
| **記述効率** | △ やや verbose。ただし明確 |
| **可読性** | ◎ 何の設定か一目でわかる |
| **学習コスト** | ○ YAML が読めれば理解できる。DSL 不要 |
| **実装コスト** | ◎ 素直な YAML デシリアライズ。kaml でそのまま data class に |
| **型安全性** | ◎ kotlinx.serialization で完全に型付け可能 |

---

## 案 C: MM-バランス

案 A（DSL 記法）と案 B（構造化 YAML）の中間を取る。
短く書けるところは簡潔に、複雑なところは構造化する。

```yaml
# @lq/types/forest_exploration.yml
ForestExploration:
  Title: "&a森の探索"
  Description:
  - "&7森に潜む魔物を討伐せよ"
  Icon: "IRON_SWORD:5001"                  # 簡略: "Material:CMD"
  Aura: true
  Giver: "&e村長"
  Category: "lq:general"

  Location: "overworld,100,64,200"

  Options:
    MaxParty: 1-4                           # "min-max" 形式
    Limits:
      Weekly: 3
      Lifetime: 5
    DeathLimit: 3

  Requirements:
    PvELevel: 10
    RequiredQuests:
    - "lq:prologue"
    Permissions:
    - "group.vip"
    PartyMode: true

  Objectives:                              # Map<String, Int> の簡略形
    KillZombie: 10
    CollectItem: 3

  Actions:                                 # Type: params のハイブリッド
    OnFirstComplete:
    - MythicItem: MMOre,5
    - Command: "say %player% first clear!"
    OnComplete:
    - Command: "lp user %player% parent add veteran"
    - PvELevel: 50
    - Item: "minecraft:diamond,3"

  Guides:
  - "&a森の入り口@80,64,180"              # 座標のみ簡略 (worldはlocationから継承)
    Condition: KillZombie=1

  Scripts:
    OnStart:
    - "say Quest started"
    OnStart+20:
    - "say 1秒経過"
    OnComplete:
    - "give %player% minecraft:diamond 1"
    OnCancel:
    - "say %player% failed the quest"
```

### 評価

| 項目 | 評価 |
|---|---|
| **MM らしさ** | ◎ 過不足ない MM っぽさ |
| **記述効率** | ○ 短いものは簡略、複雑なものは構造化 |
| **可読性** | ○ インラインと構造化の境界が明確 |
| **学習コスト** | ○ 簡略形のルールは少数 (Type:Params と Material:CMD 程度) |
| **実装コスト** | ○ 部分的にパーサーが必要だが、全体は kaml data class でOK |
| **型安全性** | ○ 基本は data class、一部カスタムパーサーで対応 |

---

## 比較表: 同じ設定を3案で書き比べ

| 設定項目 | 案 A (DSL) | 案 B (構造) | 案 C (バランス) |
|---|---|---|---|
| **アイコン** | `IRON_SWORD;cmd:5001;aura` | `Material: IRON_SWORD`<br>`CustomModelData: 5001`<br>`Aura: true` | `"IRON_SWORD:5001"` + `Aura: true` |
| **アクション** | `mythicitem{give=MMOre;amount=5}` | `Type: MythicItemGive`<br>`Item: MMOre`<br>`Amount: 5` | `MythicItem: MMOre,5` |
| **完了条件** | `kill_zombie:10` | `KillZombie:`<br>`Amount: 10` | `KillZombie: 10` |
| **受注条件** | `PvELevel: 10` | `PvELevel: 10` | `PvELevel: 10` |
| **ガイド** | `"title@world,x,y,z?req=n"` | `Title: ...`<br>`Location: ...`<br>`Conditions: ...` | `"title@x,y,z"` + `Condition: ...` |

---

## 記法要素ごとの選択肢一覧

### 1. ID 位置

| 方式 | 例 | 採用案 |
|---|---|---|
| **YAML キー** | `ForestExploration:` → ファイル名と一致推奨 | A/B/C 全案 |
| **内部フィールド** | `Id: "lq:forest_exploration"` | 旧来方式 |

### 2. アイコン記法

| 方式 | 例 | 採用案 |
|---|---|---|
| **インライン文字列** | `IRON_SWORD;cmd:5001;aura` | 案 A |
| **構造化** | `Material: IRON_SWORD` + `CustomModelData: 5001` | 案 B |
| **ハイブリッド** | `"IRON_SWORD:5001"` + `Aura: true` | 案 C |

### 3. アクション記法

| 方式 | 例 | 採用案 |
|---|---|---|
| **MM スキル DSL** | `mythicitem{give=MMOre;amount=5}` | 案 A |
| **構造化 YAML** | `Type: MythicItemGive`, `Item: MMOre`... | 案 B |
| **簡略 Type:Params** | `MythicItem: MMOre,5` | 案 C |

### 4. 完了条件 (Objectives)

| 方式 | 例 | 採用案 |
|---|---|---|
| **Map<String, Int>** | `KillZombie: 10` | 案 C (シンプル) |
| **構造化 (拡張準備)** | `KillZombie: { Amount: 10, Type: MOB_KILL }` | 案 B (将来拡張) |
| **key:value 文字列** | `"kill_zombie:10"` | 案 A (統一性) |

### 5. Options ブロック

| 方式 | 例 | 採用案 |
|---|---|---|
| **MM 式 Options** | `Options: { PlayLimits: ..., DeathLimit: ... }` | 案 B, C |
| **トップレベル直置き** | `PlayLimits: ...` `DeathLimit: ...` | 案 A (旧来互換) |

### 6. 受注条件名

| 方式 | 例 | 採用案 |
|---|---|---|
| **旧来方式** | `requirements:` (objectives と混同しやすい) | 現行 |
| **MM 命名** | `Requirements:` | A/B/C (明確化) |
| **AcceptConditions** | `AcceptConditions:` | 構造案 |

---

## 推奨パターン: 案 C (MM-バランス) ベース + α

理由:

1. **記述効率と可読性のバランスが最良**: Objectives のように `Map<String, Int>` で済むものは簡潔に、Actions のように複雑なものは Type で明確化
2. **実装コストが妥当**: 全面的な DSL パーサー不要。kaml のカスタムシリアライザー数個で対応可能
3. **学習コストが低い**: `"IRON_SWORD:5001"` のような簡略形は規則が単純
4. **MM ユーザーの移行が容易**: Options ブロック・命名センス・Type によるアクション指定は MM ユーザーに馴染み深い
5. **拡張性**: Objectives を後から構造化 (`Type: MOB_KILL` の追加) に移行できる

### 今後の検討課題

- [ ] Actions の Type 一覧を固める (item_give / mythic_item_give / pvelevel_exp / command / ...)
- [ ] Objectives の拡張仕様 (mob 種類指定・場所指定・アイテム指定など)
- [ ] Scripts と Actions の住み分け定義 (両方あると混乱しないか)
- [ ] Guide の Location 継承ルール (world 省略時は Quest の Location から継承)
- [ ] パースエラー時のフォールバック・バリデーション方針
- [ ] `@namespace/types/` のファイル名 = ID とするか、別名を許容するか

---
