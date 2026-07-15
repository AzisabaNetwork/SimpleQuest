# GUI システム

## 設計方針

Kunectron ベースの GUI に加えて、**Lore ベースの詳細表示**を導入する。

- **一覧表示系** (クエスト選択・パーティ管理など): 従来の ChestGui
- **詳細表示系** (クエスト進捗・条件確認): Lore ベース (プレイヤーの 2x2 クラフト枠を利用)

## GUI 一覧 (案)

| GUI 名 | 種類 | 用途 | 備考 |
|---|---|---|---|
| GUI 名 | 種類 | 用途 | 備考 |
|---|---|---|---|
| QuestGui | ChestGui | クエスト選択画面 (カテゴリ別、ページング、検索) | 標準 |
| QuestMenuGui | ChestGui | クエスト進行中のメニュー | 標準 |
| QuestDetailGui | 2x2 クラフト枠 | クエスト詳細表示 (Lore) | **新規** |
| QuestPanelGui | ScoreboardGui | クエスト進行中のスコアボード | 標準 |
| PartyMenuGui | ChestGui | パーティ管理画面 | 標準 |
| PartyCreateGui | ChestGui | パーティ作成画面 | 標準 |
| PartyInviteGui | ChestGui | 招待プレイヤー選択画面 | 標準 |
| ConfirmGui | ChestGui | 確認ダイアログ | 標準 |
| SearchGui | SignGui | 検索用看板入力 | 標準 |

## QuestGui (クエスト選択)

- **タイトル**: `gui.quest` (翻訳キー)
- **サイズ**: 6段 (54スロット)
- **カテゴリタブ**: 上部 1-7 スロットに表示 (ページング対応)
  - 先頭は常に "All" (カテゴリ未指定)
  - 選択中のカテゴリは緑色の stained glass pane で表示
  - 左右の PlayerHead でカテゴリのページ切り替え
- **クエスト一覧**: 中央 3-5段に 21 スロットで表示
  - 各スロットに QuestType のアイコン (Lore 付き)
  - アイコンクリックでクエスト開始
- **下部**: 前へ/次へ (ページング), 閉じる, 検索 (コンパス)

### クエスト開始クリック時のバリデーション

1. プレイヤーがパーティリーダーか
2. パーティが既にクエスト中でないか
3. パーティがサイズ条件を満たしているか
4. 全メンバーが解放済みか
5. 全メンバーが受注条件を満たしているか (PvELevel など)

## QuestDetailGui（クエスト詳細表示）

プレイヤーの 2x2 クラフトスロットを利用した Lore ベース UI。

### コンセプト

- プレイヤーが特定の操作（例: ホットキーやアイテムクリック）で呼び出す
- 2x2 クラフトグリッドの各スロットに機能を割り当て
- 情報はアイテムの Lore として表示（専用 GUI への遷移なし）

### 割り当て案

| スロット | 機能 |
|---|---|
| 左上 | クエスト情報 (タイトル・説明・進捗) |
| 右上 | 現在の Guide / 進行度 |
| 左下 | パーティー情報 (メンバー一覧) |
| 右下 | 中断 (クリック → ConfirmGui) |

### 情報更新

- **自動更新**: 5tick 間隔の BukkitRunnable で Lore を更新
- **負荷対策**: 重い場合はインベントリ開封時とクリック時にのみ更新

## QuestPanelGui (スコアボード)

- **タイトル**: `config.yml#panel.title`
- **表示内容** (動的更新):
  1. `進行中: {quest.type.title}`
  2. `進捗: {progress}/{total}`
  3. (空行)
  4. `パーティー ({size}):`
  5. メンバー一覧 (名前 + HP or 状態)
     - 生存中: アクア色の名前 + ♥HP
     - 離脱済み: 赤色 + 取り消し線 + "離脱"
     - 脱落済み: 赤色 + 取り消し線 + "脱落"
  6. (ステージ情報)
     - マウント中: `ステージ: {title}`
     - キュー待機中: `ステージキュー: {position}/{queueSize}`
  7. `footer` (config.yml#panel.footer)

## PartyMenuGui (パーティ管理)

- **メンバー表示**: 6段 GUI
  - 各メンバーは PlayerHead + 名前
  - リーダーには 👑 マーク
  - Shift+左クリック: キック (リーダーのみ)
  - Shift+右クリック: リーダー委譲 (リーダーのみ)
- **下部ボタン**:
  - 招待設定トグル (COMPARATOR) — LEADER / ALL
  - クエスト選択 (ENCHANTING_TABLE) — クエスト中でない場合のみ
  - 閉じる
  - 脱退/解散 (TNT_MINECART) — ConfirmGui 経由
- パーティに空きがあり、招待権限がある場合、追加アイコン表示

## PartyInviteGui (招待)

- オンラインプレイヤー一覧から招待対象を選択
- 検索機能 (SearchGui 経由)
- 既にメンバーのプレイヤーは除外

## ConfirmGui (確認ダイアログ)

- 3段 GUI
- GREEN_TERRACOTTA / RED_TERRACOTTA で Yes/No
- `onAccept` / `onReject` の Runnable をコンストラクタで受け取る

## SearchGui (検索)

- Kunectron SignGui を使用
- 看板の最初の行に入力された文字列を `searchable.search(query)` に渡す
- `SearchableGui` インターフェースを実装した GUI から利用可能

## UpdatableGui インターフェース

```kotlin
interface UpdatableGui {
    fun update()
}
```

PartyGui がこれを実装し、Party の状態変化時に全開いている PartyGui インスタンスを一括更新する。
