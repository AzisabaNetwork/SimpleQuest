# スクリプトシステム

Script はクエストのライフサイクルイベントに応じてコマンドを自動実行する仕組み。[アクションシステム](QUEST_SYSTEM.md#actionsアクションシステム) と併用する。

## 設計方針

- 同じ YAML 上で `actions:` と `scripts:` を併用可能
- `scripts:` はより簡易的な即時実行 / 遅延実行に使い、`actions:` は構造化された報酬に使う

## Script インターフェース

```kotlin
interface Script {
    val trigger: Trigger   // 発火トリガー
    val delay: Long        // 遅延 (tick)
    val commands: List<String>  // 実行コマンドリスト
    fun run(quest: Quest)
}
```

## トリガー (Trigger)

| トリガー | 発火タイミング |
|---|---|
| `START` | クエスト開始直後 (テレポート後) |
| `END` | 全理由のクエスト終了時 (`end()` 内、COMPLETE/CANCEL より先) |
| `COMPLETE` | 全要件達成による終了時 |
| `CANCEL` | 未達成での終了時 |
| `UNKNOWN` | パース不能なトリガー名 (コマンドは無視される) |

### 発火順序

```
START → [クエスト進行] → END → COMPLETE  (達成時)
                        → END → CANCEL    (未達成時)
```

## 遅延実行

トリガー名に `+tick数` を付加することで、発火タイミングから指定 tick 後にコマンドを実行できる。

```yaml
scripts:
  start:           # 即時実行
    - 'command1'
  start+20:        # 1秒後 (20 tick = 1秒)
    - 'command2'
```

## 正規表現パース

```kotlin
private val nameRegex = Regex("(${Trigger.entries.joinToString("|") { it.name.lowercase() }})\\+(\\d+)?")
```

## ScriptImpl の実行

```kotlin
class ScriptImpl(
    override val trigger: Script.Trigger,
    override val delay: Long,
    override val commands: List<String>
): Script {
    override fun run(quest: Quest) {
        Bukkit.getScheduler().runTaskLater(Quem.plugin, Runnable {
            for (command in commands) {
                if (command.startsWith(':')) {
                    // 拡張構文: 全プレイヤーに対して繰り返し
                    for (player in quest.players) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            command.substring(1).replace("%", player.name))
                    }
                } else {
                    // 通常: コンソールで1回実行
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                }
            }
        }, delay)
    }
}
```

## 拡張コマンド構文

### 通常コマンド (先頭 `:` なし)

```yaml
scripts:
  start:
    - 'say Quest started'  # コンソールで1回実行
```

### 拡張構文 (先頭 `:`)

```yaml
scripts:
  complete:
    - ':simplequest grant % lq:example'
```

- 先頭 `:` で開始された場合、`quest.players` の各プレイヤーごとにコマンドを繰り返し実行
- `%` プレースホルダーが各プレイヤー名に置換される

## YAML 定義例

```yaml
scripts:
  start:
    - 'command1'
  start+20:
    - 'command2'
  end:
    - 'broadcast Quest ended'
  complete:
    - ':say % completed the quest!'
    - ':simplequest grant % namespace:reward'
  cancel:
    - ':say % failed the quest...'
```

## アクションシステムとの共存

SimpleQuest では以下の使い分けを想定:

| 用途 | 推奨方式 |
|---|---|
| 簡易的なコマンド実行 | `scripts:` |
| アイテム付与・経験値付与 | `actions:` |
| 初回完了時のみの処理 | `actions.on-first-complete` |
| プレイヤー単位のループ処理 | `scripts:` (`:` 拡張構文) |
