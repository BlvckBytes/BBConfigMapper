package me.blvckbytes.bbconfigmapper.yaml;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class YamlString extends AYamlValue {

  private String value;

  public YamlString(String value) {
    super(null);

    this.value = value;
  }
}
