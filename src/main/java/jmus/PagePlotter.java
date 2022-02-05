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
import jmus.gen.MusicLine;
import jmus.gen.MusicSection;
import jmus.gen.Scale;
import jmus.gen.Song;
import jmus.gen.TextEntry;
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
    loadTools();
    BufferedImage img = mImage = ImgUtil.build(PAGE_SIZE.scaledBy(DOTS_PER_INCH),
        ImgUtil.PREFERRED_IMAGE_TYPE_COLOR);
    Graphics2D g = mGraphics = img.createGraphics();
    g.setColor(Color.white);
    g.fillRect(0, 0, img.getWidth(), img.getHeight());
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setTransform(Matrix.getScale(DOTS_PER_INCH).toAffineTransform());
    PAINT_NORMAL.apply(g);
  }

  public Graphics2D graphics() {
    return mGraphics;
  }

  public BufferedImage image() {
    return mImage;
  }

  public void generateOutputFile(File outputFile) {
    ImgUtil.writeImage(Files.S, mImage, outputFile);
  }

  public void render(Song song, Scale scale, int styleNumber) {

    if (alert("setting style"))
      styleNumber = 1;
    Style style = style(styleNumber);

    Graphics2D g = graphics();
    PAINT_NORMAL.apply(g);

    int y = PAGE_CONTENT.y;

    int sectionNumber = -1;
    for (MusicSection section : song.sections()) {

      sectionNumber++;
      if (sectionNumber != 0)
        y += style.spacingBetweenSections;

      List<List<MusicLine>> barLists = arrayList();
      int[] maxChordsPerBar = new int[50];

      // Determine chord sets within each bar, and the maximum size of each such set
      {
        for (MusicLine line : section.lines()) {
          List<MusicLine> chordsWithinBarsList = extractChordsForBars(line);
          barLists.add(chordsWithinBarsList);
          int j = -1;
          for (MusicLine m : chordsWithinBarsList) {
            j++;
            maxChordsPerBar[j] = Math.max(maxChordsPerBar[j], m.chords().size());
          }
        }
      }

      int barHeight = style.mChordHeight + 1 * style.barPadY;

      // Loop over each music line in the song

      for (List<MusicLine> barList : barLists) {

        // Loop over each set of chords in each bar

        int barNum = -1;
        int barX = PAGE_CONTENT.x;

        for (MusicLine bars : barList) {
          barNum++;

          int barWidth = (style.mMeanChordWidthPixels + style.chordPadX) * maxChordsPerBar[barNum]
              + style.chordPadX;
          style.mPaintBarFrame.apply(graphics());
          rect(graphics(), barX, y, barWidth, barHeight);

          int cx = barX + style.barPadX;
          int cy = y + style.barPadY;

          // Loop over each chord in this bar

          for (Chord chord : bars.chords()) {
            Paint chordPaint = style.mPaintChord;
            int yAdjust = 0;

            if (chord.slashChord() != null) {
              chordPaint = style.mPaintChordSmall;
              todo(
                  "why does setting stroke width(1) have different effect than default when rendering lines with CHORD_PAINT_SMALL?");
              yAdjust = -4;
            }

            chordPaint.apply(graphics());

            int displayedWidth = plotChord(chord, scale, style, new IPoint(cx, cy + yAdjust));
            cx += displayedWidth + style.chordPadX;
          }

          barX += barWidth;
        }

        y += barHeight;
        if (false && alert("stopping after single music line"))
          break;
      }

      if (false && alert("stopping after single section"))
        break;
    }
  }

  private List<MusicLine> extractChordsForBars(MusicLine line) {
    // Split the line's chords into bars
    List<MusicLine> barList = arrayList();

    MusicLine.Builder currentBar = MusicLine.newBuilder();
    for (Chord chord : line.chords()) {

      // If this chord is the start of a new bar, start a new bar list
      if (chord.beatNumber() <= 0) {
        currentBar = MusicLine.newBuilder();
        barList.add(currentBar);
      }
      currentBar.chords().add(chord);
    }
    return barList;
  }

  List<List<Chord>> barList = arrayList();

  private int plotChord(Chord chord, Scale scale, Style style, IPoint location) {
    mTextEntries.clear();

    tx().text(renderChord(chord, scale, null, null).toString());
    if (chord.slashChord() != null) {
      tx().text("~dash");
      tx().text(renderChord(chord.slashChord(), scale, null, null).toString());
    }

    int width = renderText(graphics(), style, mTextEntries, location);
    mTextEntries.clear();
    return width;
  }

  public TextEntry.Builder tx() {
    TextEntry.Builder b = TextEntry.newBuilder();
    mTextEntries.add(b);
    return b;
  }

  private static int renderText(Graphics2D g, Style style, Collection<TextEntry.Builder> textEntries,
      IPoint topLeft) {

    todo(
        "why does setting stroke width(1) have different effect than default when rendering lines with CHORD_PAINT_SMALL?");

    final boolean DEBUG = false;
    FontMetrics f = g.getFontMetrics();

    int maxWidth = 0;
    int heightTotal = 0;
    {
      int y = 0;
      for (TextEntry.Builder tx : textEntries) {

        int rowHeight = f.getHeight();
        tx.yOffset(y);

        if (tx.text().startsWith("~")) {
          todo("figure out how to parameterize this w.r.t. fonts etc");
          rowHeight = style.mDashHeight;
        } else
          tx.renderWidth(f.stringWidth(tx.text()));

        maxWidth = Math.max(maxWidth, tx.renderWidth());
        y += rowHeight;
        heightTotal = y - f.getLeading();
      }
    }

    int py = topLeft.y;
    int x0 = topLeft.x;
    int x1 = topLeft.x + maxWidth;

    if (DEBUG)
      rect(g, x0, py, maxWidth, heightTotal);

    for (TextEntry tx : textEntries) {
      if (tx.text().startsWith("~")) {
        switch (tx.text().substring(1)) {
        default:
          throw notSupported("unknown text:", tx);
        case "dash": {
          int y0 = py + style.mChordHeight / 2;
          line(g, x0, y0, x1, y0);
        }
          break;
        }
      } else {
        int y = py + tx.yOffset();
        g.drawString(tx.text(), x0, y + f.getAscent());

        if (DEBUG) {
          line(g, x0, y, x0 + maxWidth, y + f.getHeight());
          line(g, x0, y + f.getHeight(), x0 + maxWidth, y);
        }
      }
    }
    return maxWidth;
  }

  private static class Style {
    final Paint mPaintChord;
    final Paint mPaintChordSmall;
    final Paint mPaintBarFrame;
    final int mMeanChordWidthPixels;
    final int mChordHeight;
    final int mDashHeight;
    final int barPadY;
    final int barPadX;
    final int chordPadX;
    final int spacingBetweenSections;

    Style(Paint chord, Paint chordSmall, Paint barFrame, int meanChordWidth, int chordHeight, int dashHeight,
        int barPadX, int barPadY, int chordPadX, int spacingBetweenSections) {
      mPaintChord = chord;
      mPaintChordSmall = chordSmall;
      mPaintBarFrame = barFrame;
      mMeanChordWidthPixels = meanChordWidth;
      mChordHeight = chordHeight;
      mDashHeight = dashHeight;
      this.barPadX = barPadX;
      this.barPadY = barPadY;
      this.chordPadX = chordPadX;
      this.spacingBetweenSections = spacingBetweenSections;
    }
  }

  private static List<Style> sStyles;

  private static Style style(int index) {

    final Paint ptChord = PAINT_NORMAL.toBuilder().font(FONT_PLAIN, 1.8f).build();
    final Paint ptChordSmall = ptChord.toBuilder().font(FONT_PLAIN, 1f).build();
    final Paint ptFrame = PAINT_NORMAL.toBuilder().color(192, 192, 192).width(3).build();

    if (sStyles == null) {
      sStyles = arrayList();

      sStyles.add(new Style(ptChord, ptChordSmall, ptFrame, 35, 48, 3, 15, 10, 12, 34));

      sStyles.add(new Style(ptChord.toBuilder().font(FONT_PLAIN, 1.2f).build(),
          ptChordSmall.toBuilder().font(FONT_PLAIN, 0.7f).build(), ptFrame.toBuilder().width(2).build(), 24,
          32, 2, 10, 7, 9, 24));

    }
    return sStyles.get(index);
  }

  private BufferedImage mImage;
  private Graphics2D mGraphics;
  private List<TextEntry.Builder> mTextEntries = arrayList();

}
