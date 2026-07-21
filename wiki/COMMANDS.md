# コマンドと権限

## `/simplequest` — メインコマンド

| サブコマンド | 権限 | 説明 |
|---|---|---|
| _(引数なし)_ | — | プラグインのバージョン情報を表示 |
| `help` | — | コマンド一覧（このヘルプ）を表示 |
| `reload [--use-local\|--use-mysql]` | `simplequest.reload` | 設定・クエスト定義を再読み込み |
| `quest` / `gui` | — | クエスト選択 GUI を開く（プレイヤーのみ） |
| `party` | — | パーティ管理 GUI を開く（プレイヤーのみ） |
| `grant <player> <questType>` | `simplequest.grant` | プレイヤーにクエストを解放 |
| `revoke <player> <questType>` | `simplequest.revoke` | プレイヤーからクエストを剥奪 |
| `progress <player> <reqKey> <formula>` | `simplequest.progress` | クエスト進捗を変更 |

### `/simplequest`（引数なし）

```bash
/simplequest
# → §6SimpleQuest §7v1.0.0
```

### `/simplequest reload`

```bash
# 通常リロード
/simplequest reload

# コンフリクト解決（ローカル YAML 優先）
/simplequest reload --use-local

# コンフリクト解決（MySQL 優先）
/simplequest reload --use-mysql
```

- `--use-local`: コンフリクト時にローカル YAML の内容で MySQL を上書き
- `--use-mysql`: コンフリクト時に MySQL の内容でローカル YAML を上書き
- 両方省略時はコンフリクトを検出して警告のみ

### `/simplequest quest`

```bash
/simplequest quest
```

- クエスト一覧 GUI（54 スロット）を開く
- クエストをクリック → 詳細画面 → Accept でクエスト開始
- 検索ボタン（コンパス）からサイン入力でクエスト名フィルタ

### `/simplequest party`

```bash
/simplequest party
```

- パーティ管理 GUI を開く
- パーティ未所属の場合は作成画面
- 所属済みの場合はメンバー一覧・設定変更・脱退

### `/simplequest grant`

```bash
/simplequest grant <player> <questType>
```

| 引数 | 説明 |
|---|---|
| `player` | プレイヤー名（オンラインまたはオフライン） |
| `questType` | クエストタイプのキー名（例: `BotQuest`） |

```bash
# 例
/simplequest grant Steve BotQuest
/simplequest grant Steve test/TestQuest
```

- 既に解放済みでもエラーにならない（idempotent）
- クエストタイプが見つからない場合はエラー

### `/simplequest revoke`

```bash
/simplequest revoke <player> <questType>
```

- 構文は `grant` と同じ
- プレイヤーからクエスト解放を剥奪

### `/simplequest progress`

```bash
/simplequest progress <player> <reqKey> <formula>
```

| 引数 | 説明 |
|---|---|
| `player` | プレイヤー名（オンライン必須） |
| `reqKey` | Objective キー名（例: `BreakStone`） |
| `formula` | 数式（下記参照） |

**Formula 記法:**

| 演算子 | 意味 | 例 (`base=5`) | 結果 |
|---|---|---|---|
| `+` | 加算 | `+2` | 7 |
| `-` | 減算 | `-3` | 2 |
| `*` | 乗算 | `*5` | 25 |
| `/` | 除算 | `/2` | 2 |
| `=` | 代入 | `=10` / `10` | 10 |

```bash
# 例
/simplequest progress Steve BreakStone +2
/simplequest progress Steve BreakStone "=5"
/simplequest progress Steve KillZombie "*3"
```

- 演算子を省略した場合、`=`（代入）として扱われる
- `*` を含む場合はダブルクォートで囲むこと（`"*2"`）
- 全 Objective が達成されるとクエストが自動完了する

---

## `/party` — パーティコマンド

| サブコマンド | 権限 | 説明 |
|---|---|---|
| _(引数なし)_ | — | ヘルプメッセージを表示 |
| `invite <player>` | — | 指定プレイヤーをパーティに招待 |
| `accept <id>` | — | 指定 ID の招待を承諾 |
| `kick <player>` | リーダー | パーティからメンバーを追放 |

### `/party invite`

```bash
/party invite <player>
```

- パーティ未所属の場合は自動的にパーティが作成される
- 招待されたプレイヤーに `/party accept <id>` の案内が表示される
- 自分自身は招待不可
- 最大メンバー数に達している場合はエラー

### `/party accept`

```bash
/party accept <id>
```

- ID は招待時に表示される UUID
- 自分宛ての招待のみ承諾可能
- 招待の有効期限は `config.yml` の `partyInviteLimit`（デフォルト 1200 tick / 60 秒）

### `/party kick`

```bash
/party kick <player>
```

- パーティリーダーのみ実行可能
- 自分自身やリーダーはキック不可

---

## 権限一覧

| 権限ノード | 既定 | 説明 |
|---|---|---|
| `simplequest.reload` | op | `/simplequest reload` |
| `simplequest.grant` | op | `/simplequest grant` |
| `simplequest.revoke` | op | `/simplequest revoke` |
| `simplequest.progress` | op | `/simplequest progress` |
| `simplequest.stage` | op | ステージ管理（将来実装） |
| `simplequest.debug` | op | デバッグ GUI（将来実装） |

> [!NOTE]
> 一般コマンド（`/simplequest quest`, `/party invite` 等）に権限は不要です。
> 全プレイヤーがデフォルトで使用できます。
