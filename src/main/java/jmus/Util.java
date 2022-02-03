package jmus;

import static js.base.Tools.*;

import jmus.gen.Chord;
import jmus.gen.Scale;
import jmus.gen.ScaleMap;
import js.file.Files;
import js.json.JSMap;
import js.parsing.DFA;

public final class Util {

  // 
  // Useful reference for unicode:  https://en.wikipedia.org/wiki/Unicode_subscripts_and_superscripts

  private static final int[] numberToKeyIndex = { 0, 2, 4, 5, 7, 9, 11 };
  private static final int[] numberToKeyIndexFlat = { 11, 1, 3, 4, 6, 8, 10 };
  private static final int[] numberToKeyIndexSharp = { 1, 3, 4, 6, 8, 10, 0 };

  public static StringBuilder renderChord(Chord chord, Scale scale, StringBuilder sb) {
    if (sb == null)
      sb = new StringBuilder();

    if (scale != null) {

      int cn = chord.number() - 1;

      int keyIndex;

      switch (chord.accidental()) {
      case NONE:
        keyIndex = numberToKeyIndex[cn];
        break;
      default:
        throw notSupported(chord.accidental());
      case FLAT:
        keyIndex = numberToKeyIndexFlat[cn];
        break;
      case SHARP:
        keyIndex = numberToKeyIndexSharp[cn];
        break;
      }

      sb.append(scale.keys().get(keyIndex));

    } else {

      switch (chord.accidental()) {
      default:
        break;
      case FLAT:
        sb.append('♭');
        break;
      case SHARP:
        sb.append('♯');
        break;
      }

      sb.append(chord.number());
    }

    switch (chord.type()) {
    default:
      break;
    case MINOR:
      sb.append('⁻');
      break;
    case AUGMENTED:
      sb.append('⁺');
      break;
    case DIMINISHED:
      sb.append('ᵒ');
      break;
    }

    switch (chord.optType()) {
    default:
      break;
    case TWO:
      sb.append('²');
      break;
    case FOUR:
      sb.append('⁴');
      break;
    case FIVE:
      sb.append('⁵');
      break;
    case SIX:
      sb.append('⁶');
      break;
    case SEVEN:
      sb.append('⁷');
      break;
    case NINE:
      sb.append('⁹');
      break;
    }
    return sb;
  }

  public static ScaleMap scaleMap() {
    if (sScaleMap == null) {
      String content = Files.readString(Util.class, "scales.json");
      sScaleMap = Files.parseAbstractDataOpt(ScaleMap.DEFAULT_INSTANCE, new JSMap(content));
    }
    return sScaleMap;
  }

  private static ScaleMap sScaleMap;

  // ------------------------------------------------------------------
  // Parsing
  // ------------------------------------------------------------------

  public static DFA dfa() {
    if (mDFA == null)
      mDFA = new DFA(Files.readString(Util.class, "tokens.dfa"));
    return mDFA;
  }

  private static DFA mDFA;

  public static final int T_WS = 0, T_CR = 1, T_STRING = 2, T_CHORD = 3;

}
