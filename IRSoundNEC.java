
import java.util.Arrays;
import javax.sound.sampled.*;
import java.io.*;

class IRSoundNEC {
  public static final int SAMPLE_RATE = 44100;      // 44.1kHz
  public static final int IR_FREQ     = 19000;      // 19kHz
  public static final double DT = 1. / SAMPLE_RATE;

  // Parameters for WAVE
  // 16bit stereo
  public static final int WAVE_BITS = 16;
  public static final int WAVE_BYTES = WAVE_BITS / 8;
  public static final int STEREO = 2;

  // NEC format
  public static final int BITS       =    32;  // micro sec
  public static final int HDR_MARK   =  9000;  // micro sec
  public static final int HDR_SPACE  =  4500;  // micro sec
  public static final int BIT_MARK   =   560;  // micro sec
  public static final int ONE_SPACE  =  1690;  // micro sec
  public static final int ZERO_SPACE =   560;  // micro sec

  int leader_offset = 0;

  // 音声信号のバッファ
  byte[] hdr_mark_arr;    // リーダー部
  byte[] hdr_space_arr;   // リーダー部（空白）
  byte[] bit_mark_arr;    // データ部HIGH
  byte[] one_space_buf;   // データ部LOW(1)
  byte[] zero_space_buf;  // データ部LOW(0)
  byte[] buffer;          // 上記の配列の中身を組み合わせて信号にする

  public IRSoundNEC() {
    hdr_mark_arr   = new byte[(int)(HDR_MARK   * STEREO * WAVE_BYTES * SAMPLE_RATE / 1e6)];
    hdr_space_arr  = new byte[(int)(HDR_SPACE  * STEREO * WAVE_BYTES * SAMPLE_RATE / 1e6)];  // always 0
    bit_mark_arr   = new byte[(int)(BIT_MARK   * STEREO * WAVE_BYTES * SAMPLE_RATE / 1e6)];
    one_space_buf  = new byte[(int)(ONE_SPACE  * STEREO * WAVE_BYTES * SAMPLE_RATE / 1e6)];  // always 0
    zero_space_buf = new byte[(int)(ZERO_SPACE * STEREO * WAVE_BYTES * SAMPLE_RATE / 1e6)];  // always 0

    // 108ms分の大きさを確保
    buffer = new byte[WAVE_BYTES * STEREO * SAMPLE_RATE * 108 / 1000];

    // LEDを点灯させる部分に波形を設定
    fillWave(hdr_mark_arr, hdr_mark_arr.length);
    fillWave(bit_mark_arr, bit_mark_arr.length);
  }

  /**
   * バッファにキャリア周波数の正弦波を入れる
   *
   * @param buf
   * @param length
   */
  public void fillWave(byte[] buf, int length) {
    final int sample_length = length / STEREO / WAVE_BYTES;
    for (int i=0; i < sample_length; i++) {
      double t = (double)i / SAMPLE_RATE;
      double y = Math.sin(2*Math.PI*IR_FREQ*t);  // [-1, 1]
      short v = (short)(Short.MAX_VALUE * Math.sin(2*Math.PI* IR_FREQ *t)); // 正弦波

      // STEREO=2, WAVE_BYTES=2を仮定
      int index = i * STEREO * WAVE_BYTES;

      // little endian
      // left
      buf[index]     = (byte)(v & 0xff);          // 下位8bit
      buf[index + 1] = (byte)((v >>> 8 ) & 0xff); // 上位8bit
      // // right
      buf[index + 2] = (byte)(-buf[index]);
      buf[index + 3] = (byte)(-buf[index + 1]);
    }
  }

  /**
   * srcの内容でdstを埋める
   */
  public int fillArray(byte[] src, byte[] dst, int dst_offset) {
    System.arraycopy(src, 0, dst, dst_offset, src.length);
    return dst_offset + src.length;
  }

  /**
   * 32bit整数値に対応する赤外線信号の音声を作る
   * 
   * @param val 32bit整数
   * @return バッファのサイズ
   */
  public int setValue(int val) {
    int offset = 0;

    // リーダー部の書き込み
    offset = fillArray(hdr_mark_arr, buffer, offset);
    offset = fillArray(hdr_space_arr, buffer, offset);

    // 赤外線の信号はbig endian
    for (int mask = 1 << (BITS - 1);  mask != 0;  mask = mask >>> 1) {
      // high
      offset = fillArray(bit_mark_arr, buffer, offset);

      // bitにより空白期間の長さを変える
      if ((val & mask) != 0) {
        offset = fillArray(one_space_buf, buffer, offset);
      } else {
        offset = fillArray(zero_space_buf, buffer, offset);
      }
    }
    // terminate
    offset = fillArray(bit_mark_arr, buffer, offset);

    return offset;
  }

  public byte[] getByteArray() {
    return buffer;
  }

  /**
   * コマンドライン引数のパース
   */
  public static int parseArg(String arg) {
    int base = 10;
    if (arg.startsWith("0x")) {
      base = 16;
      arg = arg.substring(2, arg.length());
    } else if (arg.startsWith("0b")) {
      base = 2;
      arg = arg.substring(2, arg.length());
    }
    return (int)Long.parseLong(arg, base);
  }

  public static void main(String[] args){
    try{
      if (args.length != 1) {
        System.out.println("32bit integer is required.");
        System.exit(1);
      }

      // 赤外線信号の値（32bit整数値）
      int signal = parseArg(args[0]) & 0xffffffff;
      System.out.println("Signal = " + Integer.toHexString(signal));

      // 音声信号の作成
      IRSoundNEC irsound = new IRSoundNEC();
      int length = irsound.setValue(signal);
      byte[] buffer = irsound.getByteArray();

      // WAVEファイルの作成
      String wavefile = "ir.wav";
      AudioFormat frmt= new AudioFormat(
          IRSoundNEC.SAMPLE_RATE, IRSoundNEC.WAVE_BITS, IRSoundNEC.STEREO, true, false);
      AudioInputStream ais = new AudioInputStream(
          new ByteArrayInputStream(buffer), frmt, length);
      AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(wavefile));
      System.out.println("'" + wavefile + "' has been created.");
    }
    catch(Exception e){
      e.printStackTrace(System.err);
    }
  }
}
