package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.gpeee.IExpressionEvaluator;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.parser.expression.AExpression;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ConfigValue implements IEvaluable {

  private final @Nullable Object value;
  private final IExpressionEvaluator evaluator;

  // If the cached value is a list or map, this will be the value type which had been used
  private @Nullable ScalarType[] cachedGenericTypes;
  private @Nullable Object cachedValue;

  public ConfigValue(@Nullable Object value, IExpressionEvaluator evaluator) {
    this.value = value;
    this.evaluator = evaluator;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T asScalar(ScalarType type, IEvaluationEnvironment env) {
    return (T) interpretOrReadCache(value, type.getType(), null, env);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> List<T> asList(ScalarType type, IEvaluationEnvironment env) {
    return (List<T>) interpretOrReadCache(value, List.class, new ScalarType[] { type }, env);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Set<T> asSet(ScalarType type, IEvaluationEnvironment env) {
    return (Set<T>) interpretOrReadCache(value, Set.class, new ScalarType[] { type }, env);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T, U> Map<T, U> asMap(ScalarType key, ScalarType value, IEvaluationEnvironment env) {
    return (Map<T, U>) interpretOrReadCache(value, Map.class, new ScalarType[] { key, value }, env);
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
  private<T> T interpretScalar(@Nullable Object input, ScalarType type, IEvaluationEnvironment env) {
    Class<?> typeClass = type.getType();

    if (typeClass.isInstance(input))
      return (T) input;

    // The input is an expression which needs to be evaluated first
    if (input instanceof AExpression)
      input = this.evaluator.evaluateExpression((AExpression) input, env);

    return (T) type.getInterpreter().apply(input, env);
  }

  /**
   * Compares two generic type arrays and only returns true if both depict the same types
   * @param genericTypesA Array A
   * @param genericTypesB Array B
   * @return True on match, false otherwise
   */
  private boolean doGenericTypesEqual(@Nullable ScalarType[] genericTypesA, @Nullable ScalarType[] genericTypesB) {
    // Both are null - was a scalar
    if (genericTypesA == null && genericTypesB == null)
      return true;

    // Only one is null, does not match
    if (genericTypesA == null || genericTypesB == null)
      return false;

    // Amount of generics mismatch
    if (genericTypesA.length != genericTypesB.length)
      return false;

    // Compare array entries
    for (int i = 0; i < genericTypesA.length; i++) {
      if (genericTypesA[i] != genericTypesB[i])
        return false;
    }

    // All entries matched
    return true;
  }

  /**
   * Interpret a nullable input value as a scalar, list or map configuration
   * value by making use of {@link #interpretScalar}
   * to either convert to a scalar value or to convert list items / map values to the
   * requested scalar type before responding. Responses are cached in order to not compute
   * the same request twice in a row
   * @param input Nullable value input
   * @param type Required result type
   * @param genericTypes Scalar types of a list/map (null for scalar requests)
   * @param env Environment used to interpret and evaluate with
   * @return Guaranteed non-null value of the requested type
   */
  @SuppressWarnings("unchecked")
  private<T> T interpretOrReadCache(@Nullable Object input, Class<T> type, @Nullable ScalarType[] genericTypes, IEvaluationEnvironment env) {

    // The cached value is of the requested type, both as a host type as well as it's generic type (if applicable)
    if (type.isInstance(cachedValue) && doGenericTypesEqual(cachedGenericTypes, genericTypes))
      return (T) cachedValue;

    if (type == List.class || type == Set.class) {

      if (genericTypes == null || genericTypes.length < 1 || genericTypes[0] == null)
        throw new IllegalStateException("Cannot require a List without specifying a generic type");

      cachedGenericTypes = genericTypes;

      // Is not a list, interpret the value as the requested generic type
      // and return a list with that one entry only
      if (!(input instanceof List)) {
        cachedValue = List.of(interpretScalar(input, genericTypes[0], env));
        return (T) cachedValue;
      }

      List<?> items = (List<?>) input;
      Collection<Object> results;

      if (type == List.class)
        results = new ArrayList<>();
      else
        results = new HashSet<>();

      // Interpret each item as the requested generic type
      for (Object item : items)
        results.add(interpretScalar(item, genericTypes[0], env));

      cachedValue = results;
      return (T) cachedValue;
    }

    if (type == Map.class) {

      if (genericTypes == null || genericTypes.length < 2 || genericTypes[0] == null || genericTypes[1] == null) {
        throw new IllegalStateException("Cannot require a Map without specifying generic types");
      }

      // Null will just be the empty map
      if (input == null) {
        cachedValue = Map.of();
        cachedGenericTypes = genericTypes;
        return (T) cachedValue;
      }

      if (!(input instanceof Map))
        throw new IllegalStateException("Cannot transform type " + input.getClass().getName() + " into a map");

      Map<?, ?> items = (Map<?, ?>) input;
      Map<Object, Object> results = new HashMap<>();

      // Interpret each value as the requested generic type
      for (Map.Entry<?, ?> entry : items.entrySet())
        results.put(interpretScalar(entry.getKey(), genericTypes[0], env), interpretScalar(entry.getValue(), genericTypes[1], env));

      cachedGenericTypes = genericTypes;
      cachedValue = results;
      return (T) cachedValue;
    }

    ScalarType scalarType = ScalarType.fromClass(type);

    if (scalarType == null)
      throw new IllegalStateException("Unknown scalar type provided: " + type);

    cachedValue = interpretScalar(input, scalarType, env);
    return (T) cachedValue;
  }

  @Override
  public String toString() {
    return "ConfigValue{" +
      "value=" + value +
      '}';
  }
}
