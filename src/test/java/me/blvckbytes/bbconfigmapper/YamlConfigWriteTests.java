package me.blvckbytes.bbconfigmapper;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class YamlConfigWriteTests {

  private final TestHelper helper = new TestHelper();

  @Test
  public void shouldAttachComments() throws Exception {
    YamlConfig config = helper.makeConfig("scalars.yml");
    helper.assertAttachCommentInMemory("a", List.of(" Comment above a", " Second line"), true, config);
    helper.assertAttachCommentInMemory("a.c", List.of(" Comment at the value of c", " Second line"), false, config);
    helper.assertAttachCommentInMemory("a.i", List.of(" Comment at the key i", " Second line"), true, config);
    helper.assertSave("scalars_commented.yml", config);
  }

  @Test
  public void shouldRefuseToAttachCommentsToNonExistingPath() throws Exception {
    YamlConfig config = helper.makeConfig("scalars.yml");
    helper.assertThrowsWithMsg(IllegalStateException.class, () -> helper.assertAttachCommentInMemory("invalid", List.of(""), true, config), "Cannot attach a comment to a non-existing path");
    helper.assertThrowsWithMsg(IllegalStateException.class, () -> helper.assertAttachCommentInMemory("invalid.invalid", List.of(""), false, config), "Cannot attach a comment to a non-existing path");
  }

  @Test
  public void shouldRemoveKeys() throws Exception {
    YamlConfig config = helper.makeConfig("mappings.yml");
    helper.assertRemovalInMemory("b.d", true, config);
    helper.assertRemovalInMemory("g.i", true, config);
    helper.assertRemovalInMemory("a.d", true, config);
    helper.assertRemovalInMemory("x", true, config);
    helper.assertRemovalInMemory("invalid_key", false, config);
    helper.assertSave("mappings_removed.yml", config);
  }

  @Test
  public void shouldOverwriteKeys() throws Exception {
    YamlConfig config = helper.makeConfig("mappings.yml");
    helper.assertSetInMemory("g.i", List.of("this", "has", "been", "overwritten"), config);
    helper.assertSetInMemory("a", helper.map("b", 21L, "x", "hello", "d", helper.map("y", false)), config);
    helper.assertSetInMemory("b.d", helper.map("e", List.of(1L, 2L, 3L), "f", helper.map("hello", "world")), config);
    helper.assertSave("mappings_overwritten.yml", config);
  }

  @Test
  public void shouldExtendMappings() throws Exception {
    YamlConfig config = helper.makeConfig("mappings.yml");
    helper.assertSetInMemory("a.new1", helper.map("hello", "world"), config);
    helper.assertSetInMemory("b.d.new2", helper.map("hello", "world"), config);
    helper.assertSave("mappings_extended.yml", config);
  }

  @Test
  public void shouldExchangeRootNode() throws Exception {
    YamlConfig config = helper.makeConfig("mappings.yml");
    helper.assertSetInMemory(null, helper.map("a", helper.map("b", helper.map("c", 5L))), config);
    helper.assertSave("root_exchanged.yml", config);
  }

  @Test
  public void shouldRefuseToExchangeRootNodeForNonMapping() throws Exception {
    YamlConfig config = helper.makeConfig("mappings.yml");
    helper.assertThrowsWithMsg(IllegalArgumentException.class, () -> helper.assertSetInMemory(null, List.of(1, 2, 3), config), "Cannot exchange the root-node for a non-map node");
    helper.assertThrowsWithMsg(IllegalArgumentException.class, () -> helper.assertSetInMemory(null, 12, config), "Cannot exchange the root-node for a non-map node");
    helper.assertThrowsWithMsg(IllegalArgumentException.class, () -> helper.assertSetInMemory(null, "hello, world", config), "Cannot exchange the root-node for a non-map node");
  }

  @Test
  public void shouldRefuseToWriteUnknownType() throws Exception {
    YamlConfig config = helper.makeConfig("mappings.yml");
    helper.assertThrowsWithMsg(IllegalArgumentException.class, () -> config.set("test", config), "Cannot store a value of type class me.blvckbytes.bbconfigmapper.YamlConfig");
  }

  @Test
  public void shouldWriteEmptyFileIfRootReset() throws Exception {
    YamlConfig config = helper.makeConfig("mappings.yml");
    config.remove(null);
    assertEquals(Map.of(), config.get(null));
    helper.assertSave("empty_line.yml", config);
  }

  @Test
  public void shouldWriteEmptyFileIfUnloaded() throws Exception {
    YamlConfig config = helper.makeConfig(null);
    helper.assertSave("empty_line.yml", config);
  }
}
