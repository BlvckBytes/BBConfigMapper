package me.blvckbytes.bbconfigmapper;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakeConfig implements IConfig {

  private final Map<String, Object> values;

  public FakeConfig() {
    this.values = new HashMap<>();

    this.values.put("hello", "5 + 5");
    this.values.put("hello$", "5 + 5");
    this.values.put("lore$", List.of(
      "3*2",
      "4 - 8",
      "2^4"
    ));
  }

  @Override
  public @Nullable Object get(String key) {
    return values.get(key);
  }

  @Override
  public void set(String key, @Nullable Object value) {
    values.put(key, value);
  }

  @Override
  public boolean exists(String key) {
    return values.containsKey(key);
  }

  @Override
  public boolean attachComment(String key, List<String> lines) {
    // noop
    return false;
  }

  @Override
  public @Nullable List<String> readComment(String key) {
    // noop
    return null;
  }
}
