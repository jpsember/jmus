/**
 * MIT License
 * 
 * Copyright (c) 2021 Jeff Sember
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 **/
package jmus;

import static js.base.Tools.*;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.io.File;
import java.util.List;
import java.util.Random;

import static jmus.MusUtil.*;

import jmus.gen.Chord;
import jmus.gen.SongConfig;
import jmus.gen.Scale;
import jmus.gen.Song;
import js.app.AppOper;
import js.data.IntArray;
import js.file.Files;
import js.geometry.IPoint;
import js.graphics.Paint;

public class SongOper extends AppOper {

  @Override
  public String userCommand() {
    return "main";
  }

  @Override
  public String getHelpDescription() {
    return "main operation";
  }

  @Override
  public SongConfig defaultArgs() {
    return SongConfig.DEFAULT_INSTANCE;
  }

  @Override
  public void perform() {
    mConfig = config();

    if (false && alert("rendering fonts")) {
      PagePlotter p = new PagePlotter();
      int nextFontIndex = renderFonts(p.graphics(), 194);
      pr("next font index:", nextFontIndex);
      File outFile = Files.getDesktopFile("fonts194.png");
      p.generateOutputFile(outFile);
      return;
    }

    if (Files.empty(mConfig.input())) {
      generateQuiz();
      return;
    }

    mSourceFile = mConfig.input();
    if (Files.empty(mSourceFile)) {
      setError("Please specify a source file");
    }

    Song song = new SongParser(mSourceFile).parse();

    Scale scale = null;
    if (nonEmpty(mConfig.scale()))
      scale = scale(mConfig.scale());

    PagePlotter p = new PagePlotter();
    p.setKey(scale);
    p.render(song, mConfig.style());
    File outFile = mConfig.output();
    if (Files.empty(outFile))
      outFile = mSourceFile;
    outFile = Files.setExtension(outFile, "png");
    p.generateOutputFile(outFile);
  }

  private static final Paint PAINT_SCALE = Paint.newBuilder().color(128, 128, 128).font(FONT_PLAIN, 0.8f)
      .build();

  private static final Paint PAINT_SEP = Paint.newBuilder().color(128, 128, 128).width(3).build();

  private static final Paint PAINT_ROW_BGND0 = Paint.newBuilder().color(200, 200, 200).build();
  private static final Paint PAINT_ROW_BGND1 = Paint.newBuilder().color(220, 220, 220).build();

  private void generateQuiz() {

    PagePlotter p = new PagePlotter();
    Graphics2D g = p.graphics();
    // rect(g, PAGE_CONTENT);

    Style style = style(0);
    int xAdvance = style.meanChordWidthPixels + style.chordPadX + 8;

    int y = PAGE_CONTENT.y;

    int startY = y;
    int setHeightPixels = 0;

    while (true) {

      int indent = PIXELS_PER_INCH * 1;
      int ysep = style.chordHeight + style.spacingBetweenSections;

      int chordsPerRow = 16;
      List<Chord> chords = randomChords(chordsPerRow);

      drawBarBetweenSets(g, style, y);

      int x = PAGE_CONTENT.x + indent;
      p.setKey(null);
      plotChords(p, chords, null, style, new IPoint(x, y), xAdvance);

      y += ysep * 1.2;

      List<Scale> scales = buildScaleList();

      int x0 = x - (int) (style.barPadX * 1.6);
      int x1 = x + xAdvance * chords.size();

      int rowNum = -1;
      for (Scale scale : scales) {
        rowNum++;
        {
          int y0 = y - style.chordPadX;
          int y1 = y0 + ysep;
          Paint bgndPaint = ((rowNum & 1) == 0) ? PAINT_ROW_BGND0 : PAINT_ROW_BGND1;
          bgndPaint.apply(g);
          fill(g, x0, y0, x1 - x0, y1 - y0);
        }

        String n = symbolicName(scale);
        {
          PAINT_SCALE.apply(g);
          FontMetrics f = g.getFontMetrics();
          int tx = x - f.stringWidth(n) - style.chordPadX * 2;
          g.drawString(n, tx, y + f.getAscent());
        }

        p.setKey(scale);
        plotChords(p, chords, scale, style, new IPoint(x, y), xAdvance);
        y += ysep;
      }
      y += ysep * .3f;
      if (setHeightPixels == 0)
        setHeightPixels = y - startY;
      if (y + setHeightPixels > PAGE_CONTENT.endY())
        break;
    }

    drawBarBetweenSets(g, style, y);

    File outFile;
    for (int i = 0;; i++) {
      outFile = Files.getDesktopFile(String.format("quiz%02d.png", i));
      if (!outFile.exists())
        break;
    }
    p.generateOutputFile(outFile);
  }

