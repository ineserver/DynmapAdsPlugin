# DynmapAdsPlugin

Dynmap上に商業施設と広告マーカーを作成・管理するSpigot/Paperプラグインです。

## 機能

- **商業施設マーカー**: プレイヤーがDynmap上に自分の店舗を登録
- **Discord承認システム**: 商業施設の登録はDiscordで管理者が承認/却下
- **広告掲載機能**: 承認済み店舗を期間限定で広告として目立たせる
- **Vault連携**: 登録・広告に費用がかかる経済システム
- **自動期限切れ**: 広告期間終了後に自動で通常マーカーに戻る

## 依存プラグイン

- **Vault** (経済連携)
- **Dynmap** (マップマーカー)
- **DiscordSRV** (Discord連携)

## コマンド

| コマンド | 説明 |
|---------|------|
| `/mapmarker commercial <店名> <説明>` | 商業施設の登録申請 |
| `/mapmarker ads <店名> <期間(日)> [宣伝文句]` | 広告掲載開始 |
| `/mapmarker delete <店名>` | 店舗削除 |

## 権限

| 権限 | 説明 |
|------|------|
| `mapmarker.admin` | 他プレイヤーの店舗を管理可能 |

## 設定 (config.yml)

```yaml
discord:
  approval-channel-id: "チャンネルID"  # 承認用チャンネル
  ads-channel-id: "チャンネルID"       # 広告通知チャンネル

economy:
  commercial-fee: 10000      # 商業施設登録料
  ads-fee-per-day: 30000     # 広告料/日
  currency-name: "ine"       # 通貨名

dynmap:
  commercial-marker-set: "commercial"  # 商業施設マーカーセットID
  ads-marker-set: "ads"                # 広告マーカーセットID
```

## ワークフロー

### 商業施設登録
1. プレイヤーが `/mapmarker commercial 店名 説明` を実行
2. 登録料が引かれ、Discordに承認リクエストが送信
3. 管理者がDiscordで ✅ または ❌ をクリック
4. 承認されるとDynmapにマーカーが作成される

### 広告掲載
1. 承認済み店舗のオーナーが `/mapmarker ads 店名 日数 PR文` を実行
2. 広告料が引かれ、マーカーが広告セットに移動
3. Discordに広告開始通知が送信
4. 期間終了後、自動的に通常マーカーに戻る

## Dynmapマーカー HTML構造

マーカーのポップアップ表示はCSSでスタイリング可能なHTML構造で生成されます。

### 商業施設マーカー

```html
<div class="shop-entry">
  <div class="shop-name">店舗名</div>
  <div class="shop-description">店舗の説明</div>
  <div class="shop-owner">オーナー名</div>
</div>
```

### 広告マーカー

```html
<div class="shop-entry shop-ads">
  <div class="shop-name">店舗名</div>
  <div class="shop-description">店舗の説明</div>
  <div class="shop-owner">オーナー名</div>
  <div class="shop-pr">PR文（宣伝文句）</div>
</div>
```

### CSSクラス

| クラス名 | 説明 |
|---------|------|
| `.shop-entry` | マーカーポップアップのルート要素 |
| `.shop-ads` | 広告マーカーに追加されるクラス |
| `.shop-name` | 店舗名 |
| `.shop-description` | 店舗説明 |
| `.shop-owner` | オーナー名 |
| `.shop-pr` | 広告のPR文（広告時のみ表示） |

### CSSカスタマイズ例

Dynmapの `custom.css` に追加：

```css
.shop-entry {
  font-family: 'Noto Sans JP', sans-serif;
  padding: 10px;
}

.shop-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.shop-description {
  margin: 5px 0;
  color: #666;
}

.shop-owner {
  font-size: 12px;
  color: #999;
}

.shop-entry.shop-ads {
  background: linear-gradient(135deg, #fff9c4, #fff176);
  border: 2px solid #ffc107;
}

.shop-pr {
  margin-top: 10px;
  padding: 8px;
  background: #fffde7;
  border-left: 3px solid #ff9800;
  font-style: italic;
}
```

## ビルド

```bash
mvn clean package
```

生成されるJAR: `target/DynmapAdsPlugin-1.0.0.jar`

## ライセンス

MIT License
