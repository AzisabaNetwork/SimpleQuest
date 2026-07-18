# config.yml 設定リファレンス

`plugins/SimpleQuest/config.yml` の全設定項目の解説です。

初回起動時に自動生成されるデフォルト値は、シングルサーバー運用を想定しています。
マルチサーバー運用の場合は `multi-server` セクションを適宜変更してください。

---

## 全設定項目一覧

```yaml
# ── データベース接続設定 ──
database:
  host: "localhost"        # MariaDB / MySQL のホスト名 or IP
  port: 3306               # ポート番号
  name: "simplequest"        # データベース名
  user: "root"             # 接続ユーザー名
  password: ""             # 接続パスワード

# ── Redis 接続設定 ──
redis:
  host: "localhost"        # Redis のホスト名 or IP
  port: 6379               # ポート番号
  password: ""             # 認証パスワード（requirepass 設定時）

# ── パーティ設定 ──
maxPartySize: 8            # 1パーティの最大人数
partyInviteLimit: 1200     # パーティ招待の有効期限（秒、デフォルト 20 分）

# ── GUI パネル設定 ──
panel:
  title: "&dSimpleQuest"     # クエスト選択 GUI のタイトル
  footer: "&7azisaba.net"  # GUI 下部のフッターテキスト

# ── マルチサーバー同期設定 ──
multi-server:
  write-to-mysql: false    # ローカル YAML → MySQL への書き込みを有効にする
  write-to-yaml: false     # MySQL → ローカル YAML への書き戻しを有効にする
  conflict-mode: "LOCAL"   # コンフリクト時のデフォルト解決方向: LOCAL / MYSQL
  backup:
    enabled: false         # 定期バックアップの有効 / 無効
    interval-hours: 24     # バックアップ間隔（時間）
    retention-days: 30     # バックアップ保持日数
    directory: "plugins/SimpleQuest/backups/"  # バックアップ格納ディレクトリ

# ── Discord 通知設定 ──
discord:
  webhook-url: ""          # Discord Webhook URL（コンフリクト通知用）
```

---

## 各セクションの詳細

### `database`

MariaDB（または MySQL）への接続情報です。

- **host**: `localhost` 以外のリモートホストを指定する場合、MariaDB 側でリモート接続を許可してください
- **user / password**: 対象データベースへの `ALL PRIVILEGES` を持つユーザーが必要です（テーブル自動作成のため）
- コネクションプール設定:
  - `maximumPoolSize`: 10
  - `minimumIdle`: 2
  - `connectionTimeout`: 5000ms
  - `idleTimeout`: 30000ms
  - `maxLifetime`: 600000ms

> [!NOTE]
> プールサイズ等は現在コードにハードコードされています。必要に応じて `DatabaseManager.kt` を編集してください。

### `redis`

Redis への接続情報です。キャッシュと PubSub メッセージングに使用されます。

- **password**: Redis 側で `requirepass` を設定している場合に指定。未設定なら空文字 `""` で問題ありません

### `maxPartySize` / `partyInviteLimit`

パーティシステムの制限設定です。

- **maxPartySize**: パーティに所属できる最大メンバー数（リーダー含む）
- **partyInviteLimit**: 招待の有効期限（秒）。期限内に `/party accept <id>` で承諾する必要があります。デフォルト 1200（20分）

### `panel`

Kunectron GUI の表示設定です。

- **title / footer**: `&` カラーコードが使用可能です

### `multi-server`

複数サーバー間でのクエスト定義同期を制御します。詳細は [docs/MULTI_SERVER.md](../docs/MULTI_SERVER.md) を参照。

#### サーバーの役割と推奨設定

| 役割 | write-to-mysql | write-to-yaml | 説明 |
|---|---|---|---|
| **マスター**（編集サーバー） | `true` | `false` | YAML 編集 → MySQL 反映を担当 |
| **スレーブ**（配信サーバー） | `false` | `true` | MySQL → ローカル YAML 反映を担当 |
| **リードオンリー**（参照のみ） | `false` | `false` | クエスト定義の編集不可、進捗表示のみ |

> [!WARNING]
> `write-to-mysql: true` のサーバーを **複数立てないでください**。競合の原因になります。

#### `conflict-mode`

コンフリクト発生時のデフォルト解決方向です。

- `"LOCAL"`: ローカル YAML の内容を優先
- `"MYSQL"`: MySQL の内容を優先

コンフリクトが発生した場合、`/simplequest reload --use-local` または `--use-mysql` で明示的に解決できます。

#### `backup`

`write-to-mysql: true` のサーバーでのみ動作する定期バックアップ設定です。

- バックアップファイル: `backups/yyyy-MM-dd-HH.tar.gz`
- 内容: `plugins/SimpleQuest/@namespace/` 以下の全 YAML + config.yml
- `retention-days` を超えたファイルは起動時 / 定期タスクで自動削除されます

### `discord`

コンフリクト発生時の通知先 Discord Webhook URL です。

- 空文字 `""` の場合は通知されません
- Webhook URL の取得方法: Discord サーバー設定 → 連携サービス → Webhook → 新規作成

---

## 設定変更の反映

```bash
# サーバー内でコマンド実行
/simplequest reload

# コンフリクトがある場合の強制反映
/simplequest reload --use-local    # ローカル YAML 優先
/simplequest reload --use-mysql    # MySQL 優先
```

> [!IMPORTANT]
> `config.yml` の変更は `/simplequest reload` で反映されます。サーバー再起動は不要です。
