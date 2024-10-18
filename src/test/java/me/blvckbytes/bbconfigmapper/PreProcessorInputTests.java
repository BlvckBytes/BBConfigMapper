package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.bbconfigmapper.preprocessor.InputConflict;
import me.blvckbytes.bbconfigmapper.preprocessor.PreProcessorInput;
import me.blvckbytes.bbconfigmapper.preprocessor.PreProcessorInputException;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

public class PreProcessorInputTests extends PreProcessorTestBase {

  @Test
  public void shouldParseInput() throws IOException {
    var processorInput = parseInputFile("/preprocessor/input.txt");

    assertEquals(7, processorInput.getKeys().size());
    assertEquals("Value of the first key", processorInput.getValue("FIRST_KEY"));
    assertEquals(" Value of the second key", processorInput.getValue("SECOND_KEY"));
    assertEquals(" Value of the third key ", processorInput.getValue("THIRD_KEY"));
    assertEquals("\\ Value of the fourth key \\", processorInput.getValue("FOURTH_KEY"));
    assertEquals("\\\\Value of the fifth key\\\\", processorInput.getValue("5TH_KEY"));
    assertEquals("\\", processorInput.getValue("6TH_KEY"));
    assertEquals("\\\\", processorInput.getValue("7TH_KEY"));
  }

  @Test
  public void shouldThrowOnMalformedInput() {
    makeInputExceptionCase("lowercase-key test value", InputConflict.INVALID_KEY_CHARACTERS);
    makeInputExceptionCase("illègal_chärs test value", InputConflict.INVALID_KEY_CHARACTERS);
    makeInputExceptionCase("NO_VALUE", InputConflict.BLANK_VALUE);
    makeInputExceptionCase("EMPTY_VALUE ", InputConflict.BLANK_VALUE);
    makeInputExceptionCase("BLANK_VALUE  ", InputConflict.BLANK_VALUE);
  }

  @Test
  public void shouldSaveAsRead() throws Exception {
    var filePath = "/preprocessor/input.txt";
    var processor = parseInputFile(filePath);
    assertLinesEqual(
      getFileContents(filePath),
      captureOutput(processor::save)
    );
  }

  @Test
  public void shouldMigrate() throws Exception {
    var inputProcessor = parseInputFile("/preprocessor/migration_base.txt");
    var extensionProcessor = parseInputFile("/preprocessor/migration_updated.txt");

    assertEquals(6, inputProcessor.migrateTo(extensionProcessor));

    assertLinesEqual(
      getFileContents("/preprocessor/migration_result.txt"),
      captureOutput(inputProcessor::save)
    );
  }

  private void makeInputExceptionCase(String inputString, InputConflict expectedConflict) {
    assertEquals(
      expectedConflict,
      assertThrows(
        PreProcessorInputException.class,
        () -> new PreProcessorInput().load(new StringReader(inputString))
      ).conflict
    );
  }

  private PreProcessorInput parseInputFile(String filePath) throws IOException {
    try (
      var inputStream = getClass().getResourceAsStream(filePath);
    ) {
      assert inputStream != null;

      var processorInput = new PreProcessorInput();
      processorInput.load(new InputStreamReader(inputStream));
      return processorInput;
    }
  }
}
