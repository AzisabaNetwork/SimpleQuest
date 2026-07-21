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
# → build/libs/SimpleQuest-1.0.0-SNAPSHOT.jar
```

## 開発サーバーセットアップ

```bash
# 1. ビルド
./gradlew jar

# 2. Jar をサーバーの plugins/ にコピー
cp build/libs/SimpleQuest-*.jar /path/to/server/plugins/

# 3. Kunectron も plugins/ に配置
#    https://github.com/tksimeji/kunectron からダウンロード

# 4. サーバー起動
```

## クエスト定義の追加

サーバー起動後、`plugins/SimpleQuest/` に以下の構造で YAML を配置:

```
plugins/SimpleQuest/
└── @lq/
    └── types/
        ├── wolf_slayer.yml
        └── supply_procurement.yml
```

リロード:

```bash
/simplequest reload
```

## テスト

```bash
# 全テスト実行
./gradlew test

# 特定テストクラス
./gradlew test --tests "net.azisaba.simplequest.QuestTypeTest"

# 詳細ログ
./gradlew test --info
```

## プロジェクト構成

```
src/
├── main/
│   └── kotlin/net/azisaba/simplequest/
│       ├── SimpleQuest.kt             — プラグインエントリポイント
│       ├── SimpleQuestLoader.kt       — YAML 名前空間スキャン・QuestType 変換
│       ├── application/quest/         — クエストサービスクラス
│       ├── command/                   — コマンドユーティリティ (Formula)
│       ├── data/                      — @Serializable config / YAML schema
│       ├── database/                  — DB 接続、リポジトリ、Flyway マイグレーション
│       ├── di/                        — Dagger DI 設定
│       ├── domain/                    — ドメイン層
│       │   ├── action/               — アクション定義 + ポート
│       │   ├── data/                 — Icon, Location
│       │   ├── party/model/          — Party, Invite
│       │   ├── quest/model/          — Quest, QuestType, Progresses 等
│       │   ├── quest/port/           — Repository, Notifier ポート
│       │   ├── registry/             — Registry, Keyed
│       │   ├── script/               — Script 定義 + Runner ポート
│       │   └── stage/                — Stage, StageLike
│       ├── gui/                       — Kunectron GUI
│       ├── infrastructure/            — インフラ層 (Bukkit 実装)
│       ├── listener/                  — Bukkit イベントリスナー
│       ├── party/                     — パーティ実装
│       ├── quest/                     — クエスト実装
│       ├── registry/                  — レジストリ定義
│       └── stage/                     — ステージ実装
└── test/
    └── kotlin/net/azisaba/simplequest/  — kotest + MockBukkit テスト
```
