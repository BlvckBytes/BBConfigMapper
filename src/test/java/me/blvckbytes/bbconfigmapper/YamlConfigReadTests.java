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

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class YamlConfigReadTests {

  private final TestHelper helper = new TestHelper();

  @Test
  public void shouldUnwrapScalars() throws FileNotFoundException {
    YamlConfig config = helper.makeConfig("scalars.yml");
    assertEquals("hello", config.get("a.b"));
    assertEquals("world", config.get("a.c"));
    assertEquals(12L, config.get("a.d"));
    assertEquals(14.3D, config.get("a.e"));
    assertEquals(true, config.get("a.f"));
    assertEquals(false, config.get("a.g"));
    assertEquals(34.12 * Math.pow(10, 3), config.get("a.h"));
    assertNull(config.get("a.i"));
    assertEquals("top level", config.get("h"));
  }

  @Test
  public void shouldUnwrapLists() throws FileNotFoundException {
    YamlConfig config = helper.makeConfig("sequences.yml");
    assertEquals(helper.list("first", "second", "third"), config.get("a"));
    assertEquals(helper.list(21L, false, .3D), config.get("b.c"));
    assertEquals(helper.list("nested", helper.list("list", "items")), config.get("b.d"));
    assertEquals(helper.list("nested", helper.map("a", "mapping", "b", 12L)), config.get("b.e"));
  }

  @Test
  public void shouldUnwrapMaps() throws FileNotFoundException {
    YamlConfig config = helper.makeConfig("mappings.yml");
    assertEquals(helper.map("b", "first", "c", "second", "d", 3L), config.get("a"));
    assertEquals(helper.map("c", "nested", "d", helper.map("e", "mapping", "f", 1.2D)), config.get("b"));
    assertEquals(helper.map("h", "nested", "i", helper.list("list", "items")), config.get("g"));
  }

  @Test
  public void shouldParseExpressions() throws FileNotFoundException {
    YamlConfig config = helper.makeConfig("expressions.yml");
    helper.assertExpression(10 + 20 * 2L, config.get("a.b"));
    assertEquals("10 + 20 * 2", config.get("a.c"));
    assertEquals("20 * 3", config.get("d.e.f"));
    assertEquals("\"hello \" & \"world\"", config.get("d.e.g"));
    helper.assertExpression(20 * 3L, config.get("d.h.i"));
    helper.assertExpression("hello world", config.get("d.h.j"));
  }

  @Test
  public void shouldCheckKeyExistence() throws FileNotFoundException {
    YamlConfig config = helper.makeConfig("key_existence.yml");
    assertTrue(config.exists("a.b"));
    assertTrue(config.exists("a.c"));
    assertTrue(config.exists("a.d"));
    assertFalse(config.exists("a.e"));
    assertFalse(config.exists("b"));
  }

  @Test
  public void shouldReadComments() throws FileNotFoundException {
    YamlConfig config = helper.makeConfig("comments.yml");
    assertLinesMatch(List.of(" Comment above a", " test 1"), config.readComment("a", true));
    assertLinesMatch(List.of(" Comment above b", " test 2", "\n"), config.readComment("a.b", true));
    assertLinesMatch(List.of(" Comment above value", " test 3"), config.readComment("a.b", false));
  }

  @Test
  public void shouldReadNullCommentListOnNonExistingPath() throws FileNotFoundException {
    YamlConfig config = helper.makeConfig("comments.yml");
    assertNull(config.readComment("invalid", true));
    assertNull(config.readComment("invalid.invalid", false));
  }

  @Test
  public void shouldReadEmptyCommentListOnNonCommentedKey() throws FileNotFoundException {
    YamlConfig config = helper.makeConfig("comments.yml");
    assertLinesMatch(List.of(), config.readComment("a.c", true));
    assertLinesMatch(List.of(), config.readComment("a.c", false));
  }

  @Test
  public void shouldThrowOnMultiDocumentInput() {
    helper.assertThrowsWithMsg(IllegalStateException.class, () -> helper.makeConfig("multi_node.yml"), "Encountered multiple nodes");
  }

  @Test
  public void shouldThrowOnNonMappingRoot() {
    helper.assertThrowsWithMsg(IllegalStateException.class, () -> helper.makeConfig("top_level_list.yml"), "The top level of a config has to be a map.");
    helper.assertThrowsWithMsg(IllegalStateException.class, () -> helper.makeConfig("top_level_scalar.yml"), "The top level of a config has to be a map.");
  }

  @Test
  public void shouldThrowOnMalformedPaths() throws FileNotFoundException {
    YamlConfig config = helper.makeConfig("comments.yml");
    helper.assertThrowsWithMsg(IllegalArgumentException.class, () -> config.set("a.", 2), "Invalid path specified: a.");
    helper.assertThrowsWithMsg(IllegalArgumentException.class, () -> config.set("a. ", 2), "Invalid path specified: a. ");
    helper.assertThrowsWithMsg(IllegalArgumentException.class, () -> config.set(" .b", 2), "Invalid path specified: ");
  }
}
