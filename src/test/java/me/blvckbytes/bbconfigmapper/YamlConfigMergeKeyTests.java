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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class YamlConfigMergeKeyTests {

  /*
    [x] Merge shallow
    [x] Merge deep
    [x] Merge multiple shallow
    [x] Merge multiple deep
    [x] Merges in merged mappings
    [x] Scalar keys are overridden, mappings are extended
   */

  private final TestHelper helper = new TestHelper();

  @Test
  public void shouldMergeKeyShallow() throws FileNotFoundException {
    YamlConfig config = helper.makeConfig("merge_key_shallow.yml");

    // Section that's merged
    assertEquals(5L, config.get("a.b"));
    assertEquals(3L, config.get("a.c"));
    assertEquals("hello, world", config.get("a.toBeOverridden"));

    // Another section that's also merged
    assertEquals(21L, config.get("f.g"));

    // Section that has been merged into
    assertEquals(5L, config.get("d.b"));
    assertEquals(3L, config.get("d.c"));
    assertEquals(12L, config.get("d.e"));
    assertEquals(21L, config.get("d.g"));
    assertEquals("i am overridden", config.get("d.toBeOverridden"));

    assertNull(config.get("d.<<"));
  }

  @Test
  public void shouldMergeMergeKeyShallow() throws FileNotFoundException {
    YamlConfig config = helper.makeConfig("merge_merge_key_shallow.yml");

    // Section that's merged into merged section
    assertEquals(9L, config.get("innerMerge.f"));
    assertEquals(10L, config.get("innerMerge.g"));

    // Section that's merged and contains inner merge
    assertEquals(5L, config.get("a.b"));
    assertEquals(3L, config.get("a.c"));
    assertEquals(9L, config.get("a.f"));
    assertEquals(10L, config.get("a.g"));

    // Section that has been merged into, both sections (effectively)
    assertEquals(5L, config.get("d.b"));
    assertEquals(3L, config.get("d.c"));
    assertEquals(9L, config.get("d.f"));
    assertEquals(10L, config.get("d.g"));
    assertEquals(12L, config.get("d.e"));

    assertNull(config.get("a.<<"));
    assertNull(config.get("d.<<"));
  }

  @Test
  public void shouldMergeKeyDeep() throws FileNotFoundException {
    YamlConfig config = helper.makeConfig("merge_key_deep.yml");

    // Section that's merged
    assertEquals(5L, config.get("a.b"));
    assertEquals(3L, config.get("a.c"));
    assertEquals(100L, config.get("a.anotherMap.k1"));
    assertEquals(200L, config.get("a.anotherMap.k2"));
    assertEquals("hello, world", config.get("a.anotherMap.toBeOverridden"));

    // Section that'll override k2
    assertEquals(300L, config.get("secondOverride.anotherMap.k2"));

    // Section that has been merged into
    assertEquals(5L, config.get("d.b"));
    assertEquals(3L, config.get("d.c"));
    assertEquals(12L, config.get("d.e"));
    assertEquals(100L, config.get("d.anotherMap.k1"));
    assertEquals(300L, config.get("d.anotherMap.k2"));
    assertEquals("i am overridden", config.get("d.anotherMap.toBeOverridden"));

    assertNull(config.get("d.<<"));
  }

  @Test
  public void shouldMergeMergeKeyDeep() throws FileNotFoundException {
    YamlConfig config = helper.makeConfig("merge_merge_key_deep.yml");

    // Section that's merged into merged section
    assertEquals(9L, config.get("innerMerge.f"));
    assertEquals(10L, config.get("innerMerge.g"));
    assertEquals("hello, world", config.get("innerMerge.anotherMap.toBeOverridden"));

    // Section that's merged and contains inner merge
    assertEquals(5L, config.get("a.b"));
    assertEquals(3L, config.get("a.c"));
    assertEquals(9L, config.get("a.f"));
    assertEquals(10L, config.get("a.g"));
    assertEquals("hello, world", config.get("a.anotherMap.toBeOverridden"));

    // Section that has been merged into, both sections (effectively)
    assertEquals(5L, config.get("d.b"));
    assertEquals(3L, config.get("d.c"));
    assertEquals(9L, config.get("d.f"));
    assertEquals(10L, config.get("d.g"));
    assertEquals(12L, config.get("d.e"));
    assertEquals("i am overridden", config.get("d.anotherMap.toBeOverridden"));

    assertNull(config.get("a.<<"));
    assertNull(config.get("d.<<"));
  }
}
