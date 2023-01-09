package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.gpeee.IExpressionEvaluator;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.gpeee.parser.expression.AExpression;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ConfigValue implements IEvaluable {

  protected final @Nullable Object value;
  private final IExpressionEvaluator evaluator;

  public ConfigValue(@Nullable Object value, IExpressionEvaluator evaluator) {
    this.value = value;
    this.evaluator = evaluator;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T asScalar(ScalarType type, IEvaluationEnvironment env) {
    return (T) interpret(value, type.getType(), null, env);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> List<T> asList(ScalarType type, IEvaluationEnvironment env) {
    return (List<T>) interpret(value, List.class, new ScalarType[] { type }, env);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Set<T> asSet(ScalarType type, IEvaluationEnvironment env) {
    return (Set<T>) interpret(value, Set.class, new ScalarType[] { type }, env);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T, U> Map<T, U> asMap(ScalarType key, ScalarType value, IEvaluationEnvironment env) {
    return (Map<T, U>) interpret(value, Map.class, new ScalarType[] { key, value }, env);
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
  protected <T> T interpretScalar(@Nullable Object input, ScalarType type, IEvaluationEnvironment env) {
    Class<?> typeClass = type.getType();

    if (typeClass.isInstance(input))
      return (T) input;

    // The input is an expression which needs to be evaluated first
    if (input instanceof AExpression)
      input = this.evaluator.evaluateExpression((AExpression) input, env);

    return (T) type.getInterpreter().apply(input, env);
  }

  /**
   * Interpret a nullable input value as a scalar, list or map configuration
   * value by making use of {@link #interpretScalar} to either convert to a scalar
   * value or to convert list items / map values to the requested scalar type before responding.
   * @param input Nullable value input
   * @param type Required result type
   * @param genericTypes Scalar types of a list/map (null for scalar requests)
   * @param env Environment used to interpret and evaluate with
   * @return Guaranteed non-null value of the requested type
   */
  @SuppressWarnings("unchecked")
  private<T> T interpret(@Nullable Object input, Class<T> type, @Nullable ScalarType[] genericTypes, IEvaluationEnvironment env) {

    if (type == List.class || type == Set.class) {

      if (genericTypes == null || genericTypes.length < 1 || genericTypes[0] == null)
        throw new IllegalStateException("Cannot require a List without specifying a generic type");

      // Is not a list, interpret the value as the requested generic type
      // and return a list with that one entry only
      if (!(input instanceof List))
        return (T) List.of((Object) interpretScalar(input, genericTypes[0], env));

      List<?> items = (List<?>) input;
      Collection<Object> results;

      if (type == List.class)
        results = new ArrayList<>();
      else
        results = new HashSet<>();

      // Interpret each item as the requested generic type
      for (Object item : items) {

        // FIXME: This seems hella repetitive to #interpretScalar

        // Expression result collections are flattened into the return result collection, if applicable
        if (item instanceof AExpression) {
          Object result = this.evaluator.evaluateExpression((AExpression) item, env);

          if (result instanceof Collection) {
            for (Object subItem : (Collection<?>) result)
              results.add(genericTypes[0].getInterpreter().apply(subItem, env));

            continue;
          }

          results.add(genericTypes[0].getInterpreter().apply(result, env));
          continue;
        }

        results.add(interpretScalar(item, genericTypes[0], env));
      }

      return (T) results;
    }

    if (type == Map.class) {

      if (genericTypes == null || genericTypes.length < 2 || genericTypes[0] == null || genericTypes[1] == null) {
        throw new IllegalStateException("Cannot require a Map without specifying generic types");
      }

      // Null will just be the empty map
      if (input == null)
        return (T) Map.of();

      if (!(input instanceof Map))
        throw new IllegalStateException("Cannot transform type " + input.getClass().getName() + " into a map");

      Map<?, ?> items = (Map<?, ?>) input;
      Map<Object, Object> results = new HashMap<>();

      // Interpret each value as the requested generic type
      for (Map.Entry<?, ?> entry : items.entrySet())
        results.put(interpretScalar(entry.getKey(), genericTypes[0], env), interpretScalar(entry.getValue(), genericTypes[1], env));

      return (T) results;
    }

    ScalarType scalarType = ScalarType.fromClass(type);

    if (scalarType == null)
      throw new IllegalStateException("Unknown scalar type provided: " + type);

    return interpretScalar(input, scalarType, env);
  }

  @Override
  public String toString() {
    return "ConfigValue{" +
      "value=" + value +
      '}';
  }
}
