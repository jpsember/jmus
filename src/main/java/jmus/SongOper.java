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

import java.io.File;
import java.util.List;
import java.util.Random;

import static jmus.MusUtil.*;

import jmus.gen.Chord;
import jmus.gen.MainConfig;
import jmus.gen.Scale;
import jmus.gen.Song;
import js.app.AppOper;
import js.file.Files;
import js.geometry.IPoint;

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

  private void experiment() {
    PagePlotter p = new PagePlotter();
    rect(p.graphics(), PAGE_CONTENT);

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

    for (Scale scale : buildScaleList()) {

      //  Scale scale = scale("b-flat");
      plotChords(p, chords, scale, style, new IPoint(x, y), xAdvance);
      y += ysep;
    }
    File outFile = new File("samples/experiment.png");
    p.generateOutputFile(outFile);
    pr("...done");
  }

  private List<Scale> buildScaleList() {
    List<Scale> scales = arrayList();
    scales.addAll(scaleMap().scales().values());
    return scales;
  }

  private void plotChords(PagePlotter p, List<Chord> chords, Scale scale, Style style, IPoint loc,
      int xAdvance) {
    int x = loc.x;
    int y = loc.y;
    for (Chord c : chords) {
      p.plotChord(c, scale, style, new IPoint(x, y));
      //rect(p.graphics(), new IRect(x, y, xAdvance - 2, style.chordHeight));
      x += xAdvance;
    }
  }

  private List<Chord> randomChords(int count) {
    List<Chord> chords = arrayList();

    for (int i = 0; i < count; i++) {

      Chord.Builder c;
      int mainChord = -1;

      // Don't reuse a main chord if it was used recently
      //
      final int uniqueRecentCount = 4;
      while (true) {
        mainChord = random().nextInt(7) + 1;
        boolean found = false;
        for (int k = Math.max(0, chords.size() - uniqueRecentCount); k < chords.size(); k++) {
          if (chords.get(k).number() == mainChord) {
            found = true;
            break;
          }
        }
        if (!found)
          break;
      }

      if (random().nextInt(4) == 2) {
        int auxChord;
        do {
          auxChord = random().nextInt(7) + 1;
        } while (auxChord == mainChord);
        c = chord(mainChord, auxChord);
      } else
        c = chord(mainChord);
      chords.add(c.build());
    }
    return chords;

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
}
