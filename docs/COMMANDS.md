# コマンド一覧

## `/simplequest` (管理コマンド)

Paper Brigadier を使用して実装。

| サブコマンド | 権限 | 説明 |
|---|---|---|
| _(なし)_ | — | バージョン表示 |
| `help` | — | コマンド一覧を表示 |
| `reload [--use-local\|--use-mysql]` | `simplequest.reload` | 設定・名前空間を再読み込み (DB 再接続含む) |
| `quest` / `gui` | — | クエスト選択 GUI |
| `party` | — | パーティ管理 GUI |
| `grant <player> <questType>` | `simplequest.grant` | プレイヤーに QuestType を解放 |
| `revoke <player> <questType>` | `simplequest.revoke` | プレイヤーから QuestType を剥奪 |
| `progress <player> <reqKey> <formula>` | `simplequest.progress` | クエスト進捗を変更 |

### grant / revoke

```shell
/simplequest grant <player> <questType>
/simplequest revoke <player> <questType>
```

- `player`: プレイヤー名（オンライン / オフライン両対応）
- `questType`: クエストキー（例: `BotQuest`, `@test/party_quest`）
- 既に解放済みのプレイヤーはスキップ（idempotent）

### progress

```shell
/simplequest progress <player> <reqKey> <formula>
```

- `player`: プレイヤー名（オンライン必須）
- `reqKey`: Objective キー名（例: `BreakStone`）
- `formula`: 数式（`+2`, `-1`, `*3`, `/2`, `=10`）

**Formula:**

| 演算子 | 例 | 説明 |
|---|---|---|
| `+` | `+2` | 加算 |
| `-` | `-3` | 減算 |
| `*` | `*5` | 乗算 |
| `/` | `/2` | 除算 |
| `=` | `=10` / `10` | 代入（デフォルト） |

## `/party` (プレイヤーコマンド)

| サブコマンド | 権限 | 説明 |
|---|---|---|
| `invite <player>` | 全プレイヤー | プレイヤーを招待 |
| `accept <uuid>` | 全プレイヤー | 招待を承諾 |
| `kick <player>` | リーダーのみ | メンバーをキック |

## 権限一覧

| 権限 | デフォルト | 用途 |
|---|---|---|
| `simplequest.reload` | op | リロード |
| `simplequest.grant` | op | QuestType 解放 |
| `simplequest.revoke` | op | QuestType 剥奪 |
| `simplequest.progress` | op | 進捗変更 |
| `simplequest.stage` | op | ステージ管理（将来実装） |
| `simplequest.debug` | op | デバッグ GUI（将来実装） |
