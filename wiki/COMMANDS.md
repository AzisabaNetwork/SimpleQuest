# コマンドと権限

## `/lifequest` — メインコマンド

| コマンド | 権限 | 説明 |
|---|---|---|
| `/lifequest` | — | プラグインのバージョン情報を表示 |
| `/lifequest reload` | `lifequest.reload` | 設定・クエスト定義を再読み込み |
| `/lifequest reload --use-local` | `lifequest.reload` | コンフリクトをローカル YAML 優先で解決 + リロード |
| `/lifequest reload --use-mysql` | `lifequest.reload` | コンフリクトを MySQL 優先で解決 + リロード |
| `/lifequest quest` | — | クエスト選択 GUI を開く（プレイヤーのみ） |
| `/lifequest gui` | — | 同上（エイリアス） |
| `/lifequest party` | — | パーティ管理 GUI を開く（プレイヤーのみ） |

### 使用例

```bash
# バージョン確認
/lifequest

# クエスト定義を再読み込み
/lifequest reload

# コンフリクト解決（ローカル優先）
/lifequest reload --use-local
```

---

## `/party` — パーティコマンド

| コマンド | 権限 | 説明 |
|---|---|---|
| `/party` | — | ヘルプメッセージを表示 |
| `/party invite <player>` | — | 指定プレイヤーをパーティに招待 |
| `/party accept <id>` | — | 指定 ID の招待を承諾 |
| `/party kick <player>` | リーダー | パーティからメンバーを追放 |

### 使用例

```bash
# プレイヤーを招待
/party invite Steve

# 招待を承諾（ID は招待時に表示される）
/party accept abc123

# メンバーをキック（パーティリーダーのみ）
/party kick Alex
```

### パーティの制限

| 項目 | 値 |
|---|---|
| 最大メンバー数 | `config.yml` の `maxPartySize`（デフォルト 8） |
| 招待の有効期限 | `config.yml` の `partyInviteLimit`（デフォルト 1200 秒 / 20 分） |
| キック権限 | パーティリーダーのみ |

---

## 権限一覧

| 権限ノード | 対象 | 説明 |
|---|---|---|
| `lifequest.reload` | 管理者 | `/lifequest reload` の実行を許可。コンフリクト解決を含む |

> [!NOTE]
> 一般プレイヤー向けのコマンド（`/lifequest quest`, `/party invite` 等）に権限は不要です。
> 全プレイヤーがデフォルトで使用できます。
