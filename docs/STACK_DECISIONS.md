# 技術スタック選定

> LifeQuest で使用するライブラリ・ツールの選定資料。
> 各カテゴリで複数候補を比較し、推奨を提示する。

## 凡例

| 記号 | 意味 |
|---|---|
| ★ 推奨 | 本プロジェクトに最も適した選択 |
| ◯ 対応可 | 採用しても問題ないが非推奨 |
| △ 非推奨 | 明確な理由があり推奨しない |

---

## 1. ビルドシステム

| 候補 | 評価 | 理由 |
|---|---|---|
| ★ **Gradle Kotlin DSL** | 推奨 | Kotlin プロジェクトの標準。型安全、IDE補完が効く |
| △ Gradle Groovy DSL | 非推奨 | 型安全でない、IDE補完が弱い、Kotlinプロジェクトで使う理由がない |

### 採用方針

- `build.gradle.kts` を使用
- `libs.versions.toml` でバージョンカタログ一元管理
- 現時点ではマルチモジュール構成は採用せず、単一モジュールで開始

```toml
# gradle/libs.versions.toml
[versions]
kotlin = "2.1.0"
paperweight = "2.0.0"
paper-api = "1.21.11-R0.1-SNAPSHOT"

exposed = "1.3.1"
hikari = "6.2.1"
mariadb = "3.5.2"
lettuce = "6.5.4"
kaml = "0.63.0"
coroutines = "1.9.0"

[libraries]
# Paper
paper-api = { module = "io.papermc.paper:paper-api", version.ref = "paper-api" }

# Database
exposed-core = { module = "org.jetbrains.exposed:exposed-core", version.ref = "exposed" }
exposed-dao = { module = "org.jetbrains.exposed:exposed-dao", version.ref = "exposed" }
exposed-jdbc = { module = "org.jetbrains.exposed:exposed-jdbc", version.ref = "exposed" }
exposed-migration-jdbc = { module = "org.jetbrains.exposed:exposed-migration-jdbc", version.ref = "exposed" }
hikari = { module = "com.zaxxer:HikariCP", version.ref = "hikari" }
mariadb = { module = "org.mariadb.jdbc:mariadb-java-client", version.ref = "mariadb" }

# Redis
lettuce = { module = "io.lettuce:lettuce-core", version.ref = "lettuce" }

# YAML / Serialization
kaml = { module = "net.mamoe.yamlkt:kaml", version.ref = "kaml" }
kotlinx-serialization-yaml = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }  # YAML は kaml, JSON は kotlinx

# Coroutines
kotlinx-coroutines = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
paperweight = { id = "io.papermc.paperweight.userdev", version.ref = "paperweight" }
```

---

## 2. Paper / Bukkit

### paperweight-userdev の選択

| 候補 | 評価 | 理由 |
|---|---|---|
| ★ **paperweight-userdev** | 推奨 | Paper 公式のビルドプラグイン。再マッピング自動化、DevBundle対応 |
| ◯ paper-api 直接依存 | 対応可 | 軽量だが再マッピングを手動管理する必要がある |

### Paper API バージョン

- **Paper 1.21.11** に対応
- 対応する DevBundle バージョンはリリースノートで確認する (2026-07-14 時点)
- `paperweight.paperDevBundle("...")` で統一的に依存解決

---

## 3. YAML パース

| 候補 | 評価 | 理由 |
|---|---|---|
| ★ **kaml** | 推奨 | kotlinx.serialization と統合。data class に直接デシリアライズ。型安全 |
| △ snakeyaml | 非推奨 | 型安全でない。Map<String, Any> からの手動パースが必要 |
| △ kotlinx-serialization-yaml | 非推奨 | 開発が停滞気味。kaml の方が Paper エコシステムで実績あり |

**決め手**: 全 YAML 設定は `@Serializable` data class にマッピングする設計なので、kaml が自然。

---

## 4. データベース

### 方針

