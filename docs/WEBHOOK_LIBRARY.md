# Webhook ライブラリ比較

Discord Webhook 経由でのエラー通知機能に使用するライブラリの比較。

## 候補

| ライブラリ | 種別 | Stars | ライセンス | Kotlin対応 |
|---|---|---|---|---|
| **ktor-client** | HTTP クライアント | 15k+ | Apache-2.0 | ネイティブ |
| **JDA** | Discord Bot ライブラリ | 4.6k | Apache-2.0 | Java (Kotlin互換) |
| **Kord** | Discord Bot ライブラリ | 1.6k | Apache-2.0 | ネイティブ |

## 比較

| 観点 | ktor-client | JDA | Kord |
|---|---|---|---|
| **用途に合っているか** | ◎ 単純な HTTP POST がしたいだけ | △ Bot 用。Webhook 送信は可能だが重い | △ 同上 |
| **依存サイズ** | 小 (ktor-client-core + cio) | 大 (5MB+) | 中 (2MB+) |
| **セットアップ工数** | ◎ Webhook URL に POST するだけ | △ Bot トークン + Gateway 接続が必要 | △ 同上 |
| **非同期対応** | ◎ コルーチンネイティブ | ○ (CompletableFuture) | ◎ コルーチンネイティブ |
| **Paperとの相性** | ◎ Paper の依存にバッティングなし | ○ (Java, 問題少ない) | ○ |
| **Webhook 特化** | ◎ REST API を叩くだけ。余計な機能なし | △ Bot が前提 | △ Bot が前提 |
| **メンテナンス** | JetBrains 公式 | コミュニティ (活発) | コミュニティ (活発) |

## 推奨: ktor-client

**理由:**

- Webhook 送信は単なる `POST /api/webhooks/{id}/{token}` の HTTP リクエスト
- JDA/Kord は Discord Bot ライブラリであり、Gateway 接続・イベントハンドリング・Bot トークン管理等のオーバーヘッドがある
- 必要なのは「JSON body を POST する」だけ。ktor-client で十分
- Paper 1.21 の依存関係とバッティングしない
- コルーチンネイティブで BukkitScheduler との統合も容易

```kotlin
// ktor-client での Webhook 送信例
suspend fun sendWebhook(url: String, message: String) {
    val client = HttpClient(CIO)
    client.post(url) {
        contentType(ContentType.Application.Json)
        setBody(webhookJson(message))
    }
    client.close()
}
```

## 結論

| 用途 | 推奨 |
|---|---|
| Webhook 通知のみ | **ktor-client** |
| 将来 Bot 機能も追加予定 | Kord (Kotlinネイティブ) |
| エコシステムに乗る | JDA (最も普及) |

今回は Webhook 通知のみなので **ktor-client** を採用。
