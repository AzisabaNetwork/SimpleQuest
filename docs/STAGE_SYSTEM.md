# ステージシステム

Stage はクエスト内で段階的な進行管理を行う仕組みです。

## Stage の定義

YAML (`@namespace/stages/...`) による定義:

```yaml
title: '&aボス戦'
location:
  world: 'minecraft:overworld'
  x: 0
  y: 0
  z: 0
maxParties: 1
```

## アーキテクチャ

### StageLike インターフェース

```kotlin
interface StageLike {
    val key: String
    fun mount()
    fun unmount()
}
```

`Stage` と `Stage.Queue` の両方がこれを実装し、Party は任意の StageLike を保持できる。

### Stage インターフェース

```kotlin
interface Stage : StageLike {
    val active: Boolean
    val tasks: List<StageTask>
}
```

### StageTask

```kotlin
data class StageTask(
    val key: String,
    val description: String,
    val isComplete: Boolean = false,
)
```

`StageTask` はステージ内の個別タスクを表す。`isComplete` がすべて `true` になるとステージクリアとなる。

## mount / unmount

- `mount()`: パーティをステージに参加させる
- `unmount()`: パーティをステージから離脱させる
- 同時参加パーティ数は `maxParties` (デフォルト 1) で制限

## Queue (待機キュー)

```kotlin
class Queue(override val stage: Stage) : StageLike {
    val first: Party?      // キューの先頭
    val size: Int          // 待機数
    fun add(party: Party): Boolean   // true=即時mount, false=キュー追加
    fun remove(party: Party)
    fun indexOf(party: Party): Int   // キュー内の位置 (0-indexed)
}
```

空きがある場合は即時 mount、満員の場合はキュー追加。

## StageLike と Party の関係

```kotlin
interface Party {
    var stage: StageLike?   // null = ステージ未参加
}
```

- マウント中: `party.stage is Stage` → ステージ情報表示
- キュー待機中: `party.stage is Stage.Queue` → 待機位置表示

## コマンド

```shell
/simplequest stage mount <player> <stage>
/simplequest stage unmount <player> <stage>
```

> **実装状況**: 基本構造は実装済み。stage mount/unmount コマンドと高度なテレポート連携は今後の実装予定。

## スコアボード表示

QuestPanelGui で Party の stage 状態に応じて表示:

| party.stage の型 | 表示内容 |
|---|---|
| `Stage` | "ステージ: {title}" |
| `Stage.Queue` | "ステージキュー: {position}/{queueSize}" |
| null | 非表示 |
