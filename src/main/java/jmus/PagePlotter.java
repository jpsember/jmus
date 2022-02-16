package jmus;

import static js.base.Tools.*;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import jmus.gen.Chord;
import jmus.gen.ChordType;
import jmus.gen.MusicKey;
import jmus.gen.MusicSection;
import jmus.gen.SectionType;
import jmus.gen.Song;
import jmus.gen.Style;
import js.base.BaseObject;
import js.data.IntArray;
import js.file.Files;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.geometry.Matrix;
import js.graphics.ImgUtil;
import js.graphics.Paint;

import static jmus.MusUtil.*;

/**
 * For plotting a song into a png
 */
public final class PagePlotter extends BaseObject {

  public PagePlotter() {
    BufferedImage img = mImage = ImgUtil.build(PAGE_SIZE.scaledBy(DOTS_PER_INCH),
        ImgUtil.PREFERRED_IMAGE_TYPE_COLOR);
    Graphics2D g = mGraphics = img.createGraphics();
    g.setColor(Color.white);
    g.fillRect(0, 0, img.getWidth(), img.getHeight());
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setTransform(Matrix.getScale(DOTS_PER_INCH).toAffineTransform());
    PAINT_NORMAL.apply(g);
  }

  public void setKey(MusicKey key) {
    mKey = key;
  }

  private static final Color BAR_COLOR = new Color(128, 128, 128);

  public Graphics2D graphics() {
    return mGraphics;
  }

  public void generateOutputFile(File outputFile) {
    if (DEV) {
      if (outputFile.getName().startsWith("quiz"))
        outputFile = Files.getDesktopFile("quiz.png");
      else
        outputFile = Files.getDesktopFile("song.png");
    }
    ImgUtil.writeImage(Files.S, mImage, outputFile);
  }

  public void plotSong(Song song, Style style) {
    //pr("plotting song:", INDENT, song);
    mSong = song;
    PAINT_NORMAL.apply(graphics());

    IPoint cursor = PAGE_CONTENT.location();

    preparePlot();

    for (int sectionNumber = 0; sectionNumber < song.sections().size(); sectionNumber++) {

      if (mStartOfRow) {
        preparePlotRow(sectionNumber);
        mStartOfRow = false;
      }

      MusicSection section = song.sections().get(sectionNumber);

      // size in pixels of section
      IPoint sectionSize = null;

      switch (section.type()) {

      default:
        throw notSupported("unsupported section type:", section);

      case BEATS:
        mBeatsPerBar = section.intArg();
        break;

      case LINE_BREAK:
        mStartOfRow = true;
        cursor = new IPoint(PAGE_CONTENT.x, cursor.y + mRowHeight);
        mRowHeight = 0;
        break;

      case PARAGRAPH_BREAK:
        mStartOfRow = true;
        cursor = new IPoint(PAGE_CONTENT.x, cursor.y + mRowHeight + style.barPadY());
        mRowHeight = 0;
        break;

      case KEY:
        mKey = musicKey(section.textArg());
        break;

      case TITLE:
      case SUBTITLE:
      case TEXT:
      case SMALL_TEXT: {
        int px = cursor.x;
        int py = cursor.y;
        int boxHeight = 0;
        int xPadding = smallPadding(style);
        px += xPadding;
        if (mRowContainsChords)
          boxHeight = style.chordHeight();
        IPoint stringSize = renderString(section.type(), section.textArg(), style, boxHeight,
            mVisibleSectionsInRow == 1
                && (section.type() == SectionType.TITLE || section.type() == SectionType.SUBTITLE),
            new IPoint(px, py));
        px += xPadding;
        sectionSize = new IPoint(px - cursor.x + stringSize.x, stringSize.y);
      }
        break;

      case CHORD_SEQUENCE: {
        List<List<Chord>> barLists = arrayList();
        extractChordsForBars(section.chords(), mBeatsPerBar, barLists);
        int barHeight = style.chordHeight() + 1 * style.barPadY();

        IPoint lineLoc = cursor;
        IPoint barLoc = lineLoc;

        for (List<Chord> barList : barLists) {
          int barWidth = (style.meanChordWidthPixels() + style.chordPadX()) * barList.size()
              + style.chordPadX();
          style.paintBarFrame().apply(graphics());
          rect(graphics(), barLoc.x, barLoc.y, barWidth, barHeight);

          int cx = barLoc.x + style.barPadX();
          int cy = barLoc.y + style.barPadY();

          for (Chord chord : barList) {
            IPoint loc = new IPoint(cx, cy);
            plotChord(chord, style, loc);
            cx += style.meanChordWidthPixels() + style.chordPadX();
          }

          barLoc = barLoc.sumWith(barWidth, 0);
        }

        sectionSize = new IPoint(barLoc.x - cursor.x, barHeight);
        lineLoc = lineLoc.withX(lineLoc.x + sectionSize.x);
      }
        break;
      }

      if (sectionSize != null) {
        mRowHeight = Math.max(mRowHeight, sectionSize.y);
        cursor = cursor.sumWith(sectionSize.x, 0);
      }
    }
  }

