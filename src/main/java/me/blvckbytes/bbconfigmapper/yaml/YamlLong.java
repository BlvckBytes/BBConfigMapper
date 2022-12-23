package me.blvckbytes.bbconfigmapper.yaml;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class YamlLong extends AYamlValue {

  private Long value;

  public YamlLong(Long value) {
    super(null);

    this.value = value;
  }
}
