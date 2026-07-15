# マルチサーバー同期仕様

## 概要

LifeQuest を複数サーバーで運用する際、クエスト定義 (YAML) とプレイヤーデータを MySQL で一元管理する。
サーバーごとに「YAML 書き出し権限」「YAML 読み込み権限」を設定し、コンフリクト検出・解決機構を備える。

## 設定

```yaml
# config.yml
multi-server:
  write-to-mysql: true     # YAML → MySQL への反映を担当 (SSOT の起点)
  write-to-yaml: true      # MySQL → ローカル YAML への書き戻しを担当
  conflict-mode: LOCAL       # LOCAL / MYSQL (コンフリクト時のデフォルト解決方向)
  backup:
    enabled: true
    interval-hours: 24
    retention-days: 30
    directory: "plugins/LifeQuest/backups/"
discord:
  webhook-url: "https://discord.com/api/webhooks/..."

# ...existing config...
```

## プレイヤーデータ

進捗・完了履歴・解放管理などのプレイヤーデータは常に MySQL で管理。
すべてのサーバーが同一 DB を参照する。Redis はキャッシュ / PubSub 用。
ライト系サーバーでも進捗の参照は可能だが、クエスト定義の編集は不可。

## 同期フロー

### 1. 起動時 / リロード時の基本フロー

```
[write-to-mysql = true のサーバー]

  1. ローカル YAML を全スキャン
  2. 各クエスト定義に QuestVersion (更新日時 + checksum) を付与
  3. MySQL quest_definitions テーブルに UPSERT
     - 既存レコードよりローカルの更新日時が新しい → 上書き
     - 既存レコードより MySQL の更新日時が新しい → コンフリクト
  4. コンフリクトがなければ MySQL 上のデータが SSOT
```

```
[write-to-yaml = true のサーバー]

  1. MySQL quest_definitions から全レコード取得
  2. 各クエスト定義の更新日時をローカル YAML と比較
  3. MySQL の更新日時が新しい → ローカル YAML を上書き
  4. ローカルの更新日時が新しい → コンフリクト
```

```
[両方 false のサーバー]

  - クエスト定義の編集は一切行わない
  - MySQL 上のクエスト定義を読み込んでレジストリに登録
  - プレイヤー進捗の表示・更新のみ可能
```

### 2. コンフリクト検出

MySQL `quest_definitions` テーブル:

```sql
CREATE TABLE quest_definitions (
    quest_key   VARCHAR(255) PRIMARY KEY,
    yaml_text   MEDIUMTEXT NOT NULL,
    checksum    VARCHAR(64) NOT NULL,          -- SHA-256
    updated_at  TIMESTAMP(3) NOT NULL,
    updated_by  VARCHAR(36) NOT NULL,          -- サーバーID (UUID)
    conflict    BOOLEAN NOT NULL DEFAULT FALSE
);
```

コンフリクト条件:

- `(local.checksum != mysql.checksum) AND (local.updated_at < mysql.updated_at)` → MySQL が新しい → MySQL を信頼 (write-to-yaml があれば YAML 更新)
- `(local.checksum != mysql.checksum) AND (local.updated_at > mysql.updated_at)` → ローカルが新しい → コンフリクト

### 3. コンフリクト解決

コンフリクト発生時:

1. `conflict = TRUE` を MySQL にセット
2. 該当クエストのレジストリ登録をスキップ
3. リロード権限 (`lifequest.reload`) を持つプレイヤーが参加した際にエラー表示:
   - `§c[LifeQuest] Quest conflict detected: @namespace/quest_name`
   - `§cUse /lifequest reload --use-local or --use-mysql to resolve`
4. Discord Webhook にエラー通知

コンフリクト解決コマンド:

```shell
# ローカルを強制適用 → MySQL へ書き込み
/lifequest reload --use-local

# MySQL を強制適用 → ローカル YAML へ書き込み (write-to-yaml のみ)
/lifequest reload --use-mysql
```

`--use-local`:

1. コンフリクト中のクエストをローカル YAML で MySQL に上書き
2. `conflict = FALSE` に戻す
3. 通常の reload を実行

`--use-mysql`:

1. コンフリクト中のクエストを MySQL の内容でローカル YAML に書き込み
2. `conflict = FALSE` に戻す
3. 通常の reload を実行
4. 再度 reload を叩くことで、その他すべての差分も MySQL に反映可能

### 4. QuestVersion

```yaml
# QuestType YAML に自動付与されるメタデータ
# ユーザーが手動で編集する必要はない
_lifequest:
  version: 1
  checksum: "sha256..."
  updated_at: "2026-07-14T12:00:00Z"
  updated_by: "server-uuid"
```

## バックアップ

`write-to-mysql = true` のサーバーで自動実行。

```yaml
backup:
  enabled: true
  interval-hours: 24
  retention-days: 30
  directory: "plugins/LifeQuest/backups/"
```

- バックアップファイル: `plugins/LifeQuest/backups/yyyy-MM-dd-HH.tar.gz`
- 内容: `plugins/LifeQuest/@namespace/` 以下の全 YAML + config.yml
- 保持ポリシー: 各サーバーごとに `retention-days` で制御
- 保持期間超過ファイルは起動時 / 定期タスクで削除

## 読取り専用サーバー

`write-to-mysql: false` + `write-to-yaml: false` の場合:

- クエスト定義一覧の表示: ✅
- クエスト詳細の表示: ✅
- 進捗状況の表示: ✅
- クエストの開始: ✅ (プレイヤー操作は全サーバーで可能)
- クエスト定義の編集: ❌
- クエストの grant / revoke: ❌ (管理コマンドを制限)
