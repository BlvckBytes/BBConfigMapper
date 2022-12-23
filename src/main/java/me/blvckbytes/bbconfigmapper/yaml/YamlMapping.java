package me.blvckbytes.bbconfigmapper.yaml;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

@Getter
public class YamlMapping extends AYamlValue {

  private final Map<String, AYamlValue> mappings;

  public YamlMapping(Map<String, AYamlValue> mappings) {
    super(null);

    this.mappings = mappings;
  }

  public @Nullable AYamlValue lookupKey(String key) {
    return mappings.get(key);
  }

  public void storeKey(String key, AYamlValue value) {
    this.mappings.put(key, value);
  }

  @Override
  public Object getValue() {
    return Collections.unmodifiableMap(mappings);
  }
}
