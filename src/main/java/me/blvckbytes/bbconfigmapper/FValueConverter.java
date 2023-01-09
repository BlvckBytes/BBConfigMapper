package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.gpeee.IExpressionEvaluator;

@FunctionalInterface
public interface FValueConverter {

  Object apply(Object value, IExpressionEvaluator evaluator);

}
