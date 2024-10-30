package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.bbconfigmapper.preprocessor.PreProcessConflict;
import me.blvckbytes.bbconfigmapper.preprocessor.PreProcessor;
import me.blvckbytes.bbconfigmapper.preprocessor.PreProcessorException;
import me.blvckbytes.bbconfigmapper.preprocessor.PreProcessorInput;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PreProcessorTests extends PreProcessorTestBase {

  @Test
  public void shouldRenderInterpolations() throws Exception {
    interpolationCase(
      "\"Simple string without variables\"",
      "Simple string without variables"
    );

    interpolationCase(
      "\"Do not interpolate { spaced } brackets\"",
      "Do not interpolate { spaced } brackets"
    );

    interpolationCase(
      "\"One substituted \" & variable & \" :)\"",
      "One substituted {variable} :)"
    );

    interpolationCase(
      "\"One substituted\" & variable & \" :)\"",
      "One substituted{variable} :)"
    );

    interpolationCase(
      "\"One substituted \" & variable & \":)\"",
      "One substituted {variable}:)"
    );

    interpolationCase(
      "\"One substituted\" & variable & \":)\"",
      "One substituted{variable}:)"
    );

    interpolationCase(
      "variable & \" leading variable\"",
      "{variable} leading variable"
    );

    interpolationCase(
      "\"trailing variable \" & variable",
      "trailing variable {variable}"
    );

    interpolationCase(
      "\"unmatched {brackets stay {untouched \" & test",
      "unmatched {brackets stay {untouched {test}"
    );

    interpolationCase(
      "a & \"Multiple\" & b & \"Variables\" & c & \":)\"",
      "{a}Multiple{b}Variables{c}:)"
    );

    interpolationCase(
      "a & \"Multiple\" & (lut[\"PREFIX\"]) & \"Variables\" & c & \":)\"",
      "{a}Multiple{b}Variables{c}:)",
      Map.of(
        "b", "lut[\"PREFIX\"]"
      )
    );

    interpolationCase(
      "\"Escaped {variable} substitution\"",
      "Escaped \\{variable} substitution"
    );

    interpolationCase(
      "\"String containing \\\" double-quotes \\\"\"",
      "String containing \" double-quotes \""
    );

    interpolationCase(
      "\"Unterminated {interpolation\"",
      "Unterminated {interpolation"
    );

    interpolationCase(
      "\"Ending with \" & variable",
      "Ending with {variable}"
    );
  }

  @Test
  public void shouldParseTemporaryVariables() throws Exception {
    var preProcessor = new PreProcessor();
    var temporaryVariables = preProcessor.parseTemporaryVariables(
      "a=lut[\"PREFIX\"];B = test ; c  =if x then y else z; d=key(y); e=value(y)",
      0
    );

    assertEquals(5, temporaryVariables.size());
    assertEquals("lut[\"PREFIX\"]", temporaryVariables.get("a"));
    assertEquals("test", temporaryVariables.get("b"));
    assertEquals("if x then y else z", temporaryVariables.get("c"));
    assertEquals("key(y)", temporaryVariables.get("d"));
    assertEquals("value(y)", temporaryVariables.get("e"));
  }

  @Test
  public void shouldThrowOnMalformedTemporaryVariables() {
    assertEquals(
      PreProcessConflict.MALFORMED_TEMPORARY_VARIABLE,
      assertThrows(
        PreProcessorException.class,
        () -> new PreProcessor().parseTemporaryVariables("hello", 0)
      ).conflict
    );

    assertEquals(
      PreProcessConflict.MALFORMED_TEMPORARY_VARIABLE,
      assertThrows(
        PreProcessorException.class,
        () -> new PreProcessor().parseTemporaryVariables("test=", 0)
      ).conflict
    );

    assertEquals(
      PreProcessConflict.MALFORMED_TEMPORARY_VARIABLE,
      assertThrows(
        PreProcessorException.class,
        () -> new PreProcessor().parseTemporaryVariables("=test", 0)
      ).conflict
    );
  }

  @Test
  public void shouldIterateScalarValues() throws Exception {
    var config = new YamlConfig(null, logger, "$");

    try (
      var inputStream = getClass().getResourceAsStream("/preprocessor/scalar_values.yml")
    ) {
      assert inputStream != null;
      config.load(new InputStreamReader(inputStream));

      var expectedKeys = new ArrayList<>(List.of(
        "value 1", "value 2", "value 3", "value 4", "value 5",
        "value 6", "value 7", "value 8", "value 9", "value 10"
      ));

      var preProcessor = new PreProcessor();

      preProcessor.forEachScalarValue(config, node -> {
        assertTrue(expectedKeys.remove(node.getValue()), "Value " + node.getValue() + " was unexpected");
        return false;
      });

      assertEquals(
        "",
        String.join("; ", expectedKeys),
        "There are remaining expected items which have not occurred"
      );
    }
  }

  @Test
  public void shouldPreProcessComplexFile() throws Exception {
    var config = new YamlConfig(null, logger, "$");

    try (
      var inputStream = getClass().getResourceAsStream("/preprocessor/base.yml")
    ) {
      assert inputStream != null;
      config.load(new InputStreamReader(inputStream));

      var variablesPath = "/preprocessor/substitution_input.txt";
      var processorInput = new PreProcessorInput();
      var preProcessor = new PreProcessor();

      try (
        var processorInputStream = getClass().getResourceAsStream(variablesPath)
      ) {
        assert processorInputStream != null;
        processorInput.load(new InputStreamReader(processorInputStream));
      }

      preProcessor.forEachScalarValue(config, node -> {
        var result = preProcessor.preProcess(node.getValue(), processorInput);
        preProcessor.setScalarValue(node, result.a);
        return result.b;
      });

      assertLinesEqual(
        getFileContents("/preprocessor/result.yml"),
        captureOutput(config::save)
      );
    }
  }

  @Test
  public void shouldThrowOnUnknownVariables() throws Exception {
    assertEquals(
      PreProcessConflict.VARIABLE_NOT_FOUND,
      assertThrows(
        PreProcessorException.class,
        () -> new PreProcessor().preProcess("@{UNKNOWN_VARIABLE}", new PreProcessorInput())
      ).conflict
    );
  }

  private void interpolationCase(String expectedOutput, String input) throws Exception {
    interpolationCase(expectedOutput, input, Map.of());
  }

  private void interpolationCase(String expectedOutput, String input, Map<String, String> temporaryVariables) throws Exception {
    var preProcessor = new PreProcessor();
    assertEquals(expectedOutput, preProcessor.renderInterpolations(input, temporaryVariables));
  }
}
