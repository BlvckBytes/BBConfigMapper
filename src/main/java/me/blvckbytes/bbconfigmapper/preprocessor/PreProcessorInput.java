package me.blvckbytes.bbconfigmapper.preprocessor;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.*;

public class PreProcessorInput {

  private static final int LINE_BUFFER_SIZE = 1024;

  private final Map<String, KeyValueLine> parsedLineByKey;
  private final List<PreProcessorLine> lines;

  public PreProcessorInput() {
    this.parsedLineByKey = new HashMap<>();
    this.lines = new ArrayList<>();
  }

  public Collection<String> getKeys() {
    return Collections.unmodifiableSet(this.parsedLineByKey.keySet());
  }

  public @Nullable String getValue(String key) {
    var parsedLine = parsedLineByKey.get(key.toLowerCase());

    if (parsedLine == null)
      return null;

    return parsedLine.value;
  }

  public void load(Reader reader) throws IOException {
    parsedLineByKey.clear();
    lines.clear();

    char[] lineBuffer = new char[LINE_BUFFER_SIZE];
    int lineBufferIndex = 0;
    int lineCounter = 0;
    var readSuccess = true;

    KeyValueLine previousKeyValueLine = null;
    var consecutiveComments = new ArrayList<CommentLine>();

    PreProcessorLine previousLine = null;

    while (readSuccess) {
      readSuccess = reader.read(lineBuffer, lineBufferIndex, 1) != -1;

      if (
        // EOS reached, and line-buffer is not empty
        (!readSuccess && lineBufferIndex != 0) ||
        // Newline encountered
        (lineBuffer[lineBufferIndex] == '\n')
      ) {
        var lineContent = new String(lineBuffer, 0, lineBufferIndex);
        var currentLine = parseLine(lineContent, ++lineCounter, consecutiveComments);

        if (currentLine instanceof CommentLine commentLine)
          consecutiveComments.add(commentLine);
        else {
          // Dangling comments, not attached to any key
          if (!(currentLine instanceof KeyValueLine))
            this.lines.addAll(consecutiveComments);

          consecutiveComments.clear();
        }

        if (currentLine instanceof KeyValueLine keyValueLine) {
          this.lines.add(keyValueLine);
          this.parsedLineByKey.put(keyValueLine.key.toLowerCase(), keyValueLine);

          if (previousKeyValueLine != null) {
            keyValueLine.previous = previousKeyValueLine;
            previousKeyValueLine.next = keyValueLine;
          }

          previousKeyValueLine = keyValueLine;

          if (previousLine instanceof BlankLine)
            keyValueLine.hadBlankBefore = true;
        }

        else if (currentLine instanceof BlankLine) {
          this.lines.add(currentLine);

          if (previousLine instanceof KeyValueLine keyValueLine)
            keyValueLine.hadBlankAfter = true;
        }

        previousLine = currentLine;
        lineBufferIndex = 0;
        continue;
      }

      ++lineBufferIndex;
    }

    // Dangling comments at the very end
    if (!consecutiveComments.isEmpty())
      this.lines.addAll(consecutiveComments);
  }

  private int indexOfWhitespace(String input, int fromIndex, boolean isWhitespace) {
    for (var i = fromIndex; i < input.length(); ++i) {
      if ((input.charAt(i) == ' ') == isWhitespace)
        return i;
    }

    return -1;
  }

  private PreProcessorLine parseLine(String line, int lineNumber, List<CommentLine> previousConsecutiveComments) {
    int keyBegin;

    if ((keyBegin = indexOfWhitespace(line, 0, false)) < 0)
      return new BlankLine(line);

    if (line.charAt(keyBegin) == '#') {
      return new CommentLine(
        line.substring(0, keyBegin),
        keyBegin == line.length() - 1
          ? ""
          : line.substring(keyBegin + 1)
      );
    }

    int keyEnd = indexOfWhitespace(line, keyBegin + 1, true);

    if (keyEnd < 0)
      keyEnd = line.length() - 1;
    else
      --keyEnd;

    var key = line.substring(keyBegin, keyEnd + 1);

    for (var keyIndex = 0; keyIndex < key.length(); ++keyIndex) {
      var keyChar = key.charAt(keyIndex);

      if (!(
        (keyChar >= 'A' && keyChar <= 'Z') ||
        (keyChar >= '0' && keyChar <= '9') ||
        keyChar == '-'
      ))
        throw new PreProcessorInputException(lineNumber, InputConflict.INVALID_KEY_CHARACTERS);
    }

    var valueBegin = indexOfWhitespace(line, keyEnd + 1, false);

    if (valueBegin < 0)
      throw new PreProcessorInputException(lineNumber, InputConflict.BLANK_VALUE);

    var valueEnd = line.length() - 1;
    var valueLength = valueEnd - valueBegin + 1;

    if (valueLength > 1 && line.charAt(valueBegin) == '\\') {
      var nextChar = line.charAt(valueBegin + 1);

      if (
        // \<blank> -> <blank>
        nextChar == ' ' ||
        // \\<blank> -> \<blank>
        valueLength > 2 && nextChar == '\\' && line.charAt(valueBegin + 2) == ' '
      )
        ++valueBegin;
    }

    if (valueLength > 1 && line.charAt(valueEnd) == '\\') {
      var previousChar = line.charAt(valueEnd - 1);

      if (
        // <blank>\ -> <blank>
        previousChar == ' ' ||
        // <blank>\\ -> <blank>\
        valueLength > 2 && previousChar == '\\' && line.charAt(valueEnd - 2) == ' '
      )
        --valueEnd;
    }

    var value = line.substring(valueBegin, valueEnd + 1);
    return new KeyValueLine(line, key, value, new ArrayList<>(previousConsecutiveComments), lineNumber);
  }

