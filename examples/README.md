# LifeQuest クエスト記法サンプル

LifeQuest のクエスト YAML 記法のサンプルです。

## 収録クエスト

| ファイル | クエスト | 特徴 |
|---|---|---|
| `wolf_slayer.yml` | オオカミ討伐 | シンプルな戦闘クエスト (PvELevel条件・日替わり制限・Guide) |
| `supply_procurement.yml` | 備蓄物資調達 | 複合クエスト (エリア侵入解放・課金受注・アイテム納品・初回限定報酬) |

## 記法の特徴

- `Icon: "MATERIAL:CMD"` — アイコンは `Material:CustomModelData` の簡略形
- `Location: "world,x,y,z"` — ロケーションはカンマ区切り文字列
- `Options.MaxParty: "1-4"` — `min-max` 形式
- `Objectives.KillWolf: 10` — 単純な `Map<String, Int>`
- `Actions.OnComplete: - Type: Item, Params: "diamond,3"` — Type/Params 構造
- `Guides.Location: "x,y,z"` — ワールド省略時は quest location から継承
- `Scripts.OnStart+20:` — トリガー名 + 遅延 tick

詳細は `docs/QUEST_FORMAT_DESIGN.md` または `AGENTS.md` を参照。