- **ORM**: Exposed (DSL + DAO)
- **マイグレーション**: Exposed Migration
- **コネクションプール**: HikariCP
- **DBMS**: MariaDB

### クエリレイヤー比較

| 候補 | 評価 | 理由 |
|---|---|---|
| ★ **Exposed** | 採用 | Kotlin ネイティブ。DSL と DAO の2モード。JetBrains 公式で v1.3.1 安定稼働。テーブル定義とクエリを型安全に記述可能 |
| ◯ jdbi | 対応可 | 今回は見送り。Exposed + Exposed Migration で一本化 |
| ◯ jOOQ | 対応可 | コード生成が重い。Kotlin 対応は良いがセットアップコストが高い |
| △ 生 JDBC | 非推奨 | クエリマッピング・リソース管理を自前で書く必要がある |

### マイグレーション

Exposed 公式の `exposed-migration-jdbc` モジュールを使用する。

```kotlin
class V1__CreateQuestProgress : Migration() {
    override fun migrate() {
        exec("""
            CREATE TABLE quest_progress (
                player_uuid  BINARY(16)    NOT NULL,
                quest_key    VARCHAR(255)  NOT NULL,
                req_key      VARCHAR(255)  NOT NULL,
                progress     INT           NOT NULL DEFAULT 0,
                PRIMARY KEY (player_uuid, quest_key, req_key)
            )
        """)
    }
    override val version: Long = 1
    override val description: String = "Create quest_progress"
}
```

```kotlin
// マイグレーション実行
transaction {
    SchemaUtils.withDataMigration(
        "lifequest_migrations",
        listOf(V1__CreateQuestProgress())
    ).apply { migrationTool.migrate() }
}
```

### Exposed クエリ例

```kotlin
// DSL
transaction {
    QuestProgressTable.selectAll()
        .where { (playerUuid eq uuid) and (questKey eq key) }
        .forEach { row -> row[QuestProgressTable.progress] }
}

// DAO (エンティティ操作)
transaction {
    val p = QuestProgressEntity.find {
        (QuestProgressTable.playerUuid eq uuid) and
        (QuestProgressTable.questKey eq key)
    }.firstOrNull()
    p?.progress = newValue  // 自動で UPDATE
}
```

### トランザクション管理

- HikariCP 接続プール + Exposed JDBC transaction
- 同期 JDBC で十分 (Bukkit のメインスレッド駆動アーキテクチャと親和性が高い)
- テストは `transaction { }` ブロックの自動ロールバックが便利

---

## 5. Redis クライアント

| 候補 | 評価 | 理由 |
|---|---|---|
| ★ **Lettuce** | 推奨 | 非同期・リアクティブ対応。コルーチン統合可能。スレッドセーフ |
| ◯ Jedis | 対応可 | シンプルだがブロッキングAPIのみ。コネクションプール管理が手間 |
| △ Redisson | 対応可 | 高機能だが重量。本プロジェクトにはオーバースペック |

**決め手**: Lettuce は非同期API + コルーチン対応が可能で、同一接続を共有しながら安全に操作できる。コネクションプール不要。

---

## 6. GUI フレームワーク

| 候補 | 評価 | 理由 |
|---|---|---|
| ★ **Kunectron** | 推奨 | アノテーションベースで宣言的に GUI を記述。compileOnly で依存 |
| △ 自前実装 | 非推奨 | 車輪の再発明。Kunectron で十分 |

- Kunectron は **compileOnly** 依存とし、実行時はサーバーにプラグインとして導入
- Lore ベース UI はプラグイン独自実装 (Kunectron 非依存)

---

## 7. コマンドフレームワーク

| 候補 | 評価 | 理由 |
|---|---|---|
| **Paper Brigadier** | 選択肢 | Paper API 標準。追加依存不要 |
| ★ **Cloud Commands** | 推奨 | 型安全・宣言的・アノテーション対応。Brigadier のラッパーとして高機能 |
| ◯ 手動パース | 非推奨 | 保守性が低い |

