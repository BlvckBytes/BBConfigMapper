package me.blvckbytes.bbconfigmapper.yaml;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class YamlSequence extends AYamlValue {

  private final List<AYamlValue> sequence;

  public YamlSequence(List<AYamlValue> sequence) {
    super(null);

    this.sequence = sequence;
  }

  @Override
  public Object getValue() {
    return Collections.unmodifiableList(sequence);
  }
}
