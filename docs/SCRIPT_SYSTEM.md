# スクリプトシステム

Script はクエストのライフサイクルイベントに応じてコマンドを自動実行する仕組み。[アクションシステム](QUEST_SYSTEM.md#actionsアクションシステム) と併用する。

## 設計方針

- 同じ YAML 上で `actions:` と `scripts:` を併用可能
- `scripts:` はより簡易的な即時実行 / 遅延実行に使い、`actions:` は構造化された報酬に使う

## Script データクラス

```kotlin
// domain/script/Script.kt
data class Script(
    val trigger: Trigger,
    val delay: Long = 0L,
    val commands: List<String> = emptyList(),
) {
    enum class Trigger {
        START,
        END,
        COMPLETE,
        CANCEL,
    }
}
```

`Script` は YAML から直接デシリアライズされる data class。実行動作は `BukkitScriptRunner` (インフラ層) が担う。

## ScriptRunner インターフェース

```kotlin
// domain/script/port/ScriptRunner.kt
interface ScriptRunner {
    fun run(script: Script, playerIds: List<String>)
}
```

## トリガー (Trigger)

| トリガー | 発火タイミング |
|---|---|
| `START` | クエスト開始直後 |
| `END` | 全理由のクエスト終了時 (`end()` 内、COMPLETE/CANCEL より先) |
| `COMPLETE` | 全要件達成による終了時 |
| `CANCEL` | 未達成での終了時 |

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

## BukkitScriptRunner の実装

```kotlin
// infrastructure/bukkit/BukkitScriptRunner.kt
@Singleton
class BukkitScriptRunner
    @Inject constructor(private val plugin: Plugin) : ScriptRunner {
    
    override fun run(script: Script, playerIds: List<String>) {
        val runnable = Runnable { execute(script, playerIds) }
        if (script.delay > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, script.delay)
        } else {
            runnable.run()
        }
    }
    
    private fun execute(script: Script, playerIds: List<String>) {
        for (command in script.commands) {
            if (command.startsWith(":")) {
                // 拡張構文: 全プレイヤーに対して繰り返し
                for (playerId in playerIds) {
                    val player = Bukkit.getPlayer(UUID.fromString(playerId))
                    if (player != null) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            command.substring(1).replace("%", player.name))
                    }
                }
            } else {
                // 通常: コンソールで1回実行
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            }
        }
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

- 先頭 `:` で開始された場合、全プレイヤーごとにコマンドを繰り返し実行
- `%` プレースホルダーが各プレイヤー名に置換される
- `%player%` も同様に置換される（BukkitScriptRunner では `%` のみ対応）

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
