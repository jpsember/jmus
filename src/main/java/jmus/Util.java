package jmus;

import static js.base.Tools.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.util.Collection;

import jmus.gen.Chord;
import jmus.gen.MusicLine;
import jmus.gen.MusicSection;
import jmus.gen.Scale;
import jmus.gen.ScaleMap;
import jmus.gen.Song;
import jmus.gen.TextEntry;
import js.file.Files;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.graphics.Paint;
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

  public static Scale scale(String name) {
    return scaleMap().scales().get(name);
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

  public static final int T_WS = 0, T_CR = 1, T_STRING = 2, T_CHORD = 3, T_FWD_SLASH = 4;

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

  // Fonts that look ok with regard to spacing of flats, sharps:
  // -----------------------------------------------------------
  // Courier
  // Courier New
  // Dialog
  // DialogInput
  // Helvetica Neue
  // Lucida Grande
  // Menlo
  // Monospaced
  // SansSerif
  //
  // (Maybe there is some way to improve the spacing so other fonts can be used as well?)
  // (A custom drawString that shifts some chars over...)

  public static final String FONT_NAME = "Dialog";

  public static final Font FONT_BOLD = new Font(FONT_NAME, Font.BOLD, 18);
  public static final Paint PAINT_NORMAL = Paint.newBuilder().font(FONT_BOLD, 1f).color(Color.black).width(1f)
      .build();
  public static final Paint PAINT_LIGHTER = PAINT_NORMAL.toBuilder().color(64, 64, 64).build();

  public static void renderText(Graphics2D g, Collection<TextEntry.Builder> textEntries, IPoint center) {
    FontMetrics f = g.getFontMetrics();

    int y = 0;
    int maxWidth = 0;
    for (TextEntry.Builder tx : textEntries) {
      tx.yOffset(y);

      if (tx.text().startsWith("~")) {
      } else
        tx.renderWidth(f.stringWidth(tx.text()));

      maxWidth = Math.max(maxWidth, tx.renderWidth());
      // apparently this cast is not required, as += performs type coercion (which is strange)
      // https://stackoverflow.com/questions/8272635
      y += (int) (f.getHeight() * tx.heightScale());
    }
    int heightTotal = y - f.getLeading();

    int py = center.y - heightTotal / 2;
    int x0 = center.x - maxWidth / 2;
    int x1 = center.x + maxWidth / 2;

    for (TextEntry tx : textEntries) {
      if (tx.text().startsWith("~")) {
        switch (tx.text().substring(1)) {
        default:
          throw notSupported("unknown text:", tx);
        case "dash": {
          int y0 = (int) (py + tx.yOffset() - f.getAscent() + (f.getAscent() * tx.heightScale() * 0.5f));
          g.drawLine(x0, y0, x1, y0);
        }
          break;
        }
      } else
        g.drawString(tx.text(), center.x - tx.renderWidth() / 2, py + tx.yOffset());
    }
  }

  // fontOffset 0, 86, 165
  //
  public static void renderFonts(Graphics2D g, int fontOffset) {
    String fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    int column = -1;
    int y = -1;
    int x = -1;

    while (true) {
      if (fontOffset >= fonts.length)
        break;
      String s = fonts[fontOffset];
      fontOffset++;
      if (y < 0) {
        column++;
        if (column >= 2)
          break;
        y = PAGE_CONTENT.y + 20;
        x = (int) (PAGE_CONTENT.x + 10 + column * PIXELS_PER_INCH * 4f);
      }

      Font f = new Font(s, Font.BOLD, 18);
      g.setFont(f);
      FontMetrics m = g.getFontMetrics();
      g.drawString(s + "(E♭ A♭ F♯)", x + 20, y + m.getAscent());
      y += m.getHeight();
      if (y + m.getAscent() >= PAGE_CONTENT.endY())
        y = -1;
    }
    pr("font offset:", fontOffset);
  }

  public static void rect(Graphics2D g, IRect r) {
    g.drawRect(r.x, r.y, r.width, r.height);
  }

  public static void fill(Graphics2D g, int x, int y, int w, int h) {
    g.fillRect(x, y, w, h);
  }

  public static void cross(Graphics2D g, int x, int y) {
    int r = 4;
    g.drawLine(x - r, y, x + r, y);
    g.drawLine(x, y - r, x, y + 4);
  }

  public static String renderSongAsText(Song song, Scale scale) {
    final int CHORD_COLUMN_SIZE = 5;

    StringBuilder lineBuilder = new StringBuilder();

    for (MusicSection section : song.sections()) {
      lineBuilder.append("\n");
      for (MusicLine line : section.lines()) {
        int cursor = lineBuilder.length();
        int chordNum = -1;
        for (Chord chord : line.chords()) {
          chordNum++;
          tab(lineBuilder, cursor + chordNum * CHORD_COLUMN_SIZE);
          String chordStr = renderChord(chord, scale, null).toString();
          lineBuilder.append(chordStr);
        }
        lineBuilder.append("\n");
      }
    }

    return lineBuilder.toString();
  }

}
