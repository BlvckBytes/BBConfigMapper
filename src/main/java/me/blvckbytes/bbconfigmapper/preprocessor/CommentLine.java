package me.blvckbytes.bbconfigmapper.preprocessor;

import java.io.IOException;
import java.io.Writer;

public class CommentLine implements PreProcessorLine {

  private final String indent;
  public final String value;

  public CommentLine(String indent, String value) {
    this.indent = indent;
    this.value = value;
  }

  @Override
  public void append(Writer writer) throws IOException {
    writer.write(indent);
    writer.write('#');
    writer.write(value);
  }
}
