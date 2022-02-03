package jmus;

import static js.base.Tools.*;

import jmus.gen.Chord;
import jmus.gen.Scale;
import jmus.gen.ScaleMap;
import js.file.Files;
import js.json.JSMap;

public final class Util {

  public static StringBuilder renderChord(Chord chord, Scale scale, StringBuilder sb) {
    if (sb == null)
      sb = new StringBuilder();

    todo("need to do something different if rendering as scale");

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

    // Useful reference for unicode:  https://en.wikipedia.org/wiki/Unicode_subscripts_and_superscripts

    if (scale != null) {
      sb.append(scale.keys().get(chord.number() - 1));
    } else {
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

}
