# SimpleQuest

Paper 1.21.11 向けクエスト管理プラグイン。
Kotlin + Exposed + HikariCP、MythicMobs ライクな YAML 記法でクエストを定義し、マルチサーバー同期に対応。

## 開発ワークフロー

### ブランチ戦略

- `main` から `feat/<issue-id>-<short-description>` または `fix/<issue-id>-<short-description>` を切って作業する
- 完了後 PR を作成し、レビュー後にマージ（マージは管理者が手動で行う）
- **明示的に push を指示されるまで `git push` は行わないこと**

### コミットメッセージ

[Conventional Commits](https://www.conventionalcommits.org/) に従い、以下の prefix で細かくコミットする：

| prefix | 用途 |
|---|---|
| `feat:` | 新機能 |
| `fix:` | バグ修正 |
| `refactor:` | リファクタリング |
| `test:` | テスト追加・修正 |
| `docs:` | ドキュメント |
| `chore:` | ビルド・CI・依存関係 |

コミットは論理的な単位で小さく分割し、1コミットに複数の関心事を混在させない。

### Issue 対応

対応した Issue には、作業完了後に **どういった対応を行ったか** をコメントで記載する。
コードの変更概要、判断理由、残課題があればそれも含める。

## 技術スタック

| カテゴリ | 採用 | バージョン |
|---|---|---|
| 言語 | Kotlin | 2.3.20 |
| JVM | Java 21 | 21 |
| Build | Gradle Kotlin DSL + Version Catalog | 8.12 |
| Platform | Paper API | 1.21.11-R0.1-SNAPSHOT |
| ORM | Exposed (DSL + DAO) | 1.3.1 |
| DB 接続 | HikariCP | 6.2.1 |
| DBMS | MariaDB (本番) / H2 MariaDB モード (テスト) | 3.5.2 / 2.2.224 |
| YAML | kaml (charleskorn) | 0.104.0 |
| GUI | Kunectron (compileOnly) | 1.0.0-beta.13 |
| HTTP | ktor-client (CIO) | 3.1.0 |
| テスト | kotest + MockBukkit | 6.2.2 / 3.133.2 |

### アーキテクチャ概要

- `action/` — クエスト完了時のアクションシステム（Command, Item, MythicItem, PvELevel）
- `quest/` / `stage/` — クエストドメインと進行管理
- `database/` — Exposed テーブル定義 + HikariCP 直SQL リポジトリ、マルチサーバー同期・バックアップ・Webhook
- `gui/` — Kunectron ベースの GUI（クエスト選択、パーティ管理など 6種）
- `script/` — 遅延実行や条件付きスクリプト
- `registry/` — クエスト定義のインメモリレジストリ

## テスト

### 方針

- 新機能・バグ修正では **必ず対応するユニットテストを追加する**
- 複数のコンポーネントを跨ぐ変更では **統合テストも適時追加する**
- テストがないコードはレビューで差し戻す

### 実行

```bash
./gradlew test
```

DB テストは H2 MariaDB モードで実行。`DatabaseHelper.setTestDataSource()` で DataSource 差し替え可能。

## 参考ドキュメント

- `docs/README.md` — 全ドキュメント目次
- `docs/MULTI_SERVER.md` — マルチサーバー同期仕様
- `docs/STACK_DECISIONS.md` — 技術選定理由
