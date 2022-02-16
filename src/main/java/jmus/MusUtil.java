package jmus;

import static js.base.Tools.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Map;

import jmus.gen.Chord;
import jmus.gen.ChordType;
import jmus.gen.MusicKey;
import jmus.gen.MusicSection;
import jmus.gen.SectionType;
import jmus.gen.Song;
import jmus.gen.Style;
import js.file.Files;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.graphics.Paint;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.DFA;

public final class MusUtil {

  public static final boolean DEV = false && alert("DEV mode is true");
  public static final boolean EXTRA_CHARS = false && alert("EXTRA_CHARS in effect");

  // 
  // Useful reference for unicode:  https://en.wikipedia.org/wiki/Unicode_subscripts_and_superscripts

  private static final int[] numberToKeyIndex = { 0, 2, 4, 5, 7, 9, 11 };
  private static final int[] numberToKeyIndexFlat = { 11, 1, 3, 4, 6, 8, 10 };
  private static final int[] numberToKeyIndexSharp = { 1, 3, 4, 6, 8, 10, 0 };

  public static StringBuilder renderChord(Chord chord, MusicKey key, StringBuilder sb,
      Chord slashChordOrNull) {
    if (sb == null)
      sb = new StringBuilder();

    if (chord.type() == ChordType.BEAT) {
      sb.append(".");
      return sb;
    }

    if (key != null) {
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

      sb.append(key.keys().get(keyIndex));
      if (EXTRA_CHARS)
        sb.append("♯");

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
      //  sb.append('ᵐ');
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

    if (slashChordOrNull != null) {
      sb.append('/');
      renderChord(slashChordOrNull, key, sb, null);
    }
    return sb;
  }

  public static MusicKey musicKey(String name) {
    MusicKey s = keyMap().get(name);
    if (s == null)
      throw badArg("Can't find key:", quote(name));
    return s;
  }

  public static Map<String, MusicKey> keyMap() {
    if (sMusicKeyMap == null) {
      JSMap jsMap = new JSMap(Files.readString(MusUtil.class, "scales.json"));
      Map<String, MusicKey> mp = hashMap();
      for (String name : jsMap.keySet())
        mp.put(name, Files.parseAbstractData(MusicKey.DEFAULT_INSTANCE, jsMap.getMap(name)));
      sMusicKeyMap = mp;
    }
    return sMusicKeyMap;
  }

  public static List<String> musicKeyNames() {
    if (sMusicKeyNames == null) {
      sMusicKeyNames = arrayList();
      sMusicKeyNames.addAll(keyMap().keySet());
    }
    return sMusicKeyNames;
  }

  private static Map<String, MusicKey> sMusicKeyMap;
  private static List<String> sMusicKeyNames;

  // ------------------------------------------------------------------
  // Parsing
  // ------------------------------------------------------------------

  public static DFA dfa() {
    if (mDFA == null)
      mDFA = new DFA(Files.readString(MusUtil.class, "tokens.dfa"));
    return mDFA;
  }

  private static DFA mDFA;

  public static final int T_WS = k(1), T_CR = k(), T_STRING = k(), T_CHORD = k(), T_FWD_SLASH = k(),
      T_PAROP = k(), T_PARCL = k(), T_PERIOD = k(), T_TITLE = k(), T_SUBTITLE = k(), T_TEXT = k(),
      T_SMALLTEXT = k(), T_BEATS = k(), T_KEY = k(), T_BWD_SLASH = k();

  // ------------------------------------------------------------------
  // Helper functions for initializing strictly increasing constants
  // ------------------------------------------------------------------

  private static int k(int initialValue) {
    sIncrementingConstant = initialValue;
    return initialValue;
  }

  private static int k() {
    int result = sIncrementingConstant;
    sIncrementingConstant++;
    return result;
  }

  private static int sIncrementingConstant = 0;

  // ------------------------------------------------------------------
  // Rendering
  // ------------------------------------------------------------------

  public static final int PIXELS_PER_INCH = 100;

  // We want the physical number of pixels to be larger than our logical page size (100 pixels per inch),
  // ideally to match whatever printer we are using.
  public static final int DOTS_PER_INCH = Math.round(300 / (float) PIXELS_PER_INCH);

  public static final IPoint PAGE_SIZE = new IPoint(PIXELS_PER_INCH * 8.5f, PIXELS_PER_INCH * 11f);
  public static final int PAGE_MARGIN = PIXELS_PER_INCH / 4;
  public static final IRect PAGE_FULL = new IRect(PAGE_SIZE);
  public static final IRect PAGE_CONTENT = PAGE_FULL.withInset(PAGE_MARGIN);

  public static final String FONT_NAME = "Courier";
  public static final Font FONT_PLAIN = new Font(FONT_NAME, Font.PLAIN, 18);
  public static final Font FONT_BOLD = new Font(FONT_NAME, Font.BOLD, 18);

  public static final String TEXT_FONT_NAME = "Chalkboard"; // "Noteworthy"; //
  public static final Font FONT_TEXT_PLAIN = new Font(TEXT_FONT_NAME, Font.PLAIN, 18);
  public static final Font FONT_TEXT_BOLD = new Font(TEXT_FONT_NAME, Font.BOLD, 18);

  public static final Paint PAINT_NORMAL = Paint.newBuilder().font(FONT_PLAIN, 1f).color(Color.black)
      .width(1f).build();

  public static void rect(Graphics2D g, IRect r) {
    g.drawRect(r.x, r.y, r.width, r.height);
  }

  public static void rect(Graphics2D g, float x, float y, float width, float height) {
    g.drawRect((int) x, (int) y, (int) width, (int) height);
  }

  public static void line(Graphics2D g, int x0, int y0, int x1, int y1) {
    g.drawLine(x0, y0, x1, y1);
  }

  public static void fill(Graphics2D g, int x, int y, int w, int h) {
    g.fillRect(x, y, w, h);
  }

  public static void cross(Graphics2D g, int x, int y) {
    int r = 4;
    line(g, x - r, y, x + r, y);
    line(g, x, y - r, x, y + 4);
  }

  public static String renderSongAsText(Song song, MusicKey key) {
    final int CHORD_COLUMN_SIZE = 5;

    StringBuilder lineBuilder = new StringBuilder();

    for (MusicSection section : song.sections()) {
      lineBuilder.append("\n");
      int chordNum = -1;
      for (Chord chord : section.chords()) {
        chordNum++;
        int cursor = lineBuilder.length();
        tab(lineBuilder, cursor + chordNum * CHORD_COLUMN_SIZE);
        renderChord(chord, key, lineBuilder, chord.slashChord());
      }
      lineBuilder.append("\n");
    }

    return lineBuilder.toString();
  }

  public static Style style(int index) {
    return getStyleList().get(index);
  }

  private static List<Style> getStyleList() {
    if (sStyleList == null) {
      JSMap jsonMap = new JSMap(Files.readString(MusUtil.class, "styles.json"));
      JSList listOfMaps = jsonMap.getList("");
      List<Style> styleList = arrayList();
      for (JSMap styleJson : listOfMaps.asMaps()) {
        Style style = Files.parseAbstractData(Style.DEFAULT_INSTANCE, styleJson);
        styleList.add(style);
      }
      sStyleList = styleList;
    }
    return sStyleList;
  }

  private static List<Style> sStyleList;

  public static Chord.Builder chord(int number) {
    return Chord.newBuilder().number(number);
  }

  public static Chord.Builder chord(int number, int baseNumber) {
    return chord(number).slashChord(chord(baseNumber));
  }

  public static String symbolicName(MusicKey key) {
    checkState(key != null, "no key defined");
    return key.keys().get(0);
  }

  public static boolean visibleSection(SectionType type) {
    return type.ordinal() <= SectionType.SMALL_TEXT.ordinal();
  }

}
