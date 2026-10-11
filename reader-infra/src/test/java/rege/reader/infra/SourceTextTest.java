package rege.reader.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SourceTextTest {
    @Test
    void emptySourceHasAnOrdinaryEofPosition() {
        SourceText text = new SourceText("");
        assertEquals(Position.start(), text.positionAt(0));
        assertEquals(0, text.offsetAtCodePoint(0));
        assertEquals(0, text.length());
        assertEquals(0, text.codePointLength());
        assertEquals(Range.at(Position.start()), text.range(0, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    void lineEndingsShareTheSameLineAndColumnConvention(String newline) {
        String source = "ab" + newline + "cd" + newline;
        SourceText text = new SourceText(source);
        assertEquals(source, text.source());
        assertEquals(new Position(0, 0, 0), text.positionAt(0));
        assertEquals(new Position(0, 2, 2), text.positionAt(2));
        int secondLine = 2 + newline.length();
        assertEquals(new Position(1, 0, secondLine), text.positionAt(secondLine));
        assertEquals(new Position(1, 2, secondLine + 2), text.positionAt(secondLine + 2));
        assertEquals(new Position(2, 0, source.length()), text.positionAt(source.length()));
        Range multiline = text.range(1, secondLine + 1);
        assertEquals(new Position(0, 1, 1), multiline.start());
        assertEquals(new Position(1, 1, secondLine + 1), multiline.end());
        assertEquals(newline.length() + 2, multiline.length());
        assertFalse(multiline.contains(multiline.end()));
    }

    @Test
    void crlfInteriorStaysAtThePrecedingLineEnd() {
        SourceText text = new SourceText("a\r\nb");
        assertEquals(new Position(0, 1, 1), text.positionAt(1));
        assertEquals(new Position(0, 1, 2), text.positionAt(2));
        assertEquals(new Position(1, 0, 3), text.positionAt(3));
    }

    @Test
    void lineBoundsExcludeLineEndingsAndLspInputsClamp() {
        SourceText text = new SourceText("a😀\r\nb\rc\n");
        assertEquals(4, text.lineCount());
        assertArrayEquals(new int[] {0, 5, 7, 9},
            java.util.stream.IntStream.range(0, 4).map(text::lineStartOffset).toArray());
        assertArrayEquals(new int[] {3, 6, 8, 9},
            java.util.stream.IntStream.range(0, 4).map(text::lineEndOffset).toArray());
        assertEquals(3, text.offsetAt(0, Integer.MAX_VALUE));
        assertEquals(6, text.offsetAt(1, 42));
        assertEquals(9, text.offsetAt(42, 0));
        assertEquals(9, text.offsetAt(3, 1));
        assertEquals(2, text.offsetAt(0, 2));
        assertThrows(IllegalArgumentException.class, () -> text.offsetAt(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> text.offsetAt(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> text.lineStartOffset(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> text.lineEndOffset(4));
    }

    @Test
    void mixedNewlinesAndEmptyLinesAreIndexedWithoutNormalization() {
        SourceText text = new SourceText("\r\n\r\n\n\rx");
        assertEquals(new Position(1, 0, 2), text.positionAt(2));
        assertEquals(new Position(2, 0, 4), text.positionAt(4));
        assertEquals(new Position(3, 0, 5), text.positionAt(5));
        assertEquals(new Position(4, 0, 6), text.positionAt(6));
        assertEquals(new Position(4, 1, 7), text.positionAt(7));
    }

    @Test
    void supplementaryCharactersCountAsTwoUtf16Units() {
        SourceText text = new SourceText("a😀\r\n𝄞z");
        int[] expectedOffsets = {0, 1, 3, 4, 5, 7, 8};
        assertEquals(6, text.codePointLength());
        assertEquals(8, text.length());
        for (int index = 0; index < expectedOffsets.length; index++) {
            assertEquals(expectedOffsets[index], text.offsetAtCodePoint(index));
        }
        assertEquals(new Position(0, 3, 3), text.positionAt(text.offsetAtCodePoint(2)));
        assertEquals(new Position(1, 2, 7), text.positionAt(text.offsetAtCodePoint(5)));
        assertEquals(2, text.range(1, 3).length());
    }

    @Test
    void utf16BoundariesWithinSurrogatePairsRemainAddressable() {
        SourceText text = new SourceText("😀");
        assertEquals(new Position(0, 1, 1), text.positionAt(1));
        assertEquals(new Position(0, 2, 2), text.positionAt(2));
    }

    @Test
    void unpairedSurrogatesEachOccupyOneCodeUnitAndCodePoint() {
        SourceText text = new SourceText("\ud83dX\ude00");
        assertEquals(3, text.codePointLength());
        for (int index = 0; index <= 3; index++) {
            assertEquals(index, text.offsetAtCodePoint(index));
        }
    }

    @Test
    void rejectsInvalidOffsetsAndReversedRanges() {
        SourceText text = new SourceText("😀");
        assertThrows(IndexOutOfBoundsException.class, () -> text.positionAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> text.positionAt(3));
        assertThrows(IndexOutOfBoundsException.class, () -> text.offsetAtCodePoint(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> text.offsetAtCodePoint(2));
        assertThrows(IllegalArgumentException.class, () -> text.range(2, 1));
        assertThrows(NullPointerException.class, () -> new SourceText(null));
    }

    @Test
    void peekableUsesExactlyTheSameCoordinatesAtEveryOffset() {
        SourceText text = new SourceText("a😀\r\nb\rc\n");
        Peekable input = new Peekable(text.source());
        for (int offset = 0; offset <= text.length(); offset++) {
            assertEquals(text.positionAt(offset), input.position());
            if (input.hasNext()) {
                assertEquals(text.source().charAt(offset), input.next());
            }
        }
        assertFalse(input.hasNext());
    }
}
