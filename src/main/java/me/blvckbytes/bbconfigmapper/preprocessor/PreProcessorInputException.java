package me.blvckbytes.bbconfigmapper.preprocessor;

public class PreProcessorInputException extends RuntimeException {

  public final int lineNumber;
  public final InputConflict conflict;

  public PreProcessorInputException(int lineNumber, InputConflict conflict) {
    this.lineNumber = lineNumber;
    this.conflict = conflict;
  }
}
