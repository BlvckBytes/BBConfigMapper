package me.blvckbytes.bbconfigmapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

@Getter
@AllArgsConstructor
public enum ScalarType {
  LONG(long.class, (i, e) -> e.getValueInterpreter().asLong(i)),
  DOUBLE(double.class, (i, e) -> e.getValueInterpreter().asDouble(i)),
  BOOLEAN(boolean.class, (i, e) -> e.getValueInterpreter().asBoolean(i)),
  STRING(String.class, (i, e) -> e.getValueInterpreter().asString(i))
  ;

  private final Class<?> type;
  private final BiFunction<@Nullable Object, IEvaluationEnvironment, Object> interpreter;

  private static final Map<Class<?>, ScalarType> lookupTable;

  static {
    lookupTable = new HashMap<>();
    lookupTable.put(long.class, LONG);
    lookupTable.put(Long.class, LONG);
    lookupTable.put(double.class, DOUBLE);
    lookupTable.put(Double.class, DOUBLE);
    lookupTable.put(boolean.class, BOOLEAN);
    lookupTable.put(Boolean.class, BOOLEAN);
    lookupTable.put(String.class, STRING);
  }

  public static @Nullable ScalarType fromClass(Class<?> c) {
    return lookupTable.get(c);
  }
}
