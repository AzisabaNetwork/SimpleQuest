# ステージシステム

Stage は**同時に挑戦できるパーティ数を制限**するための仕組み。主にボス戦などリソース競合を避けたい場面で使用する。

## Stage の定義

YAML (`@namespace/stages/...)` による定義:

```yaml
title: '&aボス戦'
location:
  world: 'minecraft:overworld'
  x: 0
  y: 0
  z: 0
unmountLocation:        # 任意
  world: 'minecraft:overworld'
  x: 0
  y: 0
  z: 0
maxParties: 1           # 任意 (default: 1)
```

## アーキテクチャ

### StageLike インターフェース

```kotlin
interface StageLike {
    val stage: Stage
}
```

`Stage` と `Stage.Queue` の両方がこれを実装し、Party は任意の StageLike を保持できる。

### Stage クラス

```kotlin
class Stage(override val key: Key, private val data: Stage): StageLike, Keyed {
    val title: Component
    val location: Location
    val unmountLocation: Location?   // null の場合は元の位置に戻す
    val maxParties: Int              // 同時収容上限 (default: 1)
    val queue: Queue                 // 待機キュー
    val size: Int                    // 現在収容中のパーティ数
}
```

### Queue クラス (内部クラス)

```kotlin
class Queue(override val stage: Stage): StageLike {
    val first: Party?      // キューの先頭
    val size: Int          // 待機数
    fun add(party: Party): Boolean   // true=即時mount, false=キュー追加
    fun remove(party: Party)
    fun indexOf(party: Party): Int   // キュー内の位置 (0-indexed)
}
```

## Mount / Unmount の動作

### Mount 処理

```kotlin
fun mount(party: Party) {
    // 既にマウント済み → 例外
    // 空きがない → UnsupportedOperationException
    parties.add(party)
    party.stage = this
    party.forEach {
        originalLocations[it] = it.location  // 現在地を保存
        it.teleport(location)                // Stage 位置へ TP
    }
}
```

### Queue からの Mount

```kotlin
class Queue {
    fun add(party: Party): Boolean {
        if (stage.parties.size < stage.maxParties) {
            stage.mount(party)   // 空きがある → 即時マウント
            return true
        }
        // 満員 → キューに追加
        set.add(party)
        party.stage = this       // 待機中は stage が Queue に変わる
        return false
    }
}
```

### Unmount 処理

```kotlin
fun unmount(party: Party) {
    party.stage = null
    party.forEach {
        // unmountLocation 有 → そこへ、無 → 元の位置へ
        it.teleport(if (unmountLocation == null) originalLocations[it]!! else unmountLocation!!)
        originalLocations.remove(it)
    }
    parties.remove(party)
    queue.first?.let { mount(it) }   // キュー先頭があれば自動マウント
}
```

### キュー先頭の自動マウント

Unmount 時に `queue.first` (キュー先頭の Party) があれば自動的に `mount()` を呼ぶ。これにより満員→解除→自動補充の流れが実現される。

## StageLike と Party の関係

```kotlin
interface Party {
    var stage: StageLike?   // null = ステージ未参加
}

// マウント中:
party.stage is Stage        → stage.title などでステージ情報を表示

// キュー待機中:
party.stage is Stage.Queue  → indexOf(party) で待機位置を表示
```

## コマンド

```shell
/simplequest stage mount <player> <stage>
/simplequest stage unmount <player> <stage>
```

詳細は [COMMANDS.md](COMMANDS.md) 参照。

## スコアボード表示

QuestPanelGui 相当の UI で Party の stage 状態に応じて以下のように表示:

| party.stage の型 | 表示内容 |
|---|---|
| `Stage` | "ステージ: {stage.title}" |
| `Stage.Queue` | "ステージキュー: {position}/{queueSize}" |
| null | 非表示 |
