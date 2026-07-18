# コマンド一覧

## /simplequest (管理コマンド)

Paper Brigadier を使用して実装。

| サブコマンド | 権限 | 説明 |
|---|---|---|
| `debug` | `simplequest.debug` | GUI デバッグ表示 |
| `grant <targets> <type>` | `simplequest.grant` | プレイヤーに QuestType を解放 |
| `progress <target> <requirement> <formula>` | `simplequest.progress` | クエスト進捗を変更 |
| `reload` | `simplequest.reload` | 設定・名前空間を再読み込み (DB 再接続含む) |
| `revoke <targets> <type>` | `simplequest.revoke` | プレイヤーから QuestType を剥奪 |
| `stage mount <targets> <stage>` | `simplequest.stage` | パーティをステージにマウント |
| `stage unmount <targets> <stage>` | `simplequest.stage` | パーティをステージからアンマウント |

### grant / revoke

```shell
/simplequest grant @p lq:example
/simplequest revoke @p lq:example
```

- `targets` は Player セレクター
- 既に解放済みのプレイヤーはスキップ
- 変更があったプレイヤーのみメッセージ表示

### progress

```shell
/simplequest progress @p requirement1 +2
/simplequest progress @p requirement1 "=10"
/simplequest progress @p requirement1 "*3"
```

**FormulaArgumentType — 数式パーサ**

| 演算子 | 意味 | 例 (`base=5`) | 結果 |
|---|---|---|---|
| `+` | 加算 | `+2` | 7 |
| `-` | 減算 | `-3` | 2 |
| `*` | 乗算 | `*5` | 25 |
| `/` | 除算 | `/2` | 2 |
| `=` | (デフォルト) 代入 | `=10` / `10` | 10 |

- 演算子を省略した場合、`=` (代入) として扱われる
- ダブルクォートで囲むことが推奨 (`"*2"`)

### stage mount / unmount

```shell
/simplequest stage mount @p namespace:boss_arena
/simplequest stage unmount @p namespace:boss_arena
```

- マウント: パーティがキューに追加される (空きがあれば即時マウント)
- アンマウント: パーティがステージから離脱、キュー先頭が自動マウント

## /party (プレイヤーコマンド)

変更なし。

| サブコマンド | 権限 | 説明 |
|---|---|---|
| `invite <target>` | 全プレイヤー (招待権限必要) | プレイヤーを招待 |
| `accept <uuid>` | 全プレイヤー | 招待を承諾 |
| `kick <target>` | リーダーのみ | メンバーをキック |

### invite

```shell
/party invite <player>
```

- パーティ未所属・招待権限がない場合はエラー
- 既に招待済みのプレイヤーはスキップ

### accept

```shell
/party accept <uuid>
```

- UUID は招待時に表示された ClickEvent から自動入力される
- 招待がない、または自分宛てでない場合はエラー

### kick

```shell
/party kick <player>
```

- リーダーのみ実行可能
- 自分自身やリーダーをキックすることは不可

## 権限一覧

| 権限 | デフォルト | 用途 |
|---|---|---|
| `simplequest.debug` | op | デバッグ GUI |
| `simplequest.grant` | op | QuestType 解放 |
| `simplequest.progress` | op | 進捗変更 |
| `simplequest.reload` | op | リロード |
| `simplequest.revoke` | op | QuestType 剥奪 |
| `simplequest.stage` | op | ステージ管理 |
