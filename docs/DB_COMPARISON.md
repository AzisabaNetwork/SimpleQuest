# DB レイヤー比較: jdbi + Flyway vs Exposed

> **2026-07-14 決定: Exposed を採用。**
> 本ドキュメントは比較検討時の資料として残してあります。
>
> クエストロジックで SQL を多用する LifeQuest の特性を踏まえ、
> jdbi (SqlObject) + Flyway と Exposed (DSL/DAO, Migration) を多角的に比較する。
> メンテコスト・ベンダーリスク・ライセンス・組織適合性を含めて総合評価。

---

## 0. 前提: このプロジェクトが SQL を「めっちゃ触る」理由

LifeQuest のデータアクセスパターン:

```
クエスト定義読み込み     → YAML (kaml)   ← DB ではない
プレイヤー進捗 CRUD      → SQL (頻繁)    ← ほぼ単純な UPSERT / SELECT
完了履歴 + 集計          → SQL (頻繁)    ← COUNT + WHERE + GROUP BY
プレイ制限判定           → SQL (頻繁)    ← 周期集計 (daily/weekly/monthly)
パーティ情報             → SQL (たまに)  ← CRUD
ステージ管理             → SQL (たまに)  ← CRUD
```

「複雑な JOIN やサブクエリ」より「**シンプルな SQL を大量に書く**」タイプ。
ここが設計判断に大きく影響する。

---

## 1. プロジェクト健全性 & ベンダーリスク

### 1.1 GitHub 統計 (2026-07-14 現在)

