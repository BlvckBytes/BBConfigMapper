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

import java.util.Arrays;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class YamlConfigExtensionTests {

  private final TestHelper helper = new TestHelper();

  @Test
  public void shouldThrowWhenTryingToExtendFromAnUnloadedConfig() throws Exception {
    YamlConfig baseConfig = helper.makeConfig("mappings_base.yml");
    YamlConfig unloadedConfig = new YamlConfig(null, Logger.getGlobal(), null);
    assertThrows(IllegalStateException.class, () -> baseConfig.extendMissingKeys(unloadedConfig), "Other config has not yet been loaded");
  }

  @Test
  public void shouldExtendKeysWithScalarValues() throws Exception {
    YamlConfig baseConfig = helper.makeConfig("mappings_base.yml");
    YamlConfig extensionConfig = helper.makeConfig("mappings_extended.yml");

    int numberOfExtendedKeys = baseConfig.extendMissingKeys(extensionConfig);
    assertEquals(5, numberOfExtendedKeys, "The base config didn't extend as many keys as expected");

    assertEquals(25L, baseConfig.get("a.e.f"));
    assertEquals("hello, world", baseConfig.get("a.e.h.j"));
    assertEquals(Arrays.asList(15L, 18L), baseConfig.get("g.j"));

    helper.assertSave("mappings_patched.yml", baseConfig);
  }

  @Test
  public void shouldNotExtendCommentedKeys() throws Exception {
    char[] caseSuffixes = {'a', 'b', 'c'};

    for (char caseSuffix : caseSuffixes) {
      YamlConfig baseConfig = helper.makeConfig("commented_keys_base_" + caseSuffix + ".yml");
      YamlConfig extensionConfig = helper.makeConfig("commented_keys_extension.yml");

      baseConfig.extendMissingKeys(extensionConfig);

      helper.assertSave("commented_keys_save_" + caseSuffix + ".yml", baseConfig);
    }
  }
}
