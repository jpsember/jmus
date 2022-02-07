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
import jmus.gen.MainConfig;
import jmus.gen.Scale;
import jmus.gen.Song;
import js.app.AppOper;
import js.data.DataUtil;
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
  public MainConfig defaultArgs() {
    return MainConfig.DEFAULT_INSTANCE;
  }

  @Override
  public void perform() {
    mConfig = config();

    if (alert("experiment")) {
      experiment();
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
    p.render(song, scale, mConfig.style());
    File outFile = mConfig.output();
    if (Files.empty(outFile))
      outFile = mSourceFile;
    outFile = Files.setExtension(outFile, "png");
    p.generateOutputFile(outFile);
    pr("...done");
  }

  private MainConfig mConfig;
  private File mSourceFile;

  private static final Paint PAINT_SCALE = Paint.newBuilder().color(128, 128, 128).font(FONT_BOLD, 1.2f)
      .build();

  private void experiment() {

    PagePlotter p = new PagePlotter();
    Graphics2D g = p.graphics();
    //rect(g, PAGE_CONTENT);

    Style style = style(0);
    int xAdvance = style.meanChordWidthPixels + style.chordPadX + 8;

    int y = PAGE_CONTENT.y;

    int indent = PIXELS_PER_INCH * 1;
    int ysep = style.chordHeight + style.spacingBetweenSections;

    int chordsPerRow = 16;
    List<Chord> chords = randomChords(chordsPerRow);

    int x = PAGE_CONTENT.x + indent;
    plotChords(p, chords, null, style, new IPoint(x, y), xAdvance);

    y += ysep * 1.5;

    List<Scale> scales = buildScaleList();

    for (Scale scale : scales) {
      String n = scale.name();
      n = n.replace('-', ' ');
      n = DataUtil.capitalizeFirst(n) + ":";

      {
        PAINT_SCALE.apply(g);
        FontMetrics f = g.getFontMetrics();
        int tx = x - f.stringWidth(n) - style.chordPadX * 2;

        g.drawString(n, tx, y + f.getAscent());

      }

      plotChords(p, chords, scale, style, new IPoint(x, y), xAdvance);
      y += ysep;
    }
    File outFile = new File("samples/experiment.png");
    p.generateOutputFile(outFile);
    pr("...done");
  }

  private List<Scale> buildScaleList() {
    List<Scale> scales = arrayList();
    String scaleExp = mConfig.scales();
    if (nullOrEmpty(scaleExp)) {
      scaleExp = "c g f e-flat";
    }
    for (String s : split(scaleExp, ' ')) {
      scales.add(scale(s));
    }
    return scales;
  }

  private void plotChords(PagePlotter p, List<Chord> chords, Scale scale, Style style, IPoint loc,
      int xAdvance) {
    int x = loc.x;
    int y = loc.y;
    for (Chord c : chords) {
      p.plotChord(c, scale, style, new IPoint(x, y));
      x += xAdvance;
    }
  }

  //
  //  private int randChord() {
  //  
  //  }
  //  
  private List<Chord> randomChords(int count) {
    List<Chord> chords = arrayList();

    int[] ci = biasedSample(chordNumbers().length, count);

    for (int i = 0; i < count; i++) {

      Chord.Builder c;
      int mainChord = -1;

      mainChord = chordNumbers()[ci[i]];

      todo("add split chords");
      //      
      //      if (false && random().nextInt(4) == 2) {
      //        int auxChord;
      //        do {
      //          auxChord = random().nextInt(7) + 1;
      //        } while (auxChord == mainChord);
      //        c = chord(mainChord, auxChord);
      //      } else
      //        
        c = chord(mainChord);
      chords.add(c.build());
    }
    return chords;

  }

  private int[] biasedSample(int range, int pop) {
    int[] result = new int[pop];
    float scale = 1.7f;
    final float shift = 0.15f;
    int sum = 0;
    while (sum < pop) {
      //float q = (random().nextFloat() * random().nextFloat() - shift) * scale;
      int k = (int) Math.floor((random().nextFloat() * random().nextFloat() - shift) * scale * range);
      if (k < 0 || k >= range)
        continue;
      result[sum++] = k;
    }
    return result;
  }

  private Random random() {
    if (mRand == null) {
      int seed = mConfig.seed();
      if (seed <= 0)
        seed = 1 + (((int) System.currentTimeMillis()) & 0xffff);
      mRand = new Random(seed);
    }
    return mRand;
  }

  private Random mRand;

  private int[] chordNumbers() {
    if (mChordNumbers == null) {
      String chordExp = mConfig.chordNumbers();
      chordExp = ifNullOrEmpty(chordExp, "4 5 6 2 7 3 1");
      List<String> expr = split(chordExp, ' ');
      IntArray.Builder cn = IntArray.newBuilder();
      for (String x : expr) {
        cn.add(Integer.parseInt(x));
        mChordNumbers = cn.array();
      }
    }
    return mChordNumbers;
  }

  private int[] mChordNumbers;
}
