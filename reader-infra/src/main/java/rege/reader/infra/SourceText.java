package rege.reader.infra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable source text and its coordinate index, independent of any syntax tree.
 * Lines and columns are zero-based; columns and absolute offsets count UTF-16
 * code units. LF, CRLF, and CR are line endings. Ranges have exclusive ends.
 *
 * <p>A position inside a CRLF pair has the preceding line's end column; its
 * absolute offset is retained. The next line starts after the entire pair.
 * EOF is an ordinary position and a trailing line ending creates an empty line.
 *
 * <p>Parsers whose indices count Unicode code points (for example ANTLR's
 * code-point streams) must convert them with {@link #offsetAtCodePoint(int)}.
 */
public final class SourceText {
    private final String source;
    private final int[] lineStarts;
    private final int[] codePointOffsets;

    /**
     * Index the given source text.
     * @param source the original text, without newline normalization
     */
    public SourceText(String source) {
        this.source = Objects.requireNonNull(source, "source cannot be null");
        var starts = new ArrayList<Integer>();
        starts.add(0);
        for (int offset = 0; offset < source.length(); offset++) {
            char ch = source.charAt(offset);
            if (ch == '\r') {
                if (offset + 1 < source.length() && source.charAt(offset + 1) == '\n') {
                    offset++;
                }
                starts.add(offset + 1);
            } else if (ch == '\n') {
                starts.add(offset + 1);
            }
        }
        lineStarts = starts.stream().mapToInt(Integer::intValue).toArray();
        codePointOffsets = new int[source.codePointCount(0, source.length()) + 1];
        int offset = 0;
        for (int index = 0; index < codePointOffsets.length - 1; index++) {
            codePointOffsets[index] = offset;
            offset += Character.charCount(source.codePointAt(offset));
        }
        codePointOffsets[codePointOffsets.length - 1] = source.length();
    }

    /**
     * Get the original source text.
     * @return the original source text
     */
    public String source() {
        return source;
    }

    /**
     * Get the source length in UTF-16 code units.
     * @return the source length in UTF-16 code units
     */
    public int length() {
        return source.length();
    }

    /**
     * Get the source length in Unicode code points.
     * @return the source length in Unicode code points
     */
    public int codePointLength() {
        return codePointOffsets.length - 1;
    }

    /**
     * Count the lines, including an empty trailing line.
     * @return the number of lines
     */
    public int lineCount() {
        return lineStarts.length;
    }

    /**
     * Get the UTF-16 offset at the start of a line.
     * @param line a zero-based line number
     * @return the line's start offset
     * @throws IndexOutOfBoundsException if the line does not exist
     */
    public int lineStartOffset(int line) {
        Objects.checkIndex(line, lineStarts.length);
        return lineStarts[line];
    }

    /**
     * Get the exclusive UTF-16 end offset of a line, excluding its line ending.
     * @param line a zero-based line number
     * @return the line's end offset
     * @throws IndexOutOfBoundsException if the line does not exist
     */
    public int lineEndOffset(int line) {
        Objects.checkIndex(line, lineStarts.length);
        if (line + 1 == lineStarts.length) {
            return source.length();
        }
        int end = lineStarts[line + 1] - 1;
        if (source.charAt(end) == '\n' && end > lineStarts[line]
                && source.charAt(end - 1) == '\r') {
            end--;
        }
        return end;
    }

    /**
     * Convert an LSP-style position to a UTF-16 offset. Columns past the line's
     * text are clamped to its end; lines past the document are clamped to EOF.
     * @param line a zero-based line number
     * @param column a zero-based UTF-16 column
     * @return the corresponding UTF-16 offset
     * @throws IllegalArgumentException for a negative line or column
     */
    public int offsetAt(int line, int column) {
        if (line < 0 || column < 0) {
            throw new IllegalArgumentException("line and column must be >= 0");
        }
        if (line >= lineStarts.length) {
            return source.length();
        }
        return lineStarts[line] + Math.min(column, lineEndOffset(line) - lineStarts[line]);
    }

    /**
     * Convert a Unicode code-point offset to a UTF-16 offset.
     * @param codePointOffset a code-point boundary, from zero through EOF
     * @return the corresponding UTF-16 offset
     * @throws IndexOutOfBoundsException if the offset is outside the source
     */
    public int offsetAtCodePoint(int codePointOffset) {
        Objects.checkIndex(codePointOffset, codePointOffsets.length);
        return codePointOffsets[codePointOffset];
    }

    /**
     * Locate a UTF-16 offset in the original text.
     * @param offset a UTF-16 offset, from zero through EOF
     * @return its zero-based position
     * @throws IndexOutOfBoundsException if the offset is outside the source
     */
    public Position positionAt(int offset) {
        Objects.checkIndex(offset, source.length() + 1);
        int line = Arrays.binarySearch(lineStarts, offset);
        if (line < 0) {
            line = -line - 2;
        }
        int end = lineEndOffset(line);
        return new Position(line, Math.min(offset, end) - lineStarts[line], offset);
    }

    /**
     * Create a half-open range from UTF-16 offsets.
     * @param startOffset inclusive start offset
     * @param endOffset exclusive end offset
     * @return the corresponding source range
     */
    public Range range(int startOffset, int endOffset) {
        return new Range(positionAt(startOffset), positionAt(endOffset));
    }
}
