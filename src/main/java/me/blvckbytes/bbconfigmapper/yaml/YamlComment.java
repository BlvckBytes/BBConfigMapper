package me.blvckbytes.bbconfigmapper.yaml;

import lombok.AllArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Setter
@AllArgsConstructor
public class YamlComment {

  private List<String> lines;

  public List<String> getLines() {
    return Collections.unmodifiableList(lines);
  }
}
