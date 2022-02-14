package jmus;

import static jmus.MusUtil.*;
import static js.base.Tools.*;

import java.io.File;

import jmus.gen.Accidental;
import jmus.gen.Chord;
import jmus.gen.ChordType;
import jmus.gen.MusicSection;
import jmus.gen.OptType;
import jmus.gen.SectionType;
import jmus.gen.Song;
import js.base.BaseObject;
import js.file.Files;
import js.parsing.Scanner;
import js.parsing.Token;

public class SongParser extends BaseObject {

  public SongParser(File sourceFile) {
    mSourceFile = sourceFile;
  }

  public Song parse() {
    mScanner = new Scanner(dfa(), Files.readString(mSourceFile));
    mScanner.setSourceDescription(mSourceFile.getName());

    mScanner.alertVerbose();

    while (mScanner.hasNext()) {

      if (consumeBreakTokens())
        continue;

      if (readIf(T_KEY)) {
        String keyString = readAndParseString();
        flushMusicSection();

        song().sections().add(MusicSection.newBuilder()//
            .type(SectionType.KEY).text(keyString) //
            .build());
        continue;
      }

      // If a string is found, assume it is 'text'
      //
      if (peekIf(T_STRING)) {
        parseText(SectionType.TEXT);
        continue;
      }

      if (readIf(T_TITLE)) {
        parseText(SectionType.TITLE);
        continue;
      }
      if (readIf(T_SUBTITLE)) {
        parseText(SectionType.SUBTITLE);
        continue;
      }
      if (readIf(T_TEXT)) {
        parseText(SectionType.TEXT);
        continue;
      }
      if (readIf(T_SMALLTEXT)) {
        parseText(SectionType.SMALL_TEXT);
        continue;
      }

      if (peekIf(T_BEATS)) {
        flushMusicSection();
        mBeatsPerBar = Integer.parseInt(chompPrefix(mScanner.read().text(), "beats:"));
        continue;
      }

      processPendingBreak();

      if (readIf(T_PAROP)) {
        int beatNumber = 0;
        while (!readIf(T_PARCL)) {
          Chord.Builder c = readScalarChord();
          c.beatNumber(beatNumber);
          beatNumber++;
          musicSection().chords().add(c.build());
        }
      } else {
        Chord.Builder c = readScalarChord();
        musicSection().chords().add(c.build());
      }
      setRecentPlotElementFlag();
    }

    flushMusicSection();
    // halt("parsed song:", INDENT, song());
    return song().build();
  }

  /**
   * Consume zero or more linefeeds or '\' tokens, and update the pending break
   * type
   * 
   * @return true if it found any such tokens
   */
  private boolean consumeBreakTokens() {
    boolean found = false;
    boolean joinFlag = false;
    {
      int crCount = 0;
      while (true) {
        if (readIf(T_CR)) {
          found = true;
          crCount++;
          mScanner.log("cr, count now:", crCount);
        } else if (readIf(T_BWD_SLASH)) {
          found = true;
          joinFlag = true;
          mScanner.log("backslash, join flag now true");
        } else
          break;
      }
      if (found) {
        if (!joinFlag) {
          mPendingBreakType = Math.min(crCount, 2);
        }
        mScanner.log("joinflag:", joinFlag, "crcount:", crCount, "pending break:", mPendingBreakType);
      }
    }
    return found;
  }

  private void parseText(SectionType type) {
    flushMusicSection();
    processPendingBreak();
    String s = mScanner.read(T_STRING).text();
    s = parseStringText(s);
    song().sections().add(MusicSection.newBuilder()//
        .type(type).text(s) //
        .build());
    setRecentPlotElementFlag();
  }

  private void setRecentPlotElementFlag() {
    mRecentPlotElement = true;
  }

  private String readAndParseString() {
    String s = mScanner.read(T_STRING).text();
    return parseStringText(s);
  }

  private String parseStringText(String s) {
    s = s.substring(1, s.length() - 1);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\\') {
        i++;
        c = s.charAt(i);
      }
      sb.append(c);
    }
    return sb.toString();
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

  private boolean peekIf(int tokenId) {
    return mScanner.hasNext() && mScanner.peek().id(tokenId);
  }

  private Song.Builder song() {
    if (mSongBuilder == null) {
      mSongBuilder = Song.newBuilder();
    }
    return mSongBuilder;
  }

  private boolean hasCurrentMusicSection() {
    return (mMusicSectionBuilder != null && !musicSection().chords().isEmpty());
  }

  private void flushMusicSection() {
    if (hasCurrentMusicSection()) {
      song().sections().add(musicSection().build());
      mMusicSectionBuilder = null;
    }
  }

  /**
   * Insert a pending line or paragraph break, if appropriate, in anticipation
   * of a new visual component
   */
  private void processPendingBreak() {
    if (mPendingBreakType == 0)
      return;
    if (mRecentPlotElement) {
      mRecentPlotElement = false;
      flushMusicSection();
      song().sections().add(mPendingBreakType == 2 ? PARAGRAPH_BREAK : LINE_BREAK);
    }
    mPendingBreakType = 0;
  }

  private MusicSection.Builder musicSection() {
    if (mMusicSectionBuilder == null)
      mMusicSectionBuilder = MusicSection.newBuilder().beatsPerBar(mBeatsPerBar)
          .type(SectionType.CHORD_SEQUENCE);
    return mMusicSectionBuilder;
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

  private static final MusicSection LINE_BREAK = MusicSection.newBuilder().type(SectionType.LINE_BREAK)
      .build();
  private static final MusicSection PARAGRAPH_BREAK = MusicSection.newBuilder()
      .type(SectionType.PARAGRAPH_BREAK).build();

  private File mSourceFile;

  private Scanner mScanner;
  private Song.Builder mSongBuilder;
  private MusicSection.Builder mMusicSectionBuilder;
  private int mBeatsPerBar;

  // 0: none 1: line 2: paragraph
  private int mPendingBreakType;

  // true if there is a previous plot element that a line or paragraph break can be separated from
  private boolean mRecentPlotElement;
}
