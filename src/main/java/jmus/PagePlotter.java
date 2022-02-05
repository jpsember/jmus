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

  private static final Paint PAINT_CHORD = PAINT_NORMAL.toBuilder().font(FONT_PLAIN, 1.8f).build();
  private static final Paint PAINT_CHORD_SMALL = PAINT_CHORD.toBuilder().font(FONT_PLAIN, 1f).build();
  private static final Paint PAINT_BAR_FRAME = PAINT_NORMAL.toBuilder().color(192, 192, 192).width(3).build();
  private static final int MEAN_CHORD_WIDTH_PIXELS = 35;
  private static final int DASH_HEIGHT = 3;

  public void render(Song song, Scale scale) {

    int chordHeight = 45;
    int barPadY = 10;
    int barPadX = 15;
    int chordPadX = 12;

    int spacingBetweenSections = 30;

    Graphics2D g = graphics();
    PAINT_NORMAL.apply(g);

    int y = PAGE_CONTENT.y;

    int sectionNumber = -1;
    for (MusicSection section : song.sections()) {

      sectionNumber++;
      if (sectionNumber != 0)
        y += spacingBetweenSections;

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

      int barHeight = chordHeight + 1 * barPadY;

      // Loop over each music line in the song

      for (List<MusicLine> barList : barLists) {

        // Loop over each set of chords in each bar

        int barNum = -1;
        int barX = PAGE_CONTENT.x;

        for (MusicLine bars : barList) {
          barNum++;

          int barWidth = (MEAN_CHORD_WIDTH_PIXELS + chordPadX) * maxChordsPerBar[barNum] + chordPadX;
          PAINT_BAR_FRAME.apply(graphics());
          rect(graphics(), barX, y, barWidth, barHeight);

          int cx = barX + barPadX;
          int cy = y + barPadY;

          // Loop over each chord in this bar

          for (Chord chord : bars.chords()) {
            Paint chordPaint = PAINT_CHORD;
            int yAdjust = 0;

            if (chord.slashChord() != null) {
              chordPaint = PAINT_CHORD_SMALL;
              todo(
                  "why does setting stroke width(1) have different effect than default when rendering lines with CHORD_PAINT_SMALL?");
              yAdjust = -4;
            }

            chordPaint.apply(graphics());

            int displayedWidth = plotChord(chord, scale, new IPoint(cx, cy + yAdjust));
            cx += displayedWidth + chordPadX;
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

  private int plotChord(Chord chord, Scale scale, IPoint location) {
    mTextEntries.clear();

    tx().text(renderChord(chord, scale, null, null).toString());
    if (chord.slashChord() != null) {
      tx().text("~dash");
      tx().text(renderChord(chord.slashChord(), scale, null, null).toString());
    }

    int width = renderText(graphics(), mTextEntries, location);
    mTextEntries.clear();
    return width;
  }

  public TextEntry.Builder tx() {
    TextEntry.Builder b = TextEntry.newBuilder();
    mTextEntries.add(b);
    return b;
  }

  private static int renderText(Graphics2D g, Collection<TextEntry.Builder> textEntries, IPoint topLeft) {

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
          rowHeight = DASH_HEIGHT;
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
          todo("clarify calculations here");
          int y0 = py + f.getAscent() + DASH_HEIGHT + 2;
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

  private BufferedImage mImage;
  private Graphics2D mGraphics;
  private List<TextEntry.Builder> mTextEntries = arrayList();

}
