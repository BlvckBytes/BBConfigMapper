/*
 * MIT License
 *
 * Copyright (c) 2023 BlvckBytes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.gpeee.GPEEE;
import me.blvckbytes.gpeee.IExpressionEvaluator;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.parser.expression.AExpression;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class TestHelper {

  private final IExpressionEvaluator evaluator;
  private final String expressionMarkerSuffix;
  private final Logger logger;

  public TestHelper() {
    this.expressionMarkerSuffix = "$";
    this.logger = Logger.getGlobal();
    this.evaluator = new GPEEE(logger);
  }

  /**
   * Get the standard environment to evaluate in
   */
  public IEvaluationEnvironment getEnv() {
    return GPEEE.EMPTY_ENVIRONMENT;
  }

  /**
   * Create a new config instance and load it's contents from a yaml file
   * @param fileName Input file within the resources folder, null to not load at all
   * @return Loaded yaml configuration instance
   */
  public YamlConfig makeConfig(@Nullable String fileName) throws FileNotFoundException {
    YamlConfig config = new YamlConfig(this.evaluator, this.logger, this.expressionMarkerSuffix);

    if (fileName != null)
      config.load(new FileReader("src/test/resources/" + fileName));

    return config;
  }

  /**
   * Create a new config instance on the provided path and then create a
   * new mapper instance on top of that configuration instance
   * @param fileName Input file within the resources folder
   * @return Mapper instance, operating on the configuration instance
   */
  public IConfigMapper makeMapper(String fileName) throws FileNotFoundException {
    return makeMapper(fileName, null);
  }

  /**
   * Create a new config instance on the provided path and then create a
   * new mapper instance on top of that configuration instance
   * @param fileName Input file within the resources folder
   * @param converterRegistry Optional registry of custom value converters
   * @return Mapper instance, operating on the configuration instance
   */
  public IConfigMapper makeMapper(String fileName, IValueConverterRegistry converterRegistry) throws FileNotFoundException {
    YamlConfig config = makeConfig(fileName);
    return new ConfigMapper(config, this.logger, this.evaluator, converterRegistry);
  }

  /**
   * Assert that a config value is an expression and that it evaluates to the expected value
   * @param expected Expected expression value
   * @param expression Expression to check
   */
  public void assertExpression(Object expected, Object expression) {
    assertTrue(expression instanceof AExpression);
    assertEquals(expected, this.evaluator.evaluateExpression((AExpression) expression, getEnv()));
  }

  /**
   * Assert that the provided yaml config saves without throwing and that the saved
   * lines equal to the line contents of the provided comparison file
   * @param fileName Comparison file name within the resources/save folder
   * @param config Configuration to save and compare
   */
  public void assertSave(String fileName, YamlConfig config) throws Exception {
    StringWriter writer = new StringWriter();
    assertDoesNotThrow(() -> config.save(writer));
    List<String> fileContents = Files.readAllLines(Paths.get("src/test/resources/save/" + fileName));
    List<String> writerContents = List.of(writer.toString().split("\n"));
    assertLinesMatch(fileContents, writerContents);
  }

  /**
   * Asserts that a given value was present before removal (if existing is true) and
   * that it's absent afterwards.
   * @param path Path to remove
   * @param existing Whether the key actually exists
   * @param config Configuration instance to remove on
   */
  public void assertRemovalInMemory(String path, boolean existing, YamlConfig config) {
    assertTrue(!existing || config.exists(path));
    config.remove(path);
    assertFalse(config.exists(path));
  }

  /**
   * Asserts that a given key's comment lines do not match the lines about to append before
   * calling attach as well as their presence afterwards.
   * @param path Path to attach at
   * @param lines Lines of comments to attach
   * @param self Whether to attach to the key itself or it's value
   * @param config Configuration instance to attach on
   */
  public void assertAttachCommentInMemory(String path, List<String> lines, boolean self, YamlConfig config) {
    assertNotEquals(lines, config.readComment(path, self));
    config.attachComment(path, lines, self);
    assertEquals(lines, config.readComment(path, self));
  }

  /**
   * Asserts that a given key's value does not match the value about to set before
   * calling set and assures the key's value equality with the set value afterwards.
   * @param path Path to set at
   * @param value Value to set
   * @param config Configuration instance to set on
   */
  public void assertSetInMemory(String path, Object value, YamlConfig config) {
    assertNotEquals(config.get(path), value);
    config.set(path, value);
    assertEquals(config.get(path), value);
  }

  /**
   * Creates an ordered map of values by joining every even indexed value as a
   * key with an odd indexed value as a corresponding value
   * @param values Key value pairs
   * @return Ordered map
   */
  public Map<Object, Object> map(Object... values) {
    if (values.length % 2 != 0)
      throw new IllegalStateException("Every key needs to be mapped to a value");

    Map<Object, Object> result = new LinkedHashMap<>();

    for (int i = 0; i < values.length; i += 2)
      result.put(values[i], values[i + 1]);

    return result;
  }

  /**
   * Creates an ordered list of values by adding all values to a list
   * @param values Values to add to the list
   * @return Ordered list
   */
  public List<Object> list(Object... values) {
    return new ArrayList<>(Arrays.asList(values));
  }

  /**
   * Asserts that an executable throws an exception of a certain type with a specific message
   * @param expectedType Expected exception type
   * @param executable Executable to run
   * @param expectedMessage Expected exception message
   */
  public <T extends Throwable> void assertThrowsWithMsg(Class<T> expectedType, Executable executable, String expectedMessage) {
    T exception = assertThrows(expectedType, executable);
    assertEquals(exception.getMessage(), expectedMessage);
  }
}
