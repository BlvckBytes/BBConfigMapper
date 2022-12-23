package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;

import java.util.List;
import java.util.Map;

public interface IEvaluable {

  long asLong(IEvaluationEnvironment env);
  double asDouble(IEvaluationEnvironment env);
  boolean asBoolean(IEvaluationEnvironment env);
  String asString(IEvaluationEnvironment env);

  List<Long> asLongList(IEvaluationEnvironment env);
  List<Double> asDoubleList(IEvaluationEnvironment env);
  List<Boolean> asBooleanList(IEvaluationEnvironment env);
  List<String> asStringList(IEvaluationEnvironment env);

  Map<String, Long> asLongMap(IEvaluationEnvironment env);
  Map<String, Double> asDoubleMap(IEvaluationEnvironment env);
  Map<String, Boolean> asBooleanMap(IEvaluationEnvironment env);
  Map<String, String> asStringMap(IEvaluationEnvironment env);

}