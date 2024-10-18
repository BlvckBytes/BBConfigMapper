package me.blvckbytes.bbconfigmapper.preprocessor;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class KeyValueLine implements PreProcessorLine {

  public final String content;
  public final String key;
  public final String value;
  public final List<CommentLine> attachedComments;

  // This line-number won't be synchronized when migrating - it's only supposed to be
  // accessed on the file migrating *from*, as to calculate reference-distance metrics
  public final int lineNumberAsRead;

  public @Nullable KeyValueLine previous;
  public @Nullable KeyValueLine next;

  public boolean hadBlankBefore;
  public boolean hadBlankAfter;

  public KeyValueLine(
    String content, String key, String value,
    List<CommentLine> attachedComments,
    int lineNumberAsRead
  ) {
    this.content = content;
    this.key = key;
    this.value = value;
    this.attachedComments = attachedComments;
    this.lineNumberAsRead = lineNumberAsRead;
  }

  @Override
  public void append(Writer writer) throws IOException {
    for (var attachedComment : attachedComments) {
      attachedComment.append(writer);
      writer.write('\n');
    }

    writer.write(content);
  }
}
