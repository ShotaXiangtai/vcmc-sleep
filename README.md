# vcmc-sleep

Folia 対応の Minecraft プラグイン。過半数のプレイヤーがベッドで寝たら夜をスキップします。

## ダウンロード

[Releases](../../releases/latest) から最新の `vcmc-sleep-*.jar` をダウンロードしてください。

## 必要環境

- Folia 26.1 以上
- Java 21 以上

## インストール

`vcmc-sleep-*.jar` をサーバーの `plugins/` フォルダに置いて再起動するだけです。

## 設定 (config.yml)

```yaml
# 就寝が必要なプレイヤーの割合 (%)
sleep-percentage: 50.0

# スペクテーターをカウントから除外するか
exclude-spectators: true

# バニラの就寝スキップを無効化するか
disable-vanilla-sleep: true
```

## コマンド

| コマンド | 説明 | 権限 |
|---|---|---|
| `/vcmcsleep reload` | 設定をリロード | `vcmcsleep.reload` |

エイリアス: `/vsleep`

## ライセンス

Public Domain ([Unlicense](UNLICENSE))
