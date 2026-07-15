## 仕様検討
### データ保持について
- 設定: yaml
	- MMの書式に似せたいかも
- データベース
	- 永続: MySQL(MariaDB)
	- メッセージング: Redis

- yaml -> MariaDBへの起動時 / リロード時吸い上げ必須かな?

### 基本思想
- Loreベースでの表示
	- LoreEditorがメインで関わってきそうかも?
- 
### アクション
(`List<Action>`みたいに持つ想定)
- Minecraftのアイテム付与
- MythicMobsのアイテム付与
- PvELevelのExp付与
- コマンド実行

### クエスト
- 名前
- タイプ
	- 一般
	- デイリー
	- ストーリー
	- イベント
- アイコン
	- Material
	- CustomModelData(Optional)
- 依頼人名(Optionalかも)
- 受注可能回数
	- Weekly
	- Monthly
	- Yearly
	- Lifetime
- 死亡可能回数
- 完了条件
	- アイテムを集める
	- etc...
- 受注条件
	- PvELevel
	- 前提クエスト
	- 特定の権限(LuckPerms)
	- パーティーモードのみ
	- などなど...

