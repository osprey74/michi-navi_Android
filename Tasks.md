# Tasks.md — Michi-Navi Android

## 実装済み (Completed)

### v0.1 — 基盤構築
- [x] Android プロジェクト初期セットアップ (Kotlin, Jetpack Compose, Material 3)
- [x] コアデータモデル定義 (RoadsideStation, Feature, PoiCategory, PoiItem, NearbyStation, AppSettings)
- [x] 道の駅 JSON データセット (600+ 駅) 組み込み
- [x] RoadsideStationRepository — JSON パース & 距離ソート
- [x] RoadsideStationService — 半径100km以内フィルタ & 走行方向フィルタ (±45°)
- [x] GeoUtils — Haversine距離計算, 方位角, 方角変換

### v0.2 — 地図 & 位置情報
- [x] Google Maps → MapLibre (OpenFreeMap) 移行 → ramani-compose → MapLibre SDK 直接利用に移行
- [x] LocationService — GPS (500ms/5m) + コンパスセンサー
- [x] MapViewModel — 統合ViewModel (位置, 設定, 地図状態)
- [x] 速度ベース自動ズーム (停車120km〜高速84km表示)
- [x] リアルタイム位置追従 & カメラ制御

### v0.3 — POI & Overpass API (削除済み)
- [x] ~~OverpassClient — Overpass API クエリ~~ → v0.5 で POI 機能削除
- [x] ~~PoiService — 5カテゴリ POI 取得~~ → v0.5 で POI 機能削除

### v0.4 — UI画面
- [x] MapScreen — メイン地図画面 (ズームボタン, 現在地ボタン, 設定ボタン)
- [x] StationDetailSheet — 道の駅詳細ボトムシート (特徴, 画像, URL)
- [x] SettingsScreen — 設定画面 (POI表示トグル, ズームボタン位置)
- [x] StationPickerScreen — 3階層駅選択 (都道府県 → 市区町村 → 駅)
- [x] SettingsRepository — DataStore による設定永続化

---

## 未実装 / 残タスク (TODO)

### 優先度: 高
- [x] ナビゲーション機能 — Google Maps 連携ナビ (StationDetailSheet「ナビ開始」ボタン)
- [x] 道の駅マーカー表示 — 地図上に道の駅 Symbol マーカー表示 (タップで詳細シート)
- [x] お気に入り機能 — DataStore 永続化, 詳細シートのハートボタン, 駅選択画面にお気に入りタブ
- [x] 到達リスト機能 — DataStore 永続化, 詳細シートの Beenhere ボタン, 駅選択画面に到達タブ
- [x] マーカー色分け — デフォルト:オレンジ LocationOn, お気に入り:赤 Favorite, 到達:青 Beenhere, お気に入り+到達:赤 Beenhere
- [x] 地図タイル切替 — 国土地理院 (淡色/標準/航空写真) + OpenFreeMap, 設定画面で選択
- [x] OpenFreeMap ベクタータイル対応 — MapLibre ベクタースタイルURL方式
- [x] 現在地に戻るボタン — 右下に配置, タップでズーム8, 位置未取得時はグレーアウト
- [x] iOS版準拠ボタン配置 — 設定:左上, 操作ボタン:右下
- [x] Google Maps タイル対応 — APIキー登録で Google Maps 地図タイル選択可能 (Map Tiles API セッション方式)
- [x] 現在地マーカー — 青丸 (白フチ付き) で現在地を地図上に表示
- [x] 走行中 UI 最適化 — 大きなボタン, 高コントラスト, 速度HUD, オートズーム, ヘディングアップ

### 優先度: 中
- [ ] オフラインサポート — 道の駅データのオフライン利用
- [ ] 検索機能 — 駅名・特徴によるフリーテキスト検索
- [ ] 通知機能 — 道の駅接近時のプッシュ通知
- [ ] ダークモード対応 — 夜間走行用テーマ切替
- [ ] 道の駅データ更新 — リモートからの JSON 更新メカニズム

### 優先度: 低
- [ ] ウィジェット — ホーム画面ウィジェット (最寄り駅表示)
- [ ] 訪問履歴 — 訪れた道の駅の記録
- [x] フォトアルバム — 道の駅ごと最大3枚の写真を保存・閲覧・削除 (iOS版準拠)
- [ ] テスト追加 — Unit テスト & UI テスト

### 技術的改善
- [x] 道の駅マーカー改善 — Material Icons (LocationOn/Favorite/Beenhere) によるアイコンフォント描画
- [ ] マーカークラスタリング — 広域表示時のマーカー集約
- [ ] DI 導入 — Hilt/Koin によるDI整理
- [ ] ProGuard / R8 設定 — リリースビルド最適化
- [ ] CI/CD — GitHub Actions によるビルド & リリース自動化

---

### バグ修正済み
- [x] ic_launcher_foreground.xml — android:fillColor 重複属性修正
- [x] MapScreen CameraPosition — rememberSaveable → remember に変更 (Bundle非対応型のクラッシュ修正)
- [x] MapLibre 実機クラッシュ修正 — ramani-compose → MapLibre SDK 直接利用に移行 (Style未ロード時のLocationComponent初期化クラッシュ回避)
- [x] 駅選択時カメラ位置バグ — factory内初期カメラを選択駅>現在地>デフォルトの優先順に修正
- [x] 道の駅マーカー表示不具合 — MapLibre GeoJSONソースの低ズーム描画問題を回避、Compose Canvas描画方式に移行

---

## 更新履歴
- 2026-03-29: 初版作成 — 実装済み機能の棚卸し & 残タスク整理
- 2026-03-29: お気に入り機能実装, バグ修正2件 (ic_launcher_foreground, CameraPosition)
- 2026-03-29: ramani-compose → MapLibre SDK 直接利用に移行 (実機クラッシュ修正), 道の駅写真表示修正 (Wikimedia直接URL変換)
- 2026-03-29: 地図タイル切替 (GSI淡色/標準/航空写真/OpenFreeMap), 現在地ボタン, iOS版準拠ボタン配置, 駅選択時カメラ移動修正
- 2026-03-29: Google Maps タイル対応 (APIキー+セッション方式), 現在地マーカー (青丸), APIキー設定UI改善
- 2026-03-29: 走行中UI最適化 (オートズーム, ヘディングアップ, ボタン大型化, 速度HUD)
- 2026-03-29: 道の駅マーカー修正 — MapLibre GeoJSONソース低ズーム描画問題をCompose Canvas描画で回避, 全1200駅表示対応
- 2026-03-30: POI機能削除, 到達リスト機能追加, マーカーをMaterial Iconsに変更 (状態別色分け), OFMベクタータイル対応, 広域/詳細ボタン撤去, 現在地ボタンzoom8化
- 2026-04-10: カントリーサイン詳細 — tourismUrl未設定時にGoogle検索ボタンを追加 (iOS版準拠)