  private void preparePlotRow(int sectionNumber) {
    mRowContainsChords = false;
    mVisibleSectionsInRow = 0;
    for (int j = sectionNumber; j < mSong.sections().size(); j++) {
      MusicSection s = mSong.sections().get(j);
      if (s.type() == SectionType.LINE_BREAK || s.type() == SectionType.PARAGRAPH_BREAK)
        break;
      if (visibleSection(s.type()))
        mVisibleSectionsInRow++;
      if (s.type() == SectionType.CHORD_SEQUENCE)
        mRowContainsChords = true;
    }
  }

  public void plotChord(Chord chord, Style style, IPoint loc) {
    Paint chordPaint = style.paintChord();
    int yAdjust = 0;

    if (chord.slashChord() != null) {
      chordPaint = style.paintChordSmall();
      todo("this yAdjustment should perhaps be in the style? What is it for?");
      yAdjust = -4;
    }

    mCharAdjustmentMap = CharAdjustmentMap.forFont(chordPaint.font());

    chordPaint.apply(graphics());
    if (chord.type() == ChordType.BEAT)
      graphics().setColor(BAR_COLOR);

    mTextEntries.clear();

    tx().text = renderChord(chord, mKey, null, null).toString();
    if (chord.slashChord() != null) {
      tx().text = "~dash";
      tx().text = renderChord(chord.slashChord(), mKey, null, null).toString();
    }

    renderTextEntries(style, loc.sumWith(0, yAdjust));
    mTextEntries.clear();
  }

  private void extractChordsForBars(List<Chord> chordList, int beatsPerBar, List<List<Chord>> barList) {
    List<Chord> currentBar = arrayList();
    for (Chord chord : chordList) {
      // If this chord is the start of a new bar, start a new bar list
      if (chord.beatNumber() <= 0) {
        padWithBeats(currentBar, beatsPerBar);
        if (!currentBar.isEmpty()) {
          barList.add(currentBar);
          currentBar = arrayList();
        }
      }
      currentBar.add(chord);
    }
    padWithBeats(currentBar, beatsPerBar);
    if (!currentBar.isEmpty()) {
      barList.add(currentBar);
    }
  }

  private void padWithBeats(List<Chord> bar, int beatsPerBar) {
    if (beatsPerBar == 0 || bar.isEmpty())
      return;
    while (bar.size() < beatsPerBar)
      bar.add(Chord.newBuilder().type(ChordType.BEAT));
  }

  private TextEntry tx() {
    TextEntry b = new TextEntry();
    mTextEntries.add(b);
    return b;
  }

