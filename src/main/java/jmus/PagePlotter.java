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

    int spacingBetweenLines = 55;
    int spacingBetweenSections = 30;
    int spacingBetweenChords = 30;

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

        int chordNum = -1;
        for (Chord chord : line.chords()) {
          chordNum++;

          todo("figure out chord spacing");
          int x = PAGE_CONTENT.x + chordNum * spacingBetweenChords;

          plotChord(chord, scale, new IPoint(x, y));
        }

        todo("figure out spacing between lines");
        y += spacingBetweenLines;
      }

      if (false && alert("stopping after single section"))
        break;
    }

  }

  private void plotChord(Chord chord, Scale scale, IPoint location) {
    mTextEntries.clear();

    tx().text(renderChord(chord, scale, null, null).toString());
    if (chord.slashChord() != null) {
      tx() .text("~dash");
      tx().text(renderChord(chord.slashChord(), scale, null, null).toString());
    }

    renderText(graphics(), mTextEntries, location);
    mTextEntries.clear();
  }

  public void experiment() {

    Graphics2D g = graphics();
    PAINT_NORMAL.apply(g);

    if (false)
      renderFonts(g, 0);

    PAINT_LIGHTER.apply(g);

    tx().text("Gm Hello4");
    tx().text("E♭ A♭ F♯");
    tx() .text("~dash");
    tx().text("Hippopotamus");

    renderText(g, mTextEntries, new IPoint(600, 200));
    mTextEntries.clear();

    generateOutputFile(new File("_SKIP_experiment.png"));
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
