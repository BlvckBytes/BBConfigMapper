package me.blvckbytes.bbconfigmapper;

@FunctionalInterface
public interface FValueConverter {

  Object apply(IEvaluable value);

}
