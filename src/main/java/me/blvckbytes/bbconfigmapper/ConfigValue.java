package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.gpeee.IExpressionEvaluator;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.parser.expression.AExpression;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigValue implements IEvaluable {

  private final @Nullable Object value;
  private final IExpressionEvaluator evaluator;

  // If the cached value is a list or map, this will be the value type which had been used
  private @Nullable Class<?> cachedGenericType;
  private @Nullable Object cachedValue;

  public ConfigValue(@Nullable Object value, IExpressionEvaluator evaluator) {
    this.value = value;
    this.evaluator = evaluator;
  }

  @Override
  public long asLong(IEvaluationEnvironment env) {
    return interpretOrReadCache(value, long.class, null, env);
  }

  @Override
  public double asDouble(IEvaluationEnvironment env) {
    return interpretOrReadCache(value, double.class, null, env);
  }

  @Override
  public boolean asBoolean(IEvaluationEnvironment env) {
    return interpretOrReadCache(value, boolean.class, null, env);
  }

  @Override
  public String asString(IEvaluationEnvironment env) {
    return interpretOrReadCache(value, String.class, null, env);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Long> asLongList(IEvaluationEnvironment env) {
    return interpretOrReadCache(value, List.class, Long.class, env);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Double> asDoubleList(IEvaluationEnvironment env) {
    return interpretOrReadCache(value, List.class, Double.class, env);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Boolean> asBooleanList(IEvaluationEnvironment env) {
    return interpretOrReadCache(value, List.class, Boolean.class, env);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> asStringList(IEvaluationEnvironment env) {
    return interpretOrReadCache(value, List.class, String.class, env);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<Object, Long> asLongMap(IEvaluationEnvironment env) {
    return interpretOrReadCache(value, Map.class, Long.class, env);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<Object, Double> asDoubleMap(IEvaluationEnvironment env) {
    return interpretOrReadCache(value, Map.class, Double.class, env);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<Object, Boolean> asBooleanMap(IEvaluationEnvironment env) {
    return interpretOrReadCache(value, Map.class, Boolean.class, env);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<Object, String> asStringMap(IEvaluationEnvironment env) {
    return interpretOrReadCache(value, Map.class, String.class, env);
  }

  /**
   * Interpret a nullable input value as a scalar configuration value by
   * evaluating applicable expressions first and then using the value
   * interpreter to interpret the result as the required data type
   * @param input Nullable value input
   * @param type Required scalar type
   * @param env Environment used to interpret and evaluate with
   * @return Guaranteed non-Null value of the requested type
   */
  @SuppressWarnings("unchecked")
  private<T> T interpretScalar(@Nullable Object input, Class<T> type, IEvaluationEnvironment env) {
    if (type.isInstance(input))
      return (T) input;

    // The input is an expression which needs to be evaluated first
    if (input instanceof AExpression)
      input = this.evaluator.evaluateExpression((AExpression) input, env);

    Object res;

    if (type == long.class || type == Long.class)
      res = env.getValueInterpreter().asLong(input);

    else if (type == double.class || type == Double.class)
      res = env.getValueInterpreter().asDouble(input);

    else if (type == String.class)
      res = env.getValueInterpreter().asString(input);

    else if (type == boolean.class || type == Boolean.class)
      res = env.getValueInterpreter().asBoolean(input);

    else
      throw new IllegalStateException("Unsupported value type requested");

    return (T) res;
  }

  /**
   * Interpret a nullable input value as a scalar, list or map configuration
   * value by making use of {@link #interpretScalar(Object, Class, IEvaluationEnvironment)}
   * to either convert to a scalar value or to convert list items / map values to the
   * requested scalar type before responding. Responses are cached in order to not compute
   * the same request twice in a row
   * @param input Nullable value input
   * @param type Required result type
   * @param genericType Scalar type of a list/map (null for scalar requests)
   * @param env Environment used to interpret and evaluate with
   * @return Guaranteed non-null value of the requested type
   */
  @SuppressWarnings("unchecked")
  private<T> T interpretOrReadCache(@Nullable Object input, Class<T> type, @Nullable Class<?> genericType, IEvaluationEnvironment env) {

    // The cached value is of the requested type, both as a host type as well as it's generic type (if applicable)
    if (type.isInstance(cachedValue) && (cachedGenericType == genericType))
      return (T) cachedValue;

    if (type == List.class) {

      if (genericType == null)
        throw new IllegalStateException("Cannot require a List without specifying a generic type");

      cachedGenericType = genericType;

      // Is not a list, interpret the value as the requested generic type
      // and return a list with that one entry only
      if (!(input instanceof List)) {
        cachedValue = List.of(interpretScalar(input, genericType, env));
        return (T) cachedValue;
      }

      List<?> items = (List<?>) input;
      List<Object> results = new ArrayList<>();

      // Interpret each item as the requested generic type
      for (Object item : items)
        results.add(interpretScalar(item, genericType, env));

      cachedValue = results;
      return (T) cachedValue;
    }

    if (type == Map.class) {

      if (genericType == null)
        throw new IllegalStateException("Cannot require a Map without specifying a generic type");

      // Null will just be the empty map
      if (input == null) {
        cachedValue = Map.of();
        cachedGenericType = genericType;
        return (T) cachedValue;
      }

      if (!(input instanceof Map))
        throw new IllegalStateException("Cannot transform type " + input.getClass().getName() + " into a map");

      Map<?, ?> items = (Map<?, ?>) input;
      Map<Object, Object> results = new HashMap<>();

      // Interpret each value as the requested generic type
      for (Map.Entry<?, ?> entry : items.entrySet())
        results.put(entry.getKey(), interpretScalar(entry.getValue(), genericType, env));

      cachedGenericType = genericType;
      cachedValue = results;
      return (T) cachedValue;
    }

    cachedValue = interpretScalar(input, type, env);
    return (T) cachedValue;
  }

  @Override
  public String toString() {
    return "ConfigValue{" +
      "value=" + value +
      '}';
  }
}