**Cloud のメリット**:

- `@Command`, `@Sender`, `@Argument` アノテーションで宣言的に記述
- 引数のバリデーション・サジェストが内蔵
- Paper の Brigadier と完全互換

**注意点**: Cloud は Paper 1.21 以降の Brigadier 統合に追随している必要がある。バージョン確認必須。

---

## 8. メッセージ・テキスト

| 候補 | 評価 | 理由 |
|---|---|---|
| ★ **Adventure + MiniMessage** | 推奨 | Paper 標準搭載。MiniMessage は YAML との親和性が高い |
| △ 生 Component API | 対応可 | MiniMessage の方が設定ファイルと相性が良い |

MiniMessage 採用により:

- YAML 内で `<green>`, `<bold>`, `<click:...>` などのタグが使える
- `&a` 形式のレガシーカラーコードとの変換も可能
- 翻訳キー管理に translatable component を使える

```yaml
title: "<gold>Wolf Slayer</gold>"
description:
  - "<gray>オオカミを10体討伐しろ！</gray>"
```

---

## 9. テスティング

| 候補 | 評価 | 理由 |
|---|---|---|
| ★ **JUnit 5 + MockBukkit** | 推奨 | Paper プラグインの標準的テスト構成。Bukkit API のモックが充実 |
| ◯ kotest | 対応可 | Kotlin ネイティブのテスティングフレームワーク。MockBukkit と併用可能 |

### テスト構成方針

```kotlin
// JUnit 5 + MockBukkit
class QuestTest {
    @Test
    fun `quest should complete when all requirements met`() {
        // MockBukkit でサーバーをモック
        val server = MockBukkit.mock()
        try {
            // テスト
        } finally {
            MockBukkit.unmock()
        }
    }
}
```

---

## 10. Kotlin 機能

| 機能 | 採用 | 理由 |
|---|---|---|
| **kotlinx.serialization** | ★ | @Serializable で全データクラスを統一的に管理 |
| **kotlinx.coroutines** | ★ | BukkitRunnable の代替。非同期処理・遅延実行に使用 |
| **Result / sealed class** | ★ | ADT によるエラー処理。成功/失敗を型で表現 |
| **Context receivers** | △ | Kotlin 2.1 では experimental。採用を見送り |
| **KSP** | ◯ | 現時点では不要。必要になった時点で導入を検討 |

---

## 推奨スタック サマリー

| カテゴリ | 採用 | バージョン目安 |
|---|---|---|
| 言語 | Kotlin | 2.1.x |
| JVM | Java 21 | 21 |
| Build | Gradle Kotlin DSL + Version Catalog | 8.x |
| Paper | paperweight-userdev + DevBundle | 2.0.0 |
| YAML | kaml | 0.63.x |
| DB (ORM) | Exposed (DSL + DAO) | 1.3.x |
| DB (Migration) | Exposed Migration | 1.3.x |
| DB (Pool) | HikariCP | 6.2.x |
| DB (Driver) | MariaDB Connector | 3.5.x |
| Redis | Lettuce | 6.5.x |
| GUI | Kunectron (compileOnly) | latest |
| Command | Cloud Commands | latest |
| Text | Adventure + MiniMessage | (Paper bundled) |
| Test | JUnit 5 + MockBukkit | latest |
| Async | kotlinx.coroutines | 1.9.x |
| Serialization | kotlinx.serialization | (Kotlin bundled) |

---

## 未確定項目

- [ ] **Paper 1.21.11** 対応の DevBundle バージョン (要リリース確認)
- [ ] Cloud Commands の Paper 1.21.11 互換性
- [ ] マルチモジュール構成の要不要 (core / plugin 分割)
- [ ] DI (Koin) の導入是非 — 現時点では手動 DI で十分な規模
- [ ] ロギング (Slf4j / Log4j) の設定方針
