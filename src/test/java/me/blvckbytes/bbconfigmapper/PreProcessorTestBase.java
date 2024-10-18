package me.blvckbytes.bbconfigmapper;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class PreProcessorTestBase {

  @FunctionalInterface
  protected interface UnsafeConsumer<T> {
    void accept(T value) throws Exception;
  }

  protected static final Logger logger = Logger.getAnonymousLogger();

  protected String captureOutput(UnsafeConsumer<Writer> handler) throws Exception {
    var stringWriter = new StringWriter();
    handler.accept(stringWriter);
    return stringWriter.toString();
  }

  protected String getFileContents(String filePath) throws IOException {
    try (
      var inputStream = getClass().getResourceAsStream(filePath);
    ) {
      assert inputStream != null;
      return new String(inputStream.readAllBytes());
    }
  }

  protected void assertLinesEqual(String expected, String actual) {
    try {
      var expectedLines = expected.split("\n");
      var actualLines = actual.split("\n");
      var minLength = Math.min(expectedLines.length, actualLines.length);

      for (var i = 0; i < minLength; ++i)
        assertEquals(expectedLines[i], actualLines[i], "Mismatch on line " + (i + 1));

      if (actualLines.length != expectedLines.length)
        throw new AssertionError("Expected " + actualLines.length + " lines but got " + expectedLines.length);
    } catch (AssertionError error) {
      System.out.println("----------------[Expected]----------------");
      System.out.println(expected);
      System.out.println("----------------[Actual]----------------");
      System.out.println(actual);

      throw error;
    }
  }
}
