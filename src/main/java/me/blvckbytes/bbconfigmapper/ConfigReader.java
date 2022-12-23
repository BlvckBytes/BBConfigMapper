package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.gpeee.IExpressionEvaluator;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ConfigReader {

  private final IConfig config;
  private final IExpressionEvaluator evaluator;
  private final Map<String, ConfigValue> cache;

  /**
   * Create a new config reader on a {@link IConfig}
   * @param config Configuration to read from
   * @param evaluator Expression evaluator instance to use when parsing expressions
   */
  public ConfigReader(IConfig config, IExpressionEvaluator evaluator) {
    this.config = config;
    this.evaluator = evaluator;
    this.cache = new HashMap<>();
  }

  /**
   * Get a wrapped value from the underlying {@link IConfig}
   * @param key Key to read from
   * @return Wrapped value, null if the key didn't exist
   */
  public @Nullable IEvaluable get(String key) {
    if (cache.containsKey(key))
      return cache.get(key);

    if (!config.exists(key)) {
      // Remember that this key didn't exist
      cache.put(key, null);
      return null;
    }

    Object value = config.get(key);

    // Marked as an expression capable key, evaluate
    if (key.endsWith("$"))
      value = parseExpressions(value);

    ConfigValue result = new ConfigValue(value, evaluator);
    cache.put(key, result);
    return result;
  }

  /**
   * Set a value within the config, identified by it's key
   * @param key Key to write to
   * @param value Value to write
   */
  public void set(String key, @Nullable Object value) {
    this.cache.remove(key);
    config.set(key, value);
  }

  /**
   * Parses all available expressions within the input value, if applicable. Scalar values will
   * be passed through without modification, while others (including strings) will be parsed as
   * an expression. If a collection is encountered, it's entries will be ran through this
   * routine internally before returning, the same concept applies to maps and their keys.
   * @param input Input value
   * @return Output value with parsed expressions, if applicable
   */
  private @Nullable Object parseExpressions(@Nullable Object input) {
    // There's no need to evaluate an expression just to receive back a scalar value again
    if (isScalarValue(input))
      return input;

    if (input instanceof Collection) {
      List<Object> result = new ArrayList<>();

      for (Object item : ((Collection<?>) input))
        result.add(parseExpressions(item));

      return result;
    }

    if (input instanceof Map) {
      Map<Object, Object> result = new HashMap<>();

      for (Map.Entry<?, ?> entry : ((Map<?, ?>) input).entrySet())
        result.put(entry.getKey(), parseExpressions(entry.getValue()));

      return result;
    }

    // Interpret as a scalar value
    return evaluator.optimizeExpression(evaluator.parseString(input.toString()));
  }

  /**
   * Checks whether a value is a scalar value, meaning that it cannot
   * hold any evaluable expressions and thus can be passed straight through
   * @param input Input value to check
   * @return True if scalar, false if evaluable
   */
  private boolean isScalarValue(@Nullable Object input) {
    return (
      input == null ||
        input instanceof Double ||
        input instanceof Float ||
        input instanceof Integer ||
        input instanceof Long ||
        input instanceof Byte
    );
  }
}
