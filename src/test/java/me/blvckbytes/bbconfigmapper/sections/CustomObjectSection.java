package me.blvckbytes.bbconfigmapper.sections;

import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class CustomObjectSection extends AConfigSection {

  public CustomObject customObject;

  public CustomObjectSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }

  public CustomObject getCustomObject() {
    return customObject;
  }
}
