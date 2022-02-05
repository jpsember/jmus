package jmus;

import static jmus.MusUtil.*;
import static js.base.Tools.*;

import java.io.File;

import jmus.gen.Accidental;
import jmus.gen.Chord;
import jmus.gen.ChordType;
import jmus.gen.MusicLine;
import jmus.gen.MusicSection;
import jmus.gen.OptType;
import jmus.gen.Song;
import js.base.BaseObject;
import js.file.Files;
import js.parsing.Scanner;
import js.parsing.Token;

public class SongParser extends BaseObject {

  // TODO: add song title
  // TODO: add auxilliary info (or arbitrary text), e.g. time signature, author, section title, section footer

  public SongParser(File sourceFile) {
    mSourceFile = sourceFile;
  }

  public Song parse() {

    mScanner = new Scanner(dfa(), Files.readString(mSourceFile));
    mScanner.setSourceDescription(mSourceFile.getName());
    mScanner.alertVerbose();

    while (mScanner.hasNext()) {

      int crs = consumeCR();
      if (crs != 0) {
        flushMusicLine();
        if (crs == 2)
          flushMusicSection();
        continue;
      }

      if (readIf(T_PAROP)) {
        int beatNumber = 0;
        while (!readIf(T_PARCL)) {
          Chord.Builder c = readScalarChord();
          c.beatNumber(beatNumber);
          beatNumber++;
          musicLine().chords().add(c.build());
        }

      } else {
        Chord.Builder c = readScalarChord();
        musicLine().chords().add(c.build());
      }
    }

    flushMusicLine();
    flushMusicSection();
    //pr("parsed:", INDENT, song());
    return song().build();
  }

  private Chord.Builder readScalarChord() {

    Chord.Builder c = parseChord();

    if (readIf(T_FWD_SLASH)) {
      Chord.Builder auxChord = parseChord();
      c.slashChord(auxChord);
    }
    return c;
  }

  private boolean readIf(int tokenId) {
    return null != mScanner.readIf(tokenId);
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

  private Chord.Builder parseChord() {
    if (readIf(T_PERIOD))
      return Chord.newBuilder().type(ChordType.BEAT);

    Token t = mScanner.read(T_CHORD);
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
      return b;
    } catch (Throwable th) {
      throw t.fail("Trouble parsing chord");
    }
  }

  private File mSourceFile;

  private Scanner mScanner;
  private Song.Builder mSongBuilder;
  private MusicSection.Builder mMusicSectionBuilder;
  private MusicLine.Builder mMusicLineBuilder;

}
