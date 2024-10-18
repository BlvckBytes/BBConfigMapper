package me.blvckbytes.bbconfigmapper.preprocessor;

import java.util.Objects;

public class EnvironmentComment {

  public final String variableName;
  private final String variableNameLower;

  public final String variableType;
  private final String variableTypeLower;

  public final CommentLine commentLine;

  public EnvironmentComment(String variableName, String variableType, CommentLine commentLine) {
    this.variableName = variableName;
    this.variableNameLower = variableName.toLowerCase();
    this.variableType = variableType;
    this.variableTypeLower = variableType.toLowerCase();
    this.commentLine = commentLine;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EnvironmentComment that)) return false;
    return Objects.equals(variableNameLower, that.variableNameLower) && Objects.equals(variableTypeLower, that.variableTypeLower);
  }

  @Override
  public int hashCode() {
    return Objects.hash(variableNameLower, variableTypeLower);
  }
}