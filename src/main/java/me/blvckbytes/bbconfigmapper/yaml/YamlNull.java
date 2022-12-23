package me.blvckbytes.bbconfigmapper.yaml;

import lombok.Getter;

@Getter
public class YamlNull extends AYamlValue {

  public YamlNull() {
    super(null);
  }

  @Override
  public Object getValue() {
    return null;
  }
}
