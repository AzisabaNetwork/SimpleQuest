# 完了時アクション (Actions)

クエスト完了時に実行される報酬アクションの設定です。

## 目次

1. [基本構造](#基本構造)
2. [トリガータイミング](#トリガータイミング)
3. [アクション種別](#アクション種別)
4. [プレースホルダー](#プレースホルダー)
5. [具体例](#具体例)

---

## 基本構造

```yaml
Actions:
  OnFirstComplete:        # 初回クリア時のみ
    - Type: Command
      Params: "give % minecraft:diamond 5"
    - Type: MythicItem
      Params: "LegendarySword,1"
  OnComplete:             # 毎回実行
    - Type: Item
      Params: "minecraft:emerald,3"
    - Type: PvELevel
      Params: "50"
    - Type: Command
      Params: "say % completed the quest!"
```

---

## トリガータイミング

| トリガー | 実行タイミング | 用途 |
|---|---|---|
| `OnFirstComplete` | 各プレイヤーが**初めて**そのクエストをクリアしたとき | 初回限定の特別報酬 |
| `OnComplete` | クエストクリアの**たびに毎回** | 通常のクリア報酬 |

```yaml
Actions:
  OnFirstComplete:
    - Type: Command
      Params: "lp user % permission set vip true"  # 初回のみVIP権限
  OnComplete:
    - Type: Item
      Params: "minecraft:diamond,5"                # 毎回ダイヤ5個
```

---

## アクション種別

### Command（コンソールコマンド実行）

サーバーコンソールとしてコマンドを実行します。

| パラメータ | 説明 |
|---|---|
| `Type` | `Command` |
| `Params` | 実行するコマンド文字列 |

```yaml
- Type: Command
  Params: "give % minecraft:diamond 5"
- Type: Command
  Params: "say % がクエストをクリアしました！"
- Type: Command
  Params: "lp user % parent add vip"
```

> **実装状況**: ✅ 動作します

### Item（Minecraft アイテム付与）

指定したアイテムをプレイヤーのインベントリに直接付与します。

| パラメータ | 説明 |
|---|---|
| `Type` | `Item` |
| `Params` | `"material,amount"` 形式 |

```yaml
- Type: Item
  Params: "minecraft:diamond,5"      # ダイヤモンド5個
- Type: Item
  Params: "minecraft:emerald,10"     # エメラルド10個
- Type: Item
  Params: "minecraft:iron_sword,1"   # 鉄の剣1個
```

- `material`: Minecraft Material 名（`DIAMOND` または `minecraft:diamond` 形式、大文字小文字不問）
- `amount`: 付与数（省略時は 1）
- インベントリに入りきらない分は地面にドロップします

> **実装状況**: ✅ 動作します

### MythicItem（MythicMobs アイテム付与）

MythicMobs プラグインのカスタムアイテムを付与します。

| パラメータ | 説明 |
|---|---|
| `Type` | `MythicItem` |
| `Params` | `"ItemName,amount"` 形式 |

```yaml
- Type: MythicItem
  Params: "LegendarySword,1"
- Type: MythicItem
  Params: "DragonScale,5"
```

> **実装状況**: ⚠️ 未実装（`MythicItemGive not implemented` の警告ログが出ます）

### PvELevel（PvE 経験値付与）

PvELevel プラグインの経験値を付与します。

| パラメータ | 説明 |
|---|---|
| `Type` | `PvELevel` |
| `Params` | 経験値量（数値） |

```yaml
- Type: PvELevel
  Params: "100"
- Type: PvELevel
  Params: "500"
```

> **実装状況**: ⚠️ 未実装（`PvELevelExp not implemented` の警告ログが出ます）

---

## プレースホルダー

`Command` アクション内では、以下のプレースホルダーが使用可能です：

| プレースホルダー | 置換内容 |
|---|---|
| `%` | プレイヤー名 |
| `%player%` | プレイヤー名（`%` と同じ） |

```yaml
- Type: Command
  Params: "give % minecraft:diamond 5"
  # → "give Steve minecraft:diamond 5"
```

---

## 具体例

### 初回特別報酬 + 通常報酬

```yaml
Actions:
  OnFirstComplete:
    - Type: Command
      Params: "give % minecraft:netherite_ingot 1"
    - Type: MythicItem
      Params: "DragonSlayerSword,1"
  OnComplete:
    - Type: Item
      Params: "minecraft:diamond,5"
    - Type: PvELevel
      Params: "100"
```

### コマンド連鎖

```yaml
Actions:
  OnComplete:
    - Type: Command
      Params: "give % minecraft:diamond 5"
    - Type: Command
      Params: "say % completed the quest!"
    - Type: Command
      Params: "title % title \"§aClear!\" \"§fQuest Complete\""
```

### アイテム複数付与

```yaml
Actions:
  OnComplete:
    - Type: Item
      Params: "minecraft:diamond,3"
    - Type: Item
      Params: "minecraft:emerald,5"
    - Type: Item
      Params: "minecraft:iron_ingot,10"
```
