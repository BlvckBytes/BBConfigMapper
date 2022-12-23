package me.blvckbytes.bbconfigmapper.yaml;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Setter
@Getter
@AllArgsConstructor
public abstract class AYamlValue {

  private @Nullable YamlComment comment;

  public abstract Object getValue();

}
