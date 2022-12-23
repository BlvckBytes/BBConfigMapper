package me.blvckbytes.bbconfigmapper.yaml;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class YamlDouble extends AYamlValue {

  private Double value;

  public YamlDouble(Double value) {
    super(null);

    this.value = value;
  }
}