  private @Nullable KeyValueLine checkExistenceByWalkingLinks(KeyValueLine line, boolean previous) {
    KeyValueLine currentLine = line;

    while ((currentLine = (previous ? currentLine.previous : currentLine.next)) != null) {
      if (!this.parsedLineByKey.containsKey(currentLine.key.toLowerCase()))
        continue;

      return currentLine;
    }

    return null;
  }

  private boolean extendMissingKey(KeyValueLine missingLine) {
    if (this.parsedLineByKey.containsKey(missingLine.key.toLowerCase()))
      return false;

    this.parsedLineByKey.put(missingLine.key.toLowerCase(), missingLine);

    KeyValueLine referencePrior = checkExistenceByWalkingLinks(missingLine, true);
    KeyValueLine referenceAfter = checkExistenceByWalkingLinks(missingLine, false);

    if (referencePrior == null && referenceAfter == null) {
      // Space them out, as to indicate that they do not belong to another paragraph of keys
      this.lines.add(new BlankLine(""));
      this.lines.add(missingLine);
      return true;
    }

    // Near relative to the file migrating to, as that's the only metric we have
    KeyValueLine nearestReference;

    if (referencePrior == null)
      nearestReference = referenceAfter;
    else if (referenceAfter == null)
      nearestReference = referencePrior;
    else {
      var referencePriorDistance = Math.abs(missingLine.lineNumberAsRead - referencePrior.lineNumberAsRead);
      var referenceAfterDistance = Math.abs(missingLine.lineNumberAsRead - referenceAfter.lineNumberAsRead);
      nearestReference = referencePriorDistance <= referenceAfterDistance ? referencePrior : referenceAfter;
    }

    for (var lineIndex = 0; lineIndex < this.lines.size(); ++lineIndex) {
      if (!(lines.get(lineIndex) instanceof KeyValueLine keyValueLine))
        continue;

      if (!(keyValueLine.key.equals(nearestReference.key)))
        continue;

      var addIndex = lineIndex + (nearestReference == referencePrior ? 1 : 0);

      this.lines.add(addIndex, missingLine);

      // Also add blank lines if not yet existing, to signal paragraphs

      if (missingLine.hadBlankBefore) {
        if (!(addIndex > 0 && this.lines.get(addIndex - 1) instanceof BlankLine))
          this.lines.add(addIndex, new BlankLine(""));
      }

      if (missingLine.hadBlankAfter) {
        if (!(addIndex + 1 < this.lines.size() && this.lines.get(addIndex + 1) instanceof BlankLine))
          this.lines.add(addIndex + 1, new BlankLine(""));
      }

      return true;
    }

    throw new IllegalStateException("Could not find a previously located reference-line: " + nearestReference.content);
  }

  private @Nullable EnvironmentComment tryParseEnvironmentComment(CommentLine line) {
    var hyphenIndex = line.value.indexOf('-');

    if (hyphenIndex < 0)
      return null;

    var nameBeginIndex = indexOfWhitespace(line.value, hyphenIndex + 1, false);

    if (nameBeginIndex < 0)
      return null;

    var colonIndex = line.value.indexOf(':', nameBeginIndex + 1);

    if (colonIndex < 0)
      return null;

    var typeBeginIndex = indexOfWhitespace(line.value, colonIndex + 1, false);

    if (typeBeginIndex < 0)
      return null;

    return new EnvironmentComment(
      line.value.substring(nameBeginIndex, colonIndex),
      line.value.substring(typeBeginIndex).stripTrailing(),
      line
    );
  }

  private void updateEnvironmentComments(KeyValueLine otherLine) {
    var localLine = parsedLineByKey.get(otherLine.key.toLowerCase());

    if (localLine == null)
      throw new IllegalStateException("Assumed local existence of key=" + otherLine.key);

    var otherEnvironmentComments = new LinkedHashSet<EnvironmentComment>();

    for (var otherComment : otherLine.attachedComments) {
      var otherEnvironmentComment = tryParseEnvironmentComment(otherComment);

      if (otherEnvironmentComment != null)
        otherEnvironmentComments.add(otherEnvironmentComment);
    }

    for (var commentIndex = localLine.attachedComments.size() - 1; commentIndex >= 0; --commentIndex) {
      var localComment = localLine.attachedComments.get(commentIndex);
      var localEnvironmentComment = tryParseEnvironmentComment(localComment);

      if (localEnvironmentComment == null)
        continue;

      // This variable does no longer exist
      if (!otherEnvironmentComments.remove(localEnvironmentComment))
        localLine.attachedComments.remove(commentIndex);
    }

    // Add missing variables to the very bottom
    for (var otherEnvironmentComment : otherEnvironmentComments)
      localLine.attachedComments.add(otherEnvironmentComment.commentLine);
  }

  public int migrateTo(PreProcessorInput other) {
    var extendedKeys = 0;

    for (var otherLine : other.parsedLineByKey.values()) {
      if (extendMissingKey(otherLine)) {
        ++extendedKeys;
        continue;
      }

      updateEnvironmentComments(otherLine);
    }

    return extendedKeys;
  }

  public void save(Writer writer) throws IOException {
    for (var i = 0; i < lines.size(); ++i) {
      var line = lines.get(i);

      if (i != 0)
        writer.write('\n');

      line.append(writer);
    }
  }
}
