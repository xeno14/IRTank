IRTank
======

![overview](https://raw.githubusercontent.com/wiki/xeno14/IRTank/images/overview.png)


C91頒布「日曜プログラマでもできる！ラジコン＆パンツアー」の
ソースコード。

## NECフォーマット対応音声信号作成ツール

**使い方**

```
javac IRSoundNEC.java && java IRSoundNEC (32bit整数値)
```

引数の例
- 10進数 `1234`
- 16進数 `0xff0000ff`
- 2進数 `0b11111111111111111111111111111111`

実行すると`ir.wav`が作成されます。
下図のように、極性を逆にした赤外線LEDプラグを作成し、適当なソフトでwaveファイルを再生すると、
引数に指定した32bit整数値に対応するNECフォーマットの赤外線リモコン信号が送信されます。

![irplug](https://raw.githubusercontent.com/wiki/xeno14/IRTank/images/irplug.jpg)


## [IRTank-Android](https://github.com/xeno14/IRTank-Android)
操作用のAndroidアプリ。

## [IRTank-Arduino](https://github.com/xeno14/IRTank-Arduino)
Arduinoスケッチ。

## 回路図

### IRTank.fzz
[fritzing](http://fritzing.org/home/)で書いた回路図

![circuit](https://raw.githubusercontent.com/wiki/xeno14/IRTank/images/circuit.png)

