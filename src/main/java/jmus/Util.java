package jmus;

import jmus.gen.Chord;

public final class Util {

  public static StringBuilder renderChord(Chord chord, StringBuilder sb) {
    if (sb == null)
      sb = new StringBuilder();

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

    sb.append(chord.number());
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

}
