package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IEvaluable {

  <T> T asScalar(ScalarType type, IEvaluationEnvironment env);

  <T, U> Map<T, U> asMap(ScalarType key, ScalarType value, IEvaluationEnvironment env);

  <T> List<T> asList(ScalarType type, IEvaluationEnvironment env);

  <T> Set<T> asSet(ScalarType type, IEvaluationEnvironment env);

}
