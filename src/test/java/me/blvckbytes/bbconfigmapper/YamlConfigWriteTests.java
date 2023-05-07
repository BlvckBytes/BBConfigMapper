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

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class YamlConfigWriteTests {

  private final TestHelper helper = new TestHelper();

  @Test
  public void shouldAttachComments() throws Exception {
    YamlConfig config = helper.makeConfig("scalars.yml");
    helper.assertAttachCommentInMemory("a", Arrays.asList(" Comment above a", " Second line"), true, config);
    helper.assertAttachCommentInMemory("a.c", Arrays.asList(" Comment at the value of c", " Second line"), false, config);
    helper.assertAttachCommentInMemory("a.i", Arrays.asList(" Comment at the key i", " Second line"), true, config);
    helper.assertSave("scalars_commented.yml", config);
  }

  @Test
  public void shouldRefuseToAttachCommentsToNonExistingPath() throws Exception {
    YamlConfig config = helper.makeConfig("scalars.yml");
    helper.assertThrowsWithMsg(IllegalStateException.class, () -> helper.assertAttachCommentInMemory("invalid", Collections.singletonList(""), true, config), "Cannot attach a comment to a non-existing path");
    helper.assertThrowsWithMsg(IllegalStateException.class, () -> helper.assertAttachCommentInMemory("invalid.invalid", Collections.singletonList(""), false, config), "Cannot attach a comment to a non-existing path");
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
    helper.assertSetInMemory("g.i", helper.list("this", "has", "been", "overwritten"), config);
    helper.assertSetInMemory("a", helper.map("b", 21L, "x", "hello", "d", helper.map("y", false)), config);
    helper.assertSetInMemory("b.d", helper.map("e", helper.list(1.2D, 2L, null), "f", helper.map("hello", "world")), config);
    helper.assertSetInMemory("x.y.z.last", helper.map("a", 5L), config);
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
    helper.assertThrowsWithMsg(IllegalArgumentException.class, () -> helper.assertSetInMemory(null, helper.list(1, 2, 3), config), "Cannot exchange the root-node for a non-map node");
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
    assertEquals(Collections.emptyMap(), config.get(null));
    helper.assertSave("empty_line.yml", config);
  }

  @Test
  public void shouldWriteEmptyFileIfUnloaded() throws Exception {
    YamlConfig config = helper.makeConfig(null);
    helper.assertSave("empty_line.yml", config);
  }
}
