package me.blvckbytes.bbconfigmapper.preprocessor;

import me.blvckbytes.bbconfigmapper.YamlConfig;
import me.blvckbytes.gpeee.Tuple;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.nodes.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class PreProcessor {

  @FunctionalInterface
  public interface ScalarNodeHandler {
    /**
     * @return Whether to enable expression-mode on the parent key, if not enabled already
     */
    boolean handle(ScalarNode node) throws Exception;
  }

  private final Field scalarNodeValueField;

  public PreProcessor() throws Exception {
    this.scalarNodeValueField = ScalarNode.class.getDeclaredField("value");
    this.scalarNodeValueField.setAccessible(true);
  }

  /**
   * @return Result, and whether any substitutions took place
   */
  public Tuple<String, Boolean> preProcess(String input, PreProcessorInput substitutions) {
    var result = new StringBuilder();

    var contentBegin = -1;
    var substitutionBegin = -1;
    var didSubstitute = false;

    for (var i = 0; i < input.length(); ++i) {
      var currentChar = input.charAt(i);

      if (currentChar == '@' && i != input.length() - 1 && input.charAt(i + 1) == '{') {
        if (contentBegin >= 0) {
          result.append(input, contentBegin, i);
          contentBegin = -1;
        }

        substitutionBegin = i;
        continue;
      }

      if (substitutionBegin >= 0 && currentChar == '}') {
        var substitutionContent = input.substring(substitutionBegin + 2, i);
        var openingParenthesisIndex = substitutionContent.indexOf('(');
        var temporaryVariables = Map.<String, String>of();

        if (openingParenthesisIndex >= 0) {
          var closingParenthesisIndex = substitutionContent.lastIndexOf(')');

          if (closingParenthesisIndex > openingParenthesisIndex) {
            var temporaryVariablesContent = substitutionContent.substring(openingParenthesisIndex + 1, closingParenthesisIndex);
            temporaryVariables = parseTemporaryVariables(temporaryVariablesContent, openingParenthesisIndex + 1);
            substitutionContent = substitutionContent.substring(0, openingParenthesisIndex).trim();
          }
        }

        var substitutionKey = substitutionContent;
        String substitution = substitutions.getValue(substitutionKey);

        if (substitution == null)
          throw new PreProcessorException(substitutionBegin, PreProcessConflict.VARIABLE_NOT_FOUND);

        result.append(renderInterpolations(substitution, temporaryVariables));
        substitutionBegin = -1;
        didSubstitute = true;
        continue;
      }

      if (substitutionBegin >= 0)
        continue;

      if (contentBegin == -1)
        contentBegin = i;
    }

    if (contentBegin >= 0)
      result.append(input.substring(contentBegin));

    return new Tuple<>(result.toString(), didSubstitute);
  }

  public Map<String, String> parseTemporaryVariables(String input, int beginIndex) {
    var result = new HashMap<String, String>();

    var assignments = input.split(";");

    for (var assignment : assignments) {
      var assignmentParts = assignment.split("=", 2);

      if (assignmentParts.length != 2)
        throw new PreProcessorException(beginIndex, PreProcessConflict.MALFORMED_TEMPORARY_VARIABLE);

      var key = assignmentParts[0].strip().toLowerCase();
      var value = assignmentParts[1].strip();

      if (key.isBlank() || value.isBlank())
        throw new PreProcessorException(beginIndex, PreProcessConflict.MALFORMED_TEMPORARY_VARIABLE);

      result.put(key, value);
    }

    return result;
  }

  public void forEachScalarValue(YamlConfig config, ScalarNodeHandler handler) throws Exception {
    forEachScalarValue(config.getRootNode(), null, config.getExpressionMarkerSuffix(), handler);
  }

  public void setScalarValue(ScalarNode node, String value) throws Exception {
    this.scalarNodeValueField.set(node, value);
  }

  public void forEachScalarValue(
    Node valueNode, @Nullable ScalarNode keyNode,
    String expressionMarkerSuffix,
    ScalarNodeHandler handler
  ) throws Exception {
    if (valueNode instanceof ScalarNode scalarNode) {
      if (handler.handle(scalarNode)) {
        if (keyNode != null && !keyNode.getValue().endsWith(expressionMarkerSuffix))
          setScalarValue(keyNode, keyNode.getValue() + expressionMarkerSuffix);
      }

      return;
    }

    if (valueNode instanceof MappingNode mappingNode) {
      for (var entry : mappingNode.getValue()) {
        if (!(entry.getKeyNode() instanceof ScalarNode currentKeyNode))
          continue;

        forEachScalarValue(
          entry.getValueNode(), currentKeyNode,
          expressionMarkerSuffix,
          handler
        );
      }

      return;
    }

    if (valueNode instanceof SequenceNode sequenceNode) {
      for (var entry : sequenceNode.getValue()) {
        forEachScalarValue(
          entry, keyNode,
          expressionMarkerSuffix,
          handler
        );
      }
    }
  }

  private boolean isValidSubstitutionChar(char c) {
    return (
      (c >= 'a' && c <= 'z') ||
      (c >= '0' && c <= '9') ||
      c == '_'
    );
  }

  public String renderInterpolations(String input, Map<String, String> temporaryVariables) {
    var result = new StringBuilder();

    var stringBuffer = new StringBuilder();
    var interpolationBuffer = new StringBuilder();

    var wasPreviousCharBackslash = false;

    for (var i = 0; i < input.length(); ++i) {
      var currentChar = input.charAt(i);

      var isEscaped = wasPreviousCharBackslash;
      wasPreviousCharBackslash = currentChar == '\\';

      if (currentChar == '{') {
        if (isEscaped)
          stringBuffer.setLength(stringBuffer.length() - 1);
        else {
          interpolationBuffer.append(currentChar);
          continue;
        }
      }

      if (!interpolationBuffer.isEmpty()) {
        if (currentChar != '}') {
          if (!isValidSubstitutionChar(currentChar)) {
            stringBuffer.append(interpolationBuffer);
            stringBuffer.append(currentChar);
            interpolationBuffer.setLength(0);
            continue;
          }

          interpolationBuffer.append(currentChar);
          continue;
        }

        if (!stringBuffer.isEmpty()) {
          if (!result.isEmpty())
            result.append(" & ");

          result.append('"').append(stringBuffer).append('"').append(" & ");
          stringBuffer.setLength(0);
        }

        var substitution = interpolationBuffer.substring(1); // Leading '{' of begin-marker
        var temporaryValue = temporaryVariables.get(substitution.toLowerCase());

        // These can be arbitrarily complex expressions, which are substituted into yet another expression
        // Add parentheses just to ensure desired order of evaluation
        if (temporaryValue != null)
          substitution = "(" + temporaryValue + ")";

        result.append(substitution);

        interpolationBuffer.setLength(0);
        continue;
      }

      if (currentChar == '"')
        stringBuffer.append('\\');

      stringBuffer.append(currentChar);
    }

    // Unterminated interpolation - treat as string
    if (!interpolationBuffer.isEmpty())
      stringBuffer.append(interpolationBuffer);

    if (!stringBuffer.isEmpty()) {
      if (!result.isEmpty())
        result.append(" & ");

      result.append('"').append(stringBuffer).append('"');
    }

    return result.toString();
  }
}
