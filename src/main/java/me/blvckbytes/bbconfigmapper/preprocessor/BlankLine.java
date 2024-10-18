package me.blvckbytes.bbconfigmapper.preprocessor;

import java.io.IOException;
import java.io.Writer;

public record BlankLine(String content) implements PreProcessorLine {

  @Override
  public void append(Writer writer) throws IOException {
    writer.write(content);
  }
}