  private void drawBarBetweenSets(Graphics2D g, Style style, int y) {
    PAINT_SEP.apply(g);
    y -= style.spacingBetweenSections * 0.7f;
    line(g, PAGE_CONTENT.x, y, PAGE_CONTENT.endX(), y);
  }

  private List<Scale> buildScaleList() {
    List<Scale> scales = arrayList();
    String scaleExp = mConfig.scales();
    if (nullOrEmpty(scaleExp)) {
      scaleExp = "e-flat g f d b-flat";
    }
    for (String s : split(scaleExp, ' ')) {
      if (s.isEmpty())
        continue;
      scales.add(scale(s));
    }
    return scales;
  }

  private void plotChords(PagePlotter p, List<Chord> chords, Scale scale, Style style, IPoint loc,
      int xAdvance) {
    p.setKey(scale);
    int x = loc.x;
    int y = loc.y;
    for (Chord c : chords) {
      p.plotChord(c, style, new IPoint(x, y));
      x += xAdvance;
    }
  }

  private List<Chord> randomChords(int count) {
    List<Chord> chords = arrayList();

    while (chords.size() < count) {

      int[] ci = biasedSample(nashvilleNumberSet().length, count);

      for (int i = 0; chords.size() < count; i++) {
        Chord.Builder c;
        int mainChord = -1;

        mainChord = nashvilleNumberSet()[ci[i]];

        if (random().nextInt(30) < 10) {
          int auxChord;
          do {
            auxChord = random().nextInt(7) + 1;
          } while (auxChord == mainChord);
          c = chord(mainChord, auxChord);
        } else
          c = chord(mainChord);
        chords.add(c.build());
      }

      fixAdjacentPairs(chords);
    }
    return chords;
  }

  /**
   * Swap chords to try to eliminate nearby duplicates; remove chords from list
   * if necessary
   */
  private void fixAdjacentPairs(List<Chord> chords) {
    int discardPoint = -1;

    // We try to avoid repeating a chord that appeared either one or two slots before it
    for (int j = 0; j < chords.size() - 1; j++) {

      Chord c1 = chords.get(j);
      Chord c0 = c1;
      if (j > 0)
        c0 = chords.get(j - 1);

      int k = j + 1;
      while (k < chords.size()) {
        Chord c2 = chords.get(k);
        if (c1.number() != c2.number() && c0.number() != c2.number())
          break;
        k++;
      }
      if (k == j + 1)
        continue;
      if (k == chords.size()) {
        discardPoint = j + 1;
        break;
      }

      {
        Chord ca = chords.get(j + 1);
        Chord cb = chords.get(k);
        chords.set(j + 1, cb);
        chords.set(k, ca);
      }
    }

    if (discardPoint >= 0) {
      removeAllButFirstN(chords, discardPoint);
    }
  }

  /**
   * Construct a random set of integers 0 <= n < range, with lower values
   * occurring more frequently
   * 
   * @param range
   * @param count
   * @return
   */
  private int[] biasedSample(int range, int count) {
    int[] result = new int[count];
    float scale = 1.7f;
    final float shift = 0.15f;
    int sum = 0;
    while (sum < count) {
      int k = (int) Math.floor((random().nextFloat() * random().nextFloat() - shift) * scale * range);
      if (k < 0 || k >= range)
        continue;
      result[sum++] = k;
    }
    return result;
  }

  private Random random() {
    if (mRandom == null) {
      int seed = mConfig.seed();
      if (seed <= 0)
        seed = 1 + (((int) System.currentTimeMillis()) & 0xffff);
      mRandom = new Random(seed);
    }
    return mRandom;
  }

  /**
   * Get set of Nashville Numbers to construct random sequence from, ordered by
   * decreasing approximate frequency
   */
  private int[] nashvilleNumberSet() {
    if (mNashvilleNumberSet == null) {
      String chordNumbersExpression = mConfig.chordNumbers();
      chordNumbersExpression = ifNullOrEmpty(chordNumbersExpression, "4 5 6 2 7 3 1");
      IntArray.Builder ints = IntArray.newBuilder();
      for (String numberExpr : split(chordNumbersExpression, ' ')) {
        if (nullOrEmpty(numberExpr))
          continue;
        int chordNumber = Integer.parseInt(numberExpr);
        checkArgument(chordNumber >= 1 && chordNumber <= 7, "Not a legal Nashville chord number:",
            chordNumber);
        ints.add(chordNumber);
        mNashvilleNumberSet = ints.array();
      }
    }
    return mNashvilleNumberSet;
  }

  private SongConfig mConfig;
  private File mSourceFile;
  private Random mRandom;
  private int[] mNashvilleNumberSet;

}
