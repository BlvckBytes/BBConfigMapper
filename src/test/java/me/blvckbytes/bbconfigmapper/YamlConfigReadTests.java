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
    assertEquals(List.of("first", "second", "third"), config.get("a"));
    assertEquals(List.of(21L, false, .3D), config.get("b.c"));
    assertEquals(List.of("nested", List.of("list", "items")), config.get("b.d"));
    assertEquals(List.of("nested", helper.map("a", "mapping", "b", 12L)), config.get("b.e"));
  }

  @Test
  public void shouldUnwrapMaps() throws FileNotFoundException {
    YamlConfig config = helper.makeConfig("mappings.yml");
    assertEquals(helper.map("b", "first", "c", "second", "d", 3L), config.get("a"));
    assertEquals(helper.map("c", "nested", "d", helper.map("e", "mapping", "f", 1.2D)), config.get("b"));
    assertEquals(helper.map("h", "nested", "i", List.of("list", "items")), config.get("g"));
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
    assertLinesMatch(List.of(" Comment above b", " test 2"), config.readComment("a.b", true));
    assertLinesMatch(List.of(" Comment above value", " test 3"), config.readComment("a.b", false));
  }
}
