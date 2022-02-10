package jmus;

import static js.base.Tools.*;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;
import java.util.List;

import jmus.gen.Chord;
import jmus.gen.ChordType;
import jmus.gen.MusicKey;
import jmus.gen.MusicLine;
import jmus.gen.MusicSection;
import jmus.gen.Song;
import js.base.BaseObject;
import js.file.Files;
import js.geometry.IPoint;
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

  public void render(Song song, int styleNumber) {
    Style style = style(styleNumber);

    Graphics2D g = graphics();
    PAINT_NORMAL.apply(g);

    IPoint pastBarPt = null;

    int y = PAGE_CONTENT.y;
    for (MusicSection section : song.sections()) {
      switch (section.type()) {
      default:
        throw notSupported("unsupported section type:", section);

      case T_KEY:
        mKey = musicKey(section.text());
        break;

      case T_TITLE:
      case T_SUBTITLE:
      case T_TEXT:
      case T_SMALLTEXT: {
        IPoint plotLoc;
        boolean sameLine = section.sameLine() && pastBarPt != null;
        if (sameLine)
          plotLoc = pastBarPt.sumWith(style.barPadX, 0);
        else
          plotLoc = new IPoint(PAGE_CONTENT.x, y);

        int adv = renderString(section.type(), section.text(), style, plotLoc, sameLine);
        if (!sameLine)
          y += adv;
      }
        break;

      case 0: {
        List<List<MusicLine>> barLists = arrayList();
        int[] maxChordsPerBar = new int[50];

        // Determine chord sets within each bar, and the maximum size of each such set
        {
          for (MusicLine line : section.lines()) {
            List<MusicLine> chordsWithinBarsList = extractChordsForBars(line, section.beatsPerBar());
            barLists.add(chordsWithinBarsList);
            int j = -1;
            for (MusicLine m : chordsWithinBarsList) {
              j++;
              maxChordsPerBar[j] = Math.max(maxChordsPerBar[j], m.chords().size());
            }
          }
        }

        int barHeight = style.chordHeight + 1 * style.barPadY;

        // Loop over each music line in the song

        for (List<MusicLine> barList : barLists) {

          // Loop over each set of chords in each bar

          int barNum = -1;
          int barX = PAGE_CONTENT.x;

          for (MusicLine bars : barList) {
            barNum++;

            int barWidth = (style.meanChordWidthPixels + style.chordPadX) * maxChordsPerBar[barNum]
                + style.chordPadX;
            style.paintBarFrame.apply(graphics());
            rect(graphics(), barX, y, barWidth, barHeight);

            int cx = barX + style.barPadX;
            int cy = y + style.barPadY;

            // Loop over each chord in this bar

            for (Chord chord : bars.chords()) {
              cx += plotChord(chord, style, new IPoint(cx, cy));
            }

            barX += barWidth;
          }
          y += barHeight;
          pastBarPt = new IPoint(barX, y);
        }

        y += style.spacingBetweenSections;
      }
        break;
      }
    }
  }

  public int plotChord(Chord chord, Style style, IPoint loc) {
    Paint chordPaint = style.paintChord;
    int yAdjust = 0;

    if (chord.slashChord() != null) {
      chordPaint = style.paintChordSmall;
      yAdjust = -4;
    }

    chordPaint.apply(graphics());
    if (chord.type() == ChordType.BEAT)
      graphics().setColor(BAR_COLOR);

    mTextEntries.clear();

    tx().text = renderChord(chord, mKey, null, null).toString();
    if (chord.slashChord() != null) {
      tx().text = "~dash";
      tx().text = renderChord(chord.slashChord(), mKey, null, null).toString();
    }

    int width = renderTextEntries(graphics(), style, mTextEntries, loc.sumWith(0, yAdjust));
    mTextEntries.clear();
    return width + style.chordPadX;
  }

  private List<MusicLine> extractChordsForBars(MusicLine line, int beatsPerBar) {
    // Split the line's chords into bars
    List<MusicLine> barList = arrayList();

    MusicLine.Builder currentBar = MusicLine.newBuilder();
    for (Chord chord : line.chords()) {

      // If this chord is the start of a new bar, start a new bar list
      if (chord.beatNumber() <= 0) {
        padWithBeats(currentBar, beatsPerBar);
        currentBar = MusicLine.newBuilder();
        barList.add(currentBar);
      }
      currentBar.chords().add(chord);
    }
    padWithBeats(currentBar, beatsPerBar);
    return barList;
  }

  private void padWithBeats(MusicLine.Builder bar, int beatsPerBar) {
    if (bar == null || beatsPerBar == 0)
      return;
    while (bar.chords().size() < beatsPerBar)
      bar.chords().add(Chord.newBuilder().type(ChordType.BEAT));
  }

  private TextEntry tx() {
    TextEntry b = new TextEntry();
    mTextEntries.add(b);
    return b;
  }

  private int renderString(int type, String text, Style style, IPoint loc, boolean plotAbove) {
    Graphics2D g = graphics();
    Paint pt;
    boolean center = false;
    switch (type) {
    default:
      throw notSupported("text type", type);
    case T_TITLE:
      center = true;
      pt = style.paintTitle;
      break;
    case T_SUBTITLE:
      center = true;
      pt = style.paintSubtitle;
      break;
    case T_TEXT:
      pt = style.paintText;
      break;
    case T_SMALLTEXT:
      pt = style.paintSmallText;
      break;
    }

    pt.apply(graphics());
    FontMetrics f = g.getFontMetrics();

    int x = loc.x;
    int y = loc.y;
    if (!plotAbove)
      y = y + f.getAscent();

    if (center)
      x = PAGE_CONTENT.midX() - f.stringWidth(text) / 2;

    g.drawString(text, x, y);

    return f.getHeight() + style.spacingBetweenSections / 3;
  }

  private static int renderTextEntries(Graphics2D g, Style style, Collection<TextEntry> textEntries,
      IPoint topLeft) {

    FontMetrics f = g.getFontMetrics();

    List<List<RenderedChar>> charPositionLists = arrayList();

    int maxWidth = 0;
    {
      int y = 0;
      for (TextEntry tx : textEntries) {

        List<RenderedChar> charPositionList = null;

        int rowHeight = f.getHeight();
        tx.yOffset = y;

        if (tx.text.startsWith("~")) {
          rowHeight = style.dashHeight;
        } else {
          charPositionList = determineCharPositions(f, tx.text);
          int newWidth = determineStringWidth(charPositionList);
          tx.renderWidth = newWidth;
        }
        maxWidth = Math.max(maxWidth, tx.renderWidth);
        y += rowHeight;
        charPositionLists.add(charPositionList);
      }
    }

    int py = topLeft.y;
    int x0 = topLeft.x;
    int x1 = topLeft.x + maxWidth;

    int row = -1;
    for (TextEntry tx : textEntries) {
      row++;
      if (tx.text.startsWith("~")) {
        switch (tx.text.substring(1)) {
        default:
          throw notSupported("unknown text:", tx);
        case "dash": {
          int y0 = py + style.dashOffset;
          line(g, x0, y0, x1, y0);
        }
          break;
        }
      } else {
        List<RenderedChar> charPositionList = charPositionLists.get(row);
        int y = py + tx.yOffset;
        String str = tx.text;

        int ry = y + f.getAscent();
        for (int i = 0; i < str.length(); i++) {
          char c = str.charAt(i);
          RenderedChar charPosition = charPositionList.get(i);
          g.drawString(Character.toString(c), x0 + charPosition.x, ry);
        }
      }
    }
    return maxWidth;
  }

  private static int determineStringWidth(List<RenderedChar> rcl) {
    if (rcl.isEmpty())
      return 0;
    RenderedChar last = last(rcl);
    return last.x + last.width;
  }

  private static List<RenderedChar> determineCharPositions(FontMetrics metrics, String text) {
    List<RenderedChar> result = arrayList();

    int x = 0;

    for (int i = 0; i < text.length(); i++) {

      RenderedChar charInfo = new RenderedChar();
      charInfo.ch = text.charAt(i);

      int cw = metrics.charWidth(charInfo.ch);

      charInfo.width = cw;

      if (charInfo.ch > 0xff) {
        x -= 2;
        charInfo.width -= 2;
      }

      charInfo.x = x;
      x += charInfo.width;
      result.add(charInfo);
    }
    return result;
  }

  private static class RenderedChar {
    char ch;
    int x;
    int width;
  }

  private static class TextEntry {
    String text;
    int renderWidth;
    int yOffset;
  }

  private MusicKey mKey;
  private BufferedImage mImage;
  private Graphics2D mGraphics;
  private List<TextEntry> mTextEntries = arrayList();

}
