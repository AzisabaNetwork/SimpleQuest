# 動作環境・ビルド

## 動作環境

| 項目 | 要件 |
|---|---|
| **サーバー** | Paper 1.21.11 以上 |
| **Java** | Java 21 (JDK 21) |
| **ビルドツール** | Gradle 8.12+ (wrapped) |

### 推奨プラグイン (サーバー側)

| プラグイン | 必須 | 用途 |
|---|---|---|
| Kunectron | ✅ compileOnly | GUI フレームワーク。Libs フォルダに配置 |
| MythicMobs | ❌ softdepend | MythicItem アクション (未実装) |
| PvELevel | ❌ softdepend | PvELevelExp アクション (未実装) |

## ビルド

```bash
# コンパイル
./gradlew build

# テスト
./gradlew test

# Jar 作成
./gradlew jar
# → build/libs/LifeQuest-1.0.0-SNAPSHOT.jar
```

## 開発サーバーセットアップ

```bash
# 1. ビルド
./gradlew jar

# 2. Jar をサーバーの plugins/ にコピー
cp build/libs/LifeQuest-*.jar /path/to/server/plugins/

# 3. Kunectron も plugins/ に配置
#    https://github.com/tksimeji/kunectron からダウンロード

# 4. サーバー起動
```

## クエスト定義の追加

サーバー起動後、`plugins/LifeQuest/` に以下の構造で YAML を配置:

```
plugins/LifeQuest/
└── @lq/
    └── types/
        ├── wolf_slayer.yml
        └── supply_procurement.yml
```

リロード:

```bash
/lifequest reload
```

## テスト

```bash
# 全テスト実行
./gradlew test

# 特定テストクラス
./gradlew test --tests "net.azisaba.lifequest.QuestTypeTest"

# 詳細ログ
./gradlew test --info
```

## プロジェクト構成

```
src/
├── main/
│   └── kotlin/net/azisaba/lifequest/
│       ├── LifeQuest.kt             — プラグインエントリポイント
│       ├── LifeQuestBootstrap.kt    — PluginBootstrap + コマンド登録
│       ├── LifeQuestLoader.kt       — YAML 名前空間スキャン
│       ├── action/                  — Action システム
│       ├── command/                 — コマンドユーティリティ
│       ├── data/                    — @Serializable data class
│       │   └── yaml/               — YAML クエストスキーマ
│       ├── database/                — DB 接続 + リポジトリ
│       ├── gui/                     — Kunectron GUI
│       ├── listener/                — Bukkit イベントリスナー
│       ├── party/                   — パーティシステム
│       ├── quest/                   — クエストドメイン
│       ├── registry/                — インメモリレジストリ
│       ├── script/                  — スクリプトシステム
│       └── stage/                   — ステージシステム
└── test/
    └── kotlin/net/azisaba/lifequest/  — kotest + MockBukkit テスト
```
