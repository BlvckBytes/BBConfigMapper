package me.blvckbytes.bbconfigmapper;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class YamlConfigTests {

  private final TestHelper helper = new TestHelper();

  @Test
  public void testTest() throws FileNotFoundException {
    YamlConfig config = helper.makeConfig("test.yml");
    assertEquals(config.get("hello"), "world");
  }
}
