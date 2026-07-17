# サーバーセットアップ手順

## 目次

1. [動作環境](#動作環境)
2. [依存インフラの準備](#依存インフラの準備)
   - [MariaDB のセットアップ](#mariadb-のセットアップ)
   - [Redis のセットアップ](#redis-のセットアップ)
3. [プラグインのビルド](#プラグインのビルド)
4. [サーバーへの導入](#サーバーへの導入)
5. [設定](#設定)
6. [クエスト定義の配置](#クエスト定義の配置)
7. [動作確認](#動作確認)

---

## 動作環境

| 項目 | 要件 |
|---|---|
| サーバーソフトウェア | **Paper 1.21.11** 以上 |
| Java | **JDK 21** |
| データベース | **MariaDB 10.6+** （または MySQL 8.0+） |
| キャッシュ / PubSub | **Redis 7.0+** |
| 依存プラグイン | **Kunectron** v1.0.0-beta.13+（GUI 表示に必須） |

> [!WARNING]
> **Java 21** 必須です。Java 17 以下では起動しません。
>
> Paper 以外のサーバー（Spigot, CraftBukkit）は動作保証外です。Paper API の Brigadier コマンドシステムを使用しています。

---

## 依存インフラの準備

### MariaDB のセットアップ

#### 1. MariaDB のインストール

```bash
# Ubuntu / Debian
sudo apt update
sudo apt install mariadb-server

# macOS (Homebrew)
brew install mariadb
brew services start mariadb
```

#### 2. データベースとユーザーの作成

```sql
-- MariaDB に root で接続
sudo mariadb

-- データベース作成
CREATE DATABASE lifequest CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ユーザー作成（パスワードは適宜変更）
CREATE USER 'lifequest'@'localhost' IDENTIFIED BY 'your_secure_password';
CREATE USER 'lifequest'@'%' IDENTIFIED BY 'your_secure_password';  -- リモート接続用

-- 権限付与
GRANT ALL PRIVILEGES ON lifequest.* TO 'lifequest'@'localhost';
GRANT ALL PRIVILEGES ON lifequest.* TO 'lifequest'@'%';

FLUSH PRIVILEGES;
EXIT;
```

#### 3. リモート接続の許可（必要な場合）

```bash
# /etc/mysql/mariadb.conf.d/50-server.cnf を編集
sudo vi /etc/mysql/mariadb.conf.d/50-server.cnf

# bind-address をコメントアウト または 0.0.0.0 に変更
# bind-address = 127.0.0.1
bind-address = 0.0.0.0

# MariaDB 再起動
sudo systemctl restart mariadb
```

> [!TIP]
> 同一マシン内で完結する場合は `bind-address = 127.0.0.1` のままでも問題ありません。

### Redis のセットアップ

#### 1. Redis のインストール

```bash
# Ubuntu / Debian
sudo apt update
sudo apt install redis-server

# macOS (Homebrew)
brew install redis
brew services start redis
```

#### 2. パスワード認証の設定（推奨）

```bash
# /etc/redis/redis.conf を編集
sudo vi /etc/redis/redis.conf

# 以下の行を探してパスワードを設定
# requirepass foobared
requirepass your_redis_password
```

```bash
# Redis 再起動
sudo systemctl restart redis-server
```

#### 3. 動作確認

```bash
redis-cli
127.0.0.1:6379> AUTH your_redis_password
OK
127.0.0.1:6379> PING
PONG
```

---

## プラグインのビルド

```bash
# リポジトリをクローン
git clone <repository-url>
cd LifeQuest

# shadowJar（依存ライブラリ同梱の fat JAR）をビルド
./gradlew shadowJar

# 生成物の確認
ls -lh build/libs/LifeQuest-*.jar
# → build/libs/LifeQuest-1.0.0-SNAPSHOT.jar
```

> `./gradlew build` でも shadowJar が自動実行されるように設定されています。

---

## サーバーへの導入

### 1. プラグインの配置

```bash
# LifeQuest 本体
cp build/libs/LifeQuest-1.0.0-SNAPSHOT.jar /path/to/server/plugins/

# Kunectron（GUI フレームワーク・必須）
# https://github.com/tksimeji/kunectron からダウンロードして plugins/ に配置
```

### 2. サーバー起動

```bash
cd /path/to/server
java -jar paper-1.21.11-xxx.jar
```

初回起動時に `plugins/LifeQuest/config.yml` が自動生成されます。

---

## 設定

`plugins/LifeQuest/config.yml` を編集します。

### 最小構成（シングルサーバー）

```yaml
database:
  host: "localhost"
  port: 3306
  name: "lifequest"
  user: "lifequest"
  password: "your_secure_password"

redis:
  host: "localhost"
  port: 6379
  password: "your_redis_password"
```

### 設定項目の詳細

全設定項目の詳細は [config.yml 設定リファレンス](CONFIGURATION.md) を参照してください。

**設定変更後は必ずリロード** してください:

```
/lifequest reload
```

---

## クエスト定義の配置

### ディレクトリ構成

```
plugins/LifeQuest/
├── config.yml
└── @namespace/         ← 任意の名前空間ディレクトリ
    └── types/          ← クエスト定義 YAML を格納
        ├── quest_a.yml
        └── quest_b.yml
```

> [!NOTE]
> デフォルトの名前空間は `@lq` です。AGENTS.md の YAML 記法例を参照してください。

### クエスト定義 YAML の例

```yaml
WolfSlayer:
  Title: "&cWolf Slayer"
  Description:
    - "&7森に出没する狼を討伐せよ"
    - "&70/10"
  Icon: "BONE:0"
  Category: "lq:general"
  Location: "world,100,64,200"
  Options:
    MaxParty: "1-4"
    Limits:
      Daily: 5
    DeathLimit: 3
  Requirements:
    PvELevel: 5
  Objectives:
    KillWolf: 10
  Actions:
    OnFirstComplete:
      - Type: Command
        Params: "give % minecraft:diamond 5"
    OnComplete:
      - Type: Item
        Params: "minecraft:emerald,3"
      - Type: PvELevel
        Params: "50"
```

> [!WARNING]
> YAML ファイルの文字コードは **UTF-8** で保存してください。

詳細な記法は [クエスト定義の作成](QUEST_CREATION.md) を参照してください。

### リロード

```bash
/lifequest reload
```

---

## 動作確認

### 1. プラグインのロード確認

サーバーコンソールに以下が表示されれば起動成功です:

```
[LifeQuest] Flyway migration completed: X migration(s) applied.
[LifeQuest] LifeQuest enabled.
```

### 2. コマンドの確認

```bash
# バージョン表示
/lifequest

# クエスト GUI を開く
/lifequest quest
```

### 3. クエスト定義の確認

```bash
/lifequest reload
```

エラーが表示されなければ設定完了です。

### トラブルシューティング

| 症状 | 考えられる原因 | 確認事項 |
|---|---|---|
| `LifeQuest enabled.` が表示されない | Java バージョン不一致 / Paper 以外 | `java -version` で 21 確認 |
| DB 接続エラー | MariaDB 未起動 / 認証情報誤り | `config.yml` の database 設定を確認 |
| `Failed to run database migrations` | DB ユーザーの権限不足 | `GRANT ALL` が正しいか確認 |
| Redis 接続エラー | Redis 未起動 / パスワード誤り | `redis-cli PING` で疎通確認 |
| GUI が開かない | Kunectron 未導入 | `plugins/Kunectron-*.jar` の存在確認 |
| YAML が読み込まれない | 拡張子が `.yml` でない / ディレクトリ構造誤り | `plugins/LifeQuest/@namespace/types/*.yml` になっているか確認 |
