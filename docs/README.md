# SimpleQuest ドキュメント

> [!NOTE]
> サーバー管理者向けのセットアップ手順・コマンドリファレンスは **[wiki/](../wiki/README.md)** を参照してください。

## 概要

- [アーキテクチャ概要](ARCHITECTURE.md) — プロジェクト構成と依存関係、起動フロー
- [データモデル](DATA_MODEL.md) — シリアライゼーション用 data class 一覧
- [動作環境・ビルド](SETUP.md) — 対応サーバー、ビルド手順

## 機能別設計

- [クエストシステム](QUEST_SYSTEM.md) — QuestType / PlayLimits / AcceptConditions / ライフサイクル
- [アクションシステム](QUEST_SYSTEM.md#actionsアクションシステム) — クエスト完了時の報酬アクション
- [Objectives とイベントトリガー](../wiki/quest/EVENTS_AND_OBJECTIVES.md) — 達成条件の命名規則（wiki 参照）
- [ステージシステム](STAGE_SYSTEM.md) — Stage / Queue / Mount / Unmount
- [パーティシステム](PARTY_SYSTEM.md) — Party / Invite / 権限管理
- [スクリプトシステム](SCRIPT_SYSTEM.md) — Script / Trigger / 遅延実行
- [GUI システム](GUI.md) — プレイヤー UI 設計
- [名前空間・設定システム](NAMESPACE_AND_CONFIG.md) — Namespace / YAML 読み込み / config.yml

## マルチサーバー同期

- [マルチサーバー同期仕様](MULTI_SERVER.md) — SSOT / コンフリクト解決 / 同期フロー
- [Webhook ライブラリ比較](WEBHOOK_LIBRARY.md) — ktor-client vs JDA vs Kord

## 技術選定

- [技術スタック選定](STACK_DECISIONS.md) — ライブラリ・ツールの比較検討
- [DBレイヤー比較](DB_COMPARISON.md) — jdbi vs Exposed

## 設計メモ（初期構想）

> 以下のファイルは初期構想メモです。現在の実装とは異なる場合があります。

- [ABOUT.md](ABOUT.md) — データ保持・Action システムの初期案
- [MEMO.md](MEMO.md) — クエスト開放条件・イベント連携・UI 設計の補足
- [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) — 実装計画
- [QUEST_FORMAT_DESIGN.md](QUEST_FORMAT_DESIGN.md) — MythicMobs ライクな YAML 記法の比較検討（旧）
