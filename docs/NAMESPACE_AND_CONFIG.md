# 名前空間・設定システム

## Namespace の概念

SimpleQuest はすべてのオブジェクト (QuestType, QuestCategory, Stage) を `namespace:key` の**完全修飾名 (FQN)** で管理する。

- **namespace**: `@名前空間` ディレクトリ名から `@` を除いた部分
- **key**: 名前空間ディレクトリ内の相対パスから拡張子を除いたもの (スラッシュ区切り)

### 使用可能文字

```
数字: 0-9
英小文字: a-z
下線: _
ハイフン: -
```

## ファイル構成

```
plugins/SimpleQuest/
├── config.yml                   # プラグイン設定
└── @namespace_name/             # 名前空間ディレクトリ (@ 必須)
    ├── categories/              # クエストカテゴリ
    │   ├── foo.yml              → namespace:foo
    │   └── sub/bar.yml          → namespace:sub/bar
    ├── types/                   # クエストタイプ
    │   ├── example.yml          → namespace:example
    │   └── event/boss.yml       → namespace:event/boss
    └── stages/                  # ステージ
        ├── boss_arena.yml       → namespace:boss_arena
        └── tower/floor1.yml     → namespace:tower/floor1
```

### スキャンルール (Loader)

1. `plugins/SimpleQuest/` 直下の `@^([a-zA-Z0-9_-]+)$` にマッチするディレクトリを列挙
2. 各ディレクトリ内の `categories/`, `types/`, `stages/` サブディレクトリを走査
3. 以下の条件を満たすファイルを再帰的に検出:
   - ファイル名の拡張子を除いた部分が `^[a-zA-Z0-9_-]+$` にマッチ
   - 拡張子は `yml`
4. ファイルの相対パスから key を生成 (拡張子除去)

### key の生成

```kotlin
// dir  = plugins/SimpleQuest/@mynamespace/types
// file = plugins/SimpleQuest/@mynamespace/types/event/boss.yml
// result = event/boss
private fun key(dir: File, file: File): String {
    val key = dir.toURI().relativize(file.toURI()).path
    return key.substring(0, key.lastIndexOf('.'))
}
```

## リロード

`/simplequest reload` または Loader.load() の実行時に以下の処理が行われる:

1. 実行中の全 Quest を終了
2. 全レジストリをクリア（ビルドインカテゴリを除く）
3. 名前空間ディレクトリを再スキャンし、全エントリを再登録
4. config.yml も再読み込み
5. DB 接続再確立（必要な場合）

## ビルドインカテゴリ

リロード時に削除されないデフォルトカテゴリ。

| Key | 表示名 | 説明 |
|---|---|---|
| `lq:general` | General | 一般的なクエスト |
| `lq:daily` | Daily | デイリークエスト (抽選対象) |
| `lq:story` | Story | ストーリークエスト |
| `lq:event` | Event | イベントクエスト |

## config.yml

```yaml
# データベース設定
database:
  host: 'localhost'
  port: 3306
  name: 'simplequest'
  user: 'root'
  password: ''

redis:
  host: 'localhost'
  port: 6379
  password: ''

# パーティーの最大サイズ
maxPartySize: 8

# パーティー招待の有効期限 (tick数)
partyInviteLimit: 1200

# クエスト終了後にテレポートする場所 (任意)
lobby:
  world: 'minecraft:overworld'
  x: 0
  y: 0
  z: 0
  yaw: 0    # 任意
  pitch: 0  # 任意

# クエストパネルの設定
panel:
  title: '&dSimpleQuest'
  footer: '&7いますぐ &eazisaba.net&7 で遊べ！'
```

## 名前空間の設計指針

- デフォルト名前空間として `lq` (simplequest) を使用
- ビルドインカテゴリに `lq:event` を追加 (イベントクエスト用)
- YAML 書式は MythicMobs ライクなスタイルを採用予定 (要検討)
