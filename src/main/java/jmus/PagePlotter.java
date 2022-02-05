package jmus;

import static js.base.Tools.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
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

import static jmus.Util.*;

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

  public void render(Song song, Scale scale) {

    int chordHeight = 45;
    int barPadY = 5;
    int barPadX = 5;
    int barWidth = 100;
    int chordPadX = 12;

    int spacingBetweenSections = 30;

    Graphics2D g = graphics();
    PAINT_NORMAL.apply(g);

    int y = PAGE_CONTENT.y;

    boolean indentRequired = false;
    for (MusicSection section : song.sections()) {
      if (indentRequired) {
        todo("figure out blank line spacing");
        y += spacingBetweenSections;
      }
      indentRequired = true;

      for (MusicLine line : section.lines()) {

        // Split the line's chords into bars
        List<List<Chord>> barList = arrayList();
        {
          List<Chord> currentBar = null;
          for (Chord chord : line.chords()) {

            // If this chord is the start of a new bar, start a new bar list
            if (chord.beatNumber() <= 0) {
              currentBar = arrayList();
              barList.add(currentBar);
            }
            currentBar.add(chord);
          }
        }

        int barHeight = chordHeight + 2 * barPadY;
        int barNum = -1;
        for (List<Chord> currentBar : barList) {
          barNum++;

          int barX = PAGE_CONTENT.x + barNum * barWidth;
          rect(graphics(), barX, y, barWidth, barHeight);

          int cx = barX + barPadX;
          int cy = y + barPadY;
          for (Chord chord : currentBar) {

            // If this is a slash chord, use a smaller font
            Paint chordPaint = (chord.slashChord() == null) ? CHORD_PAINT_NORMAL : CHORD_PAINT_SMALL;
            chordPaint.apply(graphics());

            int displayedWidth = plotChord(chord, scale, new IPoint(cx, cy));
            cx += displayedWidth + chordPadX;
          }
        }
        todo("figure out spacing between lines");
        y += barHeight;
      }

      if (false && alert("stopping after single section"))
        break;
    }

  }

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

  private BufferedImage mImage;
  private Graphics2D mGraphics;
  private List<TextEntry.Builder> mTextEntries = arrayList();

}
