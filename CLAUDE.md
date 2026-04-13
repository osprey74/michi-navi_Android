# CLAUDE.md — Michi-Navi Android

このファイルは Claude Code (claude.ai/code) がこのリポジトリで作業する際のプロジェクト固有の指示を提供します。ワークスペース共通の指示は `/mnt/g/dev/CLAUDE.md` を参照してください。

## Git ワークフロー

### コミット & プッシュの自動化

コミットを依頼されたら、コミット後に `git push origin <current-branch>` まで自動で実行してよい（毎回の確認不要）。これは durable authorization として事前承認されています。

ただし以下の場合は push 前に必ずユーザーに確認すること:

- `--force` / `--force-with-lease` を伴う push
- 現在のブランチが追跡リモートブランチを持っていない場合（新規ブランチの初回 push を含む）
- pre-commit / pre-push フックが失敗した場合
- `main` 以外のブランチへの push で、意図が不明な場合
- 直前のコミットが `git revert` や履歴改変を含む場合

### コミット前チェックリスト

1. `Tasks.md` を確認し、今回の変更内容を「実装済み」セクションまたは「バグ修正済み」セクションに追記する
2. 更新履歴セクションに当日の日付で1行追記する（`YYYY-MM-DD: <変更概要>`）
3. ビルド成果物 (`app/build/`, `.gradle/`, `.idea/` の大部分) はステージしない。ソース・リソース・ドキュメントのみをコミット対象とする

### コミットメッセージ

- 英語で簡潔に（既存コミット履歴のスタイルに合わせる）
- タイトルは動詞始まり、70文字以内
- 本文には「なぜ」を書く（差分を見れば分かる「何を」ではなく）

## バージョン管理

アプリのバージョンは `app/build.gradle.kts` の `versionName` / `versionCode` が唯一の真実です。以下の箇所はビルド時に自動追従するため、手動更新不要:

- 設定画面のバージョン表記 (`SettingsScreen.kt` — `BuildConfig.VERSION_NAME` から取得)

リリース時に更新が必要な箇所:

- `app/build.gradle.kts` の `versionName` と `versionCode`
- `Tasks.md` の更新履歴
