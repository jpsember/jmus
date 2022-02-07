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

  private static final Color BAR_COLOR = new Color(128, 128, 128);

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
    todo(
        "why does setting stroke width(1) have different effect than default when rendering lines with CHORD_PAINT_SMALL?");

    Style style = style(styleNumber);

    Graphics2D g = graphics();
    PAINT_NORMAL.apply(g);

    IPoint pastBarPt = null;

    int y = PAGE_CONTENT.y;
    for (MusicSection section : song.sections()) {

      if (section.type() != 0) {
        IPoint plotLoc;
        boolean sameLine = section.sameLine() && pastBarPt != null;
        if (sameLine)
          plotLoc = pastBarPt.sumWith(style.barPadX, 0);
        else
          plotLoc = new IPoint(PAGE_CONTENT.x, y);

        int adv = renderString(section.type(), section.text(), style, plotLoc, sameLine);
        if (!sameLine)
          y += adv;
        continue;
      }

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
            cx += plotChord(chord, scale, style, new IPoint(cx, cy));
          }

          barX += barWidth;
        }
        y += barHeight;
        pastBarPt = new IPoint(barX, y);

        if (false && alert("stopping after single music line"))
          break;
      }

      y += style.spacingBetweenSections;

      if (false && alert("stopping after single section"))
        break;
    }
  }

  public int plotChord(Chord chord, Scale scale, Style style, IPoint loc) {
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

    tx().text(renderChord(chord, scale, null, null).toString());
    if (chord.slashChord() != null) {
      tx().text("~dash");
      tx().text(renderChord(chord.slashChord(), scale, null, null).toString());
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

  List<List<Chord>> barList = arrayList();

  public TextEntry.Builder tx() {
    TextEntry.Builder b = TextEntry.newBuilder();
    mTextEntries.add(b);
    return b;
  }

 public int renderString(int type, String text, Style style, IPoint loc, boolean plotAbove) {
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

  private static int renderTextEntries(Graphics2D g, Style style, Collection<TextEntry.Builder> textEntries,
      IPoint topLeft) {

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
          rowHeight = style.dashHeight;
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
           int y0 = py + style.dashOffset;
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
