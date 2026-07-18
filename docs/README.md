# SimpleQuest ドキュメント

> [!NOTE]
> サーバー管理者向けのセットアップ手順・コマンドリファレンスは **[wiki/](../wiki/README.md)** を参照してください。

## 概要

- [アーキテクチャ概要](ARCHITECTURE.md) — プロジェクト構成と依存関係、起動フロー、データストレージ設計
- [データモデル](DATA_MODEL.md) — シリアライゼーション用 data class 一覧 (YAML / DB スキーマ)
- [動作環境・ビルド](SETUP.md) — 対応サーバー、ビルド手順

## 機能別仕様

- [名前空間・設定システム](NAMESPACE_AND_CONFIG.md) — Namespace の定義方法、YAML 読み込み、config.yml
- [アクションシステム](QUEST_SYSTEM.md#actionsアクションシステム) — クエスト完了時の報酬アクション
- [クエストシステム](QUEST_SYSTEM.md) — Quest / クエスト条件 / ライフサイクル
- [ステージシステム](STAGE_SYSTEM.md) — Stage, Queue, Mount/Unmount
- [パーティシステム](PARTY_SYSTEM.md) — Party, Invite, 権限管理
- [スクリプトシステム](SCRIPT_SYSTEM.md) — Script, Trigger, 遅延実行, 拡張コマンド構文
- [コマンド一覧](COMMANDS.md) — 管理コマンド / Party コマンド
- [GUI システム](GUI.md) — プレイヤー UI 設計

## 設計メモ

> 以下のファイルは初期構想メモです。本格的な仕様検討のインプットとして参照してください。

- [ABOUT.md](ABOUT.md) — データ保持・Action システム・クエスト基本構造の初期案
- [MEMO.md](MEMO.md) — クエスト開放条件・イベント連携・UI設計の補足メモ

## 記法設計

- [クエスト記法設計案](QUEST_FORMAT_DESIGN.md) — MythicMobs ライクな YAML 記法の比較検討 (本ページ)

## マルチサーバー同期

- [マルチサーバー同期仕様](MULTI_SERVER.md) — SSOT / コンフリクト解決 / 同期フロー (本ページ)
- [Webhook ライブラリ比較](WEBHOOK_LIBRARY.md) — ktor-client vs JDA vs Kord (本ページ)

## 技術選定

- [技術スタック選定](STACK_DECISIONS.md) — ライブラリ・ツールの比較検討 (本ページ)
- [DBレイヤー比較：jdbi vs Exposed](DB_COMPARISON.md) — クエリ・マイグレーション・書き味の詳細比較
- [Webhook ライブラリ比較](WEBHOOK_LIBRARY.md) — ktor-client vs JDA vs Kord (本ページ)
