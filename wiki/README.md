# SimpleQuest Wiki

Paper 1.21.11 向けクエスト管理プラグイン **SimpleQuest** の Wiki です。

## 目次

| ページ | 内容 |
|---|---|
| [サーバーセットアップ手順](SERVER_SETUP.md) | 必要環境、ビルド、データベース構築、プラグイン導入、動作確認 |
| [config.yml 設定リファレンス](CONFIGURATION.md) | 全設定項目の説明とデフォルト値 |
| [コマンドと権限](COMMANDS.md) | 管理コマンド / プレイヤーコマンド一覧 |
| [クエスト定義の作成](QUEST_CREATION.md) | YAML によるクエスト記法と具体例 |
| [Objectives とイベントトリガー](quest/EVENTS_AND_OBJECTIVES.md) | 達成条件の命名規則・全11イベント一覧 |
| [完了時アクション](quest/ACTIONS.md) | クエストクリア時の報酬設定（Command/Item/MythicItem/PvELevel） |
| [クエスト解放条件 (Unlock)](quest/UNLOCK.md) | エリア進入などによる自動解放の設定 |

## 関連ドキュメント

プロジェクトの設計・仕様に関する詳細は `docs/` 以下を参照してください。

| ドキュメント | 内容 |
|---|---|
| [docs/SETUP.md](../docs/SETUP.md) | 動作環境・ビルド概要 |
| [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) | アーキテクチャ概要 |
| [docs/QUEST_SYSTEM.md](../docs/QUEST_SYSTEM.md) | クエストシステムの内部設計 |
| [docs/MULTI_SERVER.md](../docs/MULTI_SERVER.md) | マルチサーバー同期仕様 |
| [docs/GUI.md](../docs/GUI.md) | GUI システム設計 |
| [docs/COMMANDS.md](../docs/COMMANDS.md) | コマンド一覧（実装リファレンス） |