| 指標 | jdbi | Flyway | Exposed |
|---|---|---|---|
| **Stars** | 2,131 | 9,914 | 9,265 |
| **Forks** | 364 | 1,621 | 788 |
| **ライセンス** | Apache-2.0 | Apache-2.0 (Community) | Apache-2.0 |
| **最新安定版** | 3.54.0 | 12.11.0 | **1.3.1** |
| **最終更新** | 2日前 | 5日前 | 1日前 |
| **開発主体** | コミュニティ (Steven[S等人) | Redgate (企業) | **JetBrains (企業)** |
| **Open Issues** | 147 | 251 | 167 |

### 1.2 ベンダーリスク評価

| リスク | jdbi | Flyway | Exposed |
|---|---|---|---|
| **開発停止リスク** | △ 低。コアメンテナー2名 + コミュニティ。企業バックアップなし | ◎ 非常に低い。Redgate 製品。収益源 | ◎ 非常に低い。JetBrains 公式 Kotlin プロジェクト |
| **バージョン方針** | セマンティックバージョニング。破壊的変更はメジャーで | セマンティック。ただし Community/Pro/Enterprise で機能差あり | **v1.x 達成 (2025〜)**。セマンティック。安定フェーズ |
| **破壊的変更リスク** | 低。3.x は後方互換を重視 | 低。ただし Pro/Enterprise 機能の Community 版削除リスクあり | △ v0.x → v1.0 で破壊的変更あり。**v1.x 内は安定** |
| **サポート** | GitHub Issues / StackOverflow / Discord | GitHub Issues + Redgate 有償サポート (Pro/Ent) | GitHub Issues / Kotlin Slack (#exposed) / 公式ドキュメント |

### 1.3 補足: Exposed 1.x 到達の意味

Exposed は 2025年に v1.0 をリリース。これにより:

- 0.x 時代の「API がコロコロ変わる」懸念が解消
- `exposed-migration-*` モジュールが公式安定モジュールに昇格
- JetBrains の Kotlin エコシステムにおける最重要プロジェクトの一つとして継続開発中

**「Exposed Migration が難しい」という印象は v0.x 時代のものかもしれない。** 当時はドキュメントも不十分だった。

---

## 2. ライセンス比較

| 観点 | jdbi | Flyway (Community) | Exposed |
|---|---|---|---|
| **SPDX** | Apache-2.0 | Apache-2.0 | Apache-2.0 |
| **商用利用** | ✅ 制限なし | ✅ 制限なし | ✅ 制限なし |
| **修正再配布** | ✅ 可能 | ✅ 可能 | ✅ 可能 |
| **特許条項** | ✅ 明記 | ✅ 明記あり | ✅ 明記あり |
| **コピーレフト** | ❌ なし (非コピーレフト) | ❌ なし | ❌ なし |
| **プロ/エンタープライズ版** | なし | **あり (有償)**。追加機能: ロールバック・SMTP通知・Java callback etc. | なし |
| **ライセンスコスト** | 無料 | Community: 無料 / Pro: 要ライセンス | 無料 |

### Flyway Community の制約 (重要度: 低)

LifeQuest に必要な migration 機能:

| 機能 | Community | Pro | Enterprise |
|---|---|---|---|
| SQL migration | ✅ | ✅ | ✅ |
| Java-based migration | ✅ | ✅ | ✅ |
| チェックサム検証 | ✅ | ✅ | ✅ |
| Undo / ロールバック | ❌ | ✅ | ✅ |
| コマンドラインツール | ✅ | ✅ | ✅ |
| Gradle / Maven plugin | ✅ | ✅ | ✅ |
| Dry run | ❌ | ✅ | ✅ |

**LifeQuest に Undo は不要。** 開発初期のスキーマ変更は「新しい migration ファイルで修正」が標準プラクティス。Community で十分。

---

## 3. メンテナンスコスト

### 3.1 日常のクエリ記述

```
jdbi (Fluent):
  handle.createQuery("""
      SELECT req_key, progress FROM quest_progress
      WHERE player_uuid = :p AND quest_key = :q
  """).bind("p", uuid).bind("q", key).mapToMap()
  → SQL は見やすいが .bind() が 1パラメータ = 1行

jdbi (SqlObject):
  @SqlQuery("SELECT req_key, progress FROM quest_progress WHERE player_uuid = :p AND quest_key = :q")
  fun getProgress(@BindKotlin params: ProgressQuery)
  → SQL も引数もコンパクト。Dagger 体験

Exposed (DSL):
  QuestProgressTable.selectAll().where { (playerUuid eq uuid) and (questKey eq key) }
  → 型安全。カラム名補完。長いクエリは DSL が煩雑になりがち

Exposed (DAO):
  QuestProgressEntity.find { (QuestProgressTable.playerUuid eq uuid) and ... }
  → CRUD 最速。ただし UUID 主キーのテーブルは IntIdTable と相性悪い
```

### 3.2 スキーマ変更時のコスト

```
シナリオ: progress テーブルに「完了日時 completed_at」カラムを追加

[Flyway]
  ① V2__add_completed_at.sql を追加
     ALTER TABLE quest_progress ADD COLUMN completed_at TIMESTAMP NULL;
  ② クエリ側の SQL を書き換え
  → 修正ファイル数: SQL 2ファイル (migration + クエリ)

[Exposed Migration]
  ① V2__AddCompletedAt : Migration() クラスを作成
     exec("ALTER TABLE quest_progress ADD COLUMN completed_at TIMESTAMP NULL")
  ② (任意) Exposed Table に completedAt カラムを追加
  ③ DSL/DAO 側のクエリを書き換え
  → 修正ファイル数: Kotlin 2〜3ファイル

[Exposed (migrationなし、SchemaUtilsのみ)]
  ① Exposed Table に completedAt を追加
  ② SchemaUtils.createMissingTablesAndColumns() を呼ぶ
  ③ DSL/DAO 側のクエリを書き換え
  → Kotlin ファイルのみ。ただし DDL の履歴はコードレビューでしか追えない
```

**開発初期でスキーマ変更が頻繁なら、Exposed の SchemaUtils で済ませてある程度固まったら Flyway に移行する、という2段構えも可能。**

### 3.3 テーブル定義の重複

| 方式 | テーブル定義の場所 | 重複 |
|---|---|---|
| jdbi + Flyway | Flyway SQL のみ | なし (SQL が唯一の定義) |
| Exposed + Flyway | Exposed Table + Flyway SQL | **あり (2重管理)** |
| Exposed (移行のみ) | Exposed Table | なし (ただし DDL 履歴は migration に任せるなら結局重複) |

### 3.4 まとめ: フェーズ別メンテコスト

| フェーズ | jdbi + Flyway | Exposed |
|---|---|---|
| **開発初期** (スキーマ激変) | △ カラム追加のたびに migration SQL が必要 | ◎ SchemaUtils で自動追従。ただし履歴管理が弱い |
| **開発安定期** (スキーマ固定) | ◎ 安定。SQL のメンテナンスのみ | ○ 安定だが Kotlin コードのメンテは必要 |
| **バグ調査** (SQL 確認) | ◎ 生 SQL がそのまま見える | △ DSL → SQL の脳内変換 or ログ確認が必要 |
| **新人参入** | ◎ SQL だけでOK | △ Exposed DSL + DAO + Migration の学習が必要 |

---

## 4. SQL ベンダーリスク (MariaDB 固有機能の利用)

| 観点 | jdbi + Flyway | Exposed |
|---|---|---|
| **MariaDB 独自機能** | ◎ SQL 直書きなので全機能を使える | △ Exposed が MariaDB の機能を全てラップしているわけではない |
| **MySQL との互換性** | 書いた SQL 次第 | Exposed がベンダー差異を吸収してくれる |
| **将来の DB 変更** | △ ほぼ全 SQL の書き直しが必要 | ○ ベンダー差異は Exposed が吸収 (理論上は変更が少ない) |
| **INSERT ... ON DUPLICATE KEY UPDATE** | ✅ 直書き | ✅ Exposed に `upsert()` あり |
| **ウィンドウ関数** | ✅ 直書き | ◯ 一部対応。複雑なものは raw SQL |
| **フルテキスト検索** | ✅ 直書き | ◯ 要確認 |
| **パーティション管理** | ✅ 直書き | △ 非対応の場合あり |

### 実質的なリスク

LifeQuest で使う SQL は `SELECT`, `INSERT`, `UPDATE`, `DELETE`, `COUNT`, `WHERE`, `GROUP BY` が 99%。**ベンダー固有機能に依存するような複雑な SQL はほぼ書かない。** よってベンダーリスクは両者とも低い。

---

## 5. テスト容易性

| 観点 | jdbi + Flyway | Exposed |
|---|---|---|
| **単体テスト用 DB** | H2 + Flyway migrate → jdbi | H2 + transaction {} |
| **セットアップ** | `Flyway.configure().dataSource(h2).load().migrate()` | `Database.connect(h2); transaction { ... }` |
| **テストごとの分離** | 自力で TRUNCATE or ロールバック | transaction 内で書けば自動ロールバック |
| **モック** | SqlObject はインターフェースモック可能 | transaction {} ごとモックは難しい |
| **テスト速度** | △ Flyway migrate が毎回かかる (クラス単位で1回で済ます) | ◎ 軽量 |

### 補足: transaction の自動ロールバック

Exposed のテストは `transaction { }` ブロックが終了すると自動でロールバックされる。これが非常に便利。

jdbi でも `useTransaction` を使えばロールバックはできるが、Exposed ほど「意識せずに書ける」わけではない。

---

## 6. 総合評価マトリクス

| 評価軸 | 重み | jdbi + Flyway | Exposed |
|---|---|---|---|
| **SQL の可視性** | ★★★★★ | 5 / 5 | 2 / 5 |
| **型安全 (カラム名)** | ★★★☆☆ | 2 / 5 (SQL文字列は未チェック) | 5 / 5 |
| **CRUD の記述量** | ★★★★☆ | 3 / 5 (適度) | 5 / 5 (DAO 最強) |
| **複雑クエリの自由度** | ★★★☆☆ | 5 / 5 | 3 / 5 |
| **migration の手軽さ** | ★★★★★ | 5 / 5 (Flyway) | 3 / 5 (Exposed Migration) |
| **開発初期の変更追従** | ★★★★☆ | 2 / 5 | 4 / 5 (SchemaUtils併用時) |
| **ベンダーリスク** | ★★★★☆ | 5 / 5 (両方成熟) | 5 / 5 (JetBrains公式) |
| **ライセンスリスク** | ★★★☆☆ | 4 / 5 | 5 / 5 |
| **新人学習コスト** | ★★★★☆ | 5 / 5 | 2 / 5 |
| **テスト容易性** | ★★★☆☆ | 3 / 5 | 5 / 5 |
| **チーム採用のしやすさ** | ★★★★★ | 5 / 5 | 3 / 5 |
| **コルーチン対応** | ★☆☆☆☆ | 2 / 5 (experimental) | 4 / 5 |
| **** | | | |
| **合計 (加重)** | | **62 / 75** | **54 / 75** |

---

## 7. 結論

### 7.1 どちらを選んでも大丈夫

両方とも Apache-2.0、GitHub 9k+ stars 級のプロジェクト。**ベンダーリスクもライセンスリスクも実質ゼロ。** ここで「間違った選択」は存在しない。

### 7.2 それでも jdbi SqlObject + Flyway を推す理由

```
「SQL をめっちゃ触る」＋「チームで開発する」＋「Flyway 使いたい」
```

この3つが揃うと、SQL 直書きのメリットが Exposed の型安全性のメリットを上回る。

特に:

1. **クエストロジックの SQL は単純** — Exposed の型安全性が生きるほど複雑ではない
2. **Flyway を使いたい** → Exposed + Flyway は二重管理。Flyway に一本化できる jdbi のほうが楽
3. **チーム拡大を考える** — 「SQL 書いて」と言えば誰でも入ってこれる

### 7.3 Exposed を選ぶべきケース (参考)

- チーム全員が Kotlin で、Exposed DSL を習得するコストを厭わない
- Flyway にこだわりがなく、Exposed Migration で完結させる
- スキーマ変更が異常に頻繁で、SchemaUtils の自動追従を活用したい
- テストの自動ロールバック性能を最大限活用したい

### 7.4 最終判断

```
Flyway を使いたい気持ちがある
  │
  ├─ Yes → jdbi (SqlObject) + Flyway
  │
  └─ No  → Exposed だけで良い (Flyway 併用はしない)
```

LifeQuest の今の状況では **jdbi (SqlObject) + Flyway** がベターだと思います。

ただし Exposed を選んでも後悔はしないレベルです。「Exposed 1.x で安定した」「JetBrains 公式」「Kotlin 体験が気持ちいい」という要素も確かにあります。
