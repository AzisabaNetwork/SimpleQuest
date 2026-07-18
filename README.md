# SimpleQuest

アジ鯖 Reincarnation PvE サーバー向け Paper プラグインのクエストマネージャー。

| 項目 | 内容 |
|---|---|
| **Server** | Paper 1.21.11 |
| **Language** | Kotlin (JVM 21) |
| **DB** | MariaDB + Exposed + HikariCP |
| **GUI** | Kunectron (optional) |

> **Original GUI System: [AzisabaNetwork/quem](https://github.com/AzisabaNetwork/quem) by tksimeji**
>
> `src/main/kotlin/net/azisaba/simplequest/gui/` 以下の GUI 実装は
> Quem の Kunectron ベース実装を移植・改変したものです。
> 各ファイルの先頭にもクレジットを記載しています。

## クイックスタート

```bash
# ビルド
./gradlew jar

# テスト (440 tests)
./gradlew test
```

生成された Jar をサーバーの `plugins/` に配置。
Kunectron も `plugins/` に導入すると GUI が有効になる。

## ディレクトリ構成

```
SimpleQuest/
├── AGENTS.md                    # プロジェクトコンテキスト
├── src/main/kotlin/net/azisaba/simplequest/
│   ├── SimpleQuest.kt             # エントリポイント
│   ├── SimpleQuestBootstrap.kt    # PluginBootstrap + コマンド
│   ├── SimpleQuestLoader.kt       # YAML 読み込み
│   ├── action/                  # 報酬アクション
│   ├── database/                # DB 接続/同期/Webhook/バックアップ
│   ├── gui/                     # Kunectron GUI
│   ├── party/                   # パーティシステム
│   ├── quest/                   # クエストエンジン
│   └── ...
├── examples/                    # YAML クエストサンプル
├── docs/                        # 仕様書
└── gradle/libs.versions.toml    # 依存管理
```

## ドキュメント

- `docs/README.md` — 全ドキュメント目次
- `AGENTS.md` — 技術スタック・構造・記法リファレンス
- `examples/` — クエスト YAML サンプル

## ライセンス

GPL-3.0
