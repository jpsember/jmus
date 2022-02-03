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

import js.parsing.DFA;
import js.parsing.Scanner;
import js.parsing.Token;

import static jmus.Util.*;

import jmus.gen.Accidental;
import jmus.gen.Chord;
import jmus.gen.ChordType;
import jmus.gen.MainConfig;
import jmus.gen.MusicLine;
import jmus.gen.MusicSection;
import jmus.gen.OptType;
import jmus.gen.Scale;
import jmus.gen.Song;
import js.app.AppOper;
import js.app.CmdLineArgs;
import js.file.Files;

public class ExampleOper extends AppOper {

  /* private */ static final int T_WS = 0, T_CR = 1, T_STRING = 2, T_CHORD = 3;

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
  protected void processAdditionalArgs() {
    int count = 0;
    CmdLineArgs args = app().cmdLineArgs();
    while (args.hasNextArg()) {
      String arg = args.nextArg();

      switch (arg) {
      default: {
        switch (count) {
        case 0:
          mSourceFile = Files.absolute(new File(arg));
          break;
        default:
          throw badArg("extraneous argument:", arg);
        }
        count++;
      }
        break;
      }
    }
    args.assertArgsDone();
  }

  @Override
  public void perform() {
    if (Files.empty(mSourceFile))
      setError("Please specify a source file");

    mScanner = new Scanner(dfa(), Files.readString(mSourceFile));

    while (mScanner.hasNext()) {

      int crs = consumeCR();
      if (crs != 0) {
        flushMusicLine();
        if (crs == 2)
          flushMusicSection();
        continue;
      }

      Token t = mScanner.read(T_CHORD);
      Chord c = parseChord(t);
      musicLine().chords().add(c);
    }

    flushMusicLine();
    flushMusicSection();

    Scale scale = Util.scaleMap().scales().get("b-flat");
    
    String songText = renderSong(song(), scale);
    System.out.println(songText);

  }

  private Song.Builder song() {
    if (mSongBuilder == null) {
      mSongBuilder = Song.newBuilder();
    }
    return mSongBuilder;
  }

  private void flushMusicSection() {
    if (!musicSection().lines().isEmpty()) {
      song().sections().add(musicSection().build());
      mMusicSectionBuilder = null;
    }
  }

  private MusicSection.Builder musicSection() {
    if (mMusicSectionBuilder == null) {
      mMusicSectionBuilder = MusicSection.newBuilder();
    }
    return mMusicSectionBuilder;
  }

  private MusicLine.Builder musicLine() {
    if (mMusicLineBuilder == null) {
      mMusicLineBuilder = MusicLine.newBuilder();
    }
    return mMusicLineBuilder;
  }

  private void flushMusicLine() {
    if (!musicLine().chords().isEmpty()) {
      musicSection().lines().add(musicLine().build());
      mMusicLineBuilder = null;
    }
  }

  private int consumeCR() {
    int crCount = 0;
    while (mScanner.readIf(T_CR) != null) {
      crCount++;
    }
    return Math.min(crCount, 2);
  }

  private DFA dfa() {
    if (mDFA == null)
      mDFA = new DFA(Files.readString(this.getClass(), "tokens.dfa"));
    return mDFA;
  }

  private Chord parseChord(Token t) {
    try {
      Chord.Builder b = Chord.newBuilder();
      String s = t.text();
      int k = 0;
      char c;

      c = s.charAt(k);
      if (c == 'b' || c == '#') {
        b.accidental(c == 'b' ? Accidental.FLAT : Accidental.SHARP);
        k++;
      }

      c = s.charAt(k++);
      int num = 1 + (c - '1');
      checkArgument(num >= 1 && num <= 7);
      b.number(num);

      while (k < s.length()) {
        c = s.charAt(k++);
        switch (c) {
        default:
          throw badArg("unsupported character:", Character.toString(c));
        case '-':
          b.type(ChordType.MINOR);
          break;
        case '\'':
          b.type(ChordType.DIMINISHED);
          break;
        case '+':
          b.type(ChordType.AUGMENTED);
          break;
        case '2':
          b.optType(OptType.TWO);
          break;
        case '4':
          b.optType(OptType.FOUR);
          break;
        case '5':
          b.optType(OptType.FIVE);
          break;
        case '6':
          b.optType(OptType.SIX);
          break;
        case '7':
          b.optType(OptType.SEVEN);
          break;
        case '9':
          b.optType(OptType.NINE);
          break;
        }
      }
      return b.build();
    } catch (Throwable th) {
      throw t.fail("Trouble parsing chord");
    }
  }

  private static final int CHORD_COLUMN_SIZE = 5;

  private String renderSong(Song song, Scale scale) {
    StringBuilder lineBuilder = new StringBuilder();

    for (MusicSection section : song.sections()) {
      lineBuilder.append("\n");
      for (MusicLine line : section.lines()) {
        int cursor = lineBuilder.length();
        int chordNum = -1;
        for (Chord chord : line.chords()) {
          chordNum++;
          tab(lineBuilder, cursor + chordNum * CHORD_COLUMN_SIZE);
          String chordStr = renderChord(chord, scale, null).toString();
          lineBuilder.append(chordStr);
        }
        lineBuilder.append("\n");
      }
    }

    return lineBuilder.toString();
  }

  private DFA mDFA;
  private File mSourceFile;
  private Scanner mScanner;
  private Song.Builder mSongBuilder;
  private MusicSection.Builder mMusicSectionBuilder;
  private MusicLine.Builder mMusicLineBuilder;

}
