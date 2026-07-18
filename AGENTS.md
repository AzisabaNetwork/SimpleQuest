# SimpleQuest

## 概要

Paper 1.21.11 向けクエスト管理プラグイン。Kotlin + Exposed + HikariCP。
MythicMobs ライクな YAML 記法でクエスト定義を記述し、マルチサーバー同期に対応。

## 技術スタック

| カテゴリ | 採用 | バージョン |
|---|---|---|
| 言語 | Kotlin | 2.3.20 |
| JVM | Java 21 | 21 |
| Build | Gradle Kotlin DSL + Version Catalog | 8.12 |
| Paper | paper-api | 1.21.11-R0.1-SNAPSHOT |
| ORM | Exposed (DSL + DAO) | 1.3.1 |
| DB接続 | HikariCP | 6.2.1 |
| DBMS | MariaDB + H2 (テスト) | 3.5.2 / 2.2.224 |
| YAML | kaml (charleskorn) | 0.104.0 |
| GUI | Kunectron (compileOnly) | 1.0.0-beta.13 |
| HTTP | ktor-client (CIO) | 3.1.0 |
| テスト | kotest + MockBukkit | 6.2.2 / 3.133.2 |

## プロジェクト構造

```
src/main/kotlin/net/azisaba/simplequest/
├── SimpleQuest.kt                — プラグインエントリポイント
├── SimpleQuestBootstrap.kt       — PluginBootstrap + コマンド
├── SimpleQuestLoader.kt          — YAML 名前空間スキャン
├── action/                     — Action システム (4種)
├── command/                    — Formula パーサー
├── data/                       — @Serializable data class
│   └── yaml/                   — YAML クエストスキーマ
├── database/                   — DB 接続/同期/バックアップ/Webhook
│   ├── table/                  — Exposed テーブル定義
│   └── repository/             — HikariCP 直SQL リポジトリ
├── gui/                        — Kunectron GUI (6種)
├── listener/                   — Bukkit イベントリスナー
├── party/                      — パーティシステム
├── quest/                      — クエストドメイン
├── registry/                   — インメモリレジストリ
├── script/                     — スクリプトシステム
└── stage/                      — ステージシステム
```

## コマンド

| コマンド | 権限 | 説明 |
|---|---|---|
| `/simplequest` | — | バージョン表示 |
| `/simplequest reload [--use-local\|--use-mysql]` | simplequest.reload | 設定再読み込み / コンフリクト解決 |
| `/simplequest quest` | — | クエスト選択GUI |
| `/simplequest party` | — | パーティー管理GUI |
| `/party invite <player>` | — | パーティ招待 |
| `/party accept <id>` | — | 招待承諾 |
| `/party kick <player>` | リーダー | メンバーキック |

## マルチサーバー同期

- `write-to-mysql: true`: 起動時/reload時にローカルYAMLをMySQLへUPSERT
- `write-to-yaml: true`: MySQLの内容でローカルYAMLを更新
- 両方 `false`: 読取り専用 (進捗表示のみ)
- コンフリクト: checksum不一致時に `conflict` フラグ。`/simplequest reload --use-local/--use-mysql` で解決

## テスト

```bash
./gradlew test   # 123 tests, 全 PASS
```

DBテストは H2 MariaDB モードで実行。`DatabaseHelper.setTestDataSource()` で DataSource 差し替え可能。

## YAML クエスト記法

```yaml
QuestName:
  Title: "&aタイトル"
  Description: ["&7説明"]
  Icon: "MATERIAL:CMD"
  Aura: true
  Giver: "&e依頼人"
  Category: "lq:general"
  Location: "world,x,y,z"
  Options:
    MaxParty: "1-4"         # "min-max" or "max"
    Limits:
      Daily: 3
      Weekly: 10
      Monthly: 30
      Yearly: 365
      Lifetime: 100
    DeathLimit: 3
  Requirements:
    PvELevel: 10
    Money: 100.0
    PartyMode: true
  Objectives:
    KillZombie: 10           # 数値 or "material*amount"
  Actions:
    OnFirstComplete:
      - Type: Command
        Params: "give % minecraft:diamond"
    OnComplete:
      - Type: Item
        Params: "minecraft:diamond,5"
      - Type: MythicItem
        Params: "MMOre,3"
      - Type: PvELevel
        Params: "100"
  Guides:
    - Title: "&a地点"
      Location: "x,y,z"
      Condition: "req=1"
  Scripts:
    OnStart: ["say start"]
    OnStart+20: ["say delayed"]
    OnComplete: ["say done"]
    OnCancel: ["say cancelled"]
```

## 参考

- `docs/README.md` — 全ドキュメント目次
- `docs/MULTI_SERVER.md` — マルチサーバー同期仕様
- `docs/STACK_DECISIONS.md` — 技術選定理由
