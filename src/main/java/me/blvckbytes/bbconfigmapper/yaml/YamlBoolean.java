package me.blvckbytes.bbconfigmapper.yaml;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class YamlBoolean extends AYamlValue {

  private Boolean value;

  public YamlBoolean(Boolean value) {
    super(null);

    this.value = value;
  }
}