  private IPoint renderString(SectionType type, String text, Style style, int boxHeight, boolean center,
      IPoint loc) {
    Graphics2D g = graphics();
    Paint pt;
    switch (type) {
    default:
      throw notSupported("text type", type);
    case TITLE:
      pt = style.paintTitle();
      break;
    case SUBTITLE:
      pt = style.paintSubtitle();
      break;
    case TEXT:
      pt = style.paintText();
      break;
    case SMALL_TEXT:
      pt = style.paintSmallText();
      break;
    }

    pt.apply(graphics());
    FontMetrics f = g.getFontMetrics();

    int width = f.stringWidth(text);
    int x = loc.x;
    int y;
    if (boxHeight != 0)
      y = loc.y + boxHeight;
    else
      y = loc.y + f.getAscent();

    if (center)
      x = PAGE_CONTENT.midX() - width / 2;

    g.drawString(text, x, y);
    return new IPoint(width, f.getHeight());
  }

  private void renderTextEntries(Style style, IPoint topLeft) {

    FontMetrics f = mGraphics.getFontMetrics();

    List<int[]> charPositionLists = arrayList();

    {
      int y = 0;
      for (TextEntry tx : mTextEntries) {

        int[] charPositionList = null;

        int rowHeight = f.getHeight();
        tx.yOffset = y;

        if (tx.text.startsWith("~")) {
          rowHeight = style.dashHeight();
        } else {
          charPositionList = determineCharPositions(style, f, tx.text);
        }
        y += rowHeight;
        charPositionLists.add(charPositionList);
      }
    }

    int py = topLeft.y;
    int x0 = topLeft.x;

    int row = -1;
    for (TextEntry tx : mTextEntries) {
      row++;
      if (tx.text.startsWith("~")) {
        switch (tx.text.substring(1)) {
        default:
          throw notSupported("unknown text:", tx);
        case "dash": {
          int y0 = py + style.dashOffset();
          mGraphics.drawString("_", x0, y0);
        }
          break;
        }
      } else {
        int[] charPositionList = charPositionLists.get(row);
        int y = py + tx.yOffset;
        String str = tx.text;

        int ry = y + f.getAscent();
        for (int i = 0; i < str.length(); i++) {
          char c = str.charAt(i);
          int charPosition = charPositionList[i];
          int x = x0 + charPosition;
          mGraphics.drawString(Character.toString(c), x, ry);
        }
      }
    }
  }

  private int[] determineCharPositions(Style style, FontMetrics metrics, String text) {
    IntArray.Builder charXPositions = IntArray.newBuilder();

    int x = 0;

    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      int cw = metrics.charWidth(c);

      IRect rect = mCharAdjustmentMap.getRect(c);

      if (rect != null) {
        // We need to plot the character n pixels further to the left,
        // and advance the cursor so the next character is drawn past the bounding rectangle
        //
        int leftPadding = 1;
        int rightPadding = 1;

        int xShift = -rect.x + leftPadding;
        cw = rect.width - xShift + rightPadding;
        x += xShift;
      }
      charXPositions.add(x);
      x += cw;
    }
    charXPositions.add(x);
    return charXPositions.array();
  }

  private static int smallPadding(Style style) {
    return style.barPadX() / 3;
  }

  private static class TextEntry {
    String text;
    int yOffset;
  }

  private void preparePlot() {
    mRowHeight = 0;
    mBeatsPerBar = 0;
    mStartOfRow = true;
    mRowContainsChords = false;
    mVisibleSectionsInRow = 0;
  }

  private MusicKey mKey;
  private BufferedImage mImage;
  private Graphics2D mGraphics;
  private List<TextEntry> mTextEntries = arrayList();

  // ------------------------------------------------------------------
  // plotSong() usage
  // ------------------------------------------------------------------

  private Song mSong;
  private int mRowHeight;
  private int mBeatsPerBar;
  // True if we're about to process song sections on a new row
  private boolean mStartOfRow;
  // True if the current row contains some chords
  private boolean mRowContainsChords;
  private int mVisibleSectionsInRow;
  private CharAdjustmentMap mCharAdjustmentMap;
}
