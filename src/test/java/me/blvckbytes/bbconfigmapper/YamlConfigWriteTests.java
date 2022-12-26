package me.blvckbytes.bbconfigmapper;

import org.junit.jupiter.api.Test;

import java.util.List;

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
  public void shouldRemoveKeys() throws Exception {
    YamlConfig config = helper.makeConfig("mappings.yml");
    helper.assertRemovalInMemory("b.d", true, config);
    helper.assertRemovalInMemory("g.i", true, config);
    helper.assertRemovalInMemory("a.d", true, config);
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
}
