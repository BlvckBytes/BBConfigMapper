package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.bbconfigmapper.sections.CSIgnore;
import me.blvckbytes.bbconfigmapper.sections.CSList;
import me.blvckbytes.bbconfigmapper.sections.CSMap;
import me.blvckbytes.bbconfigmapper.sections.IConfigSection;
import me.blvckbytes.gpeee.IExpressionEvaluator;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

public class ConfigMapper implements IConfigMapper {

  private final IConfig config;
  private final IExpressionEvaluator evaluator;
  private final Map<String, ConfigValue> cache;

  /**
   * Create a new config reader on a {@link IConfig}
   * @param config Configuration to read from
   * @param evaluator Expression evaluator instance to use when parsing expressions
   */
  public ConfigMapper(IConfig config, IExpressionEvaluator evaluator) {
    this.config = config;
    this.evaluator = evaluator;
    this.cache = new HashMap<>();
  }

  @Override
  public @Nullable IEvaluable get(String key) {
    if (cache.containsKey(key))
      return cache.get(key);

    Object value;

    // The config has no value assigned to this path
    if (!config.exists(key)) {

      // Check if it's marked as an expression
      String expressionKey = key + "$";
      if (config.exists(expressionKey))
        value = parseExpressions(config.get(expressionKey));

      // Remember that this key has no value assigned to it
      else {
        cache.put(key, null);
        return null;
      }
    }

    // Not marked as an expression
    else
      value = config.get(key);

    ConfigValue result = new ConfigValue(value, evaluator);
    cache.put(key, result);
    return result;
  }

  @Override
  public void set(String key, @Nullable Object value) {
    this.cache.remove(key);
    config.set(key, value);
  }

  @Override
  public <T extends IConfigSection> @Nullable T mapSection(@Nullable String root, Class<T> type) throws Exception {
    T instance = findDefaultConstructor(type).newInstance();
    List<Field> affectedFields = new ArrayList<>();

    Class<?> c = type;
    while (c != Object.class) {
      for (Field f : c.getDeclaredFields()) {
        if (f.isAnnotationPresent(CSIgnore.class))
          continue;

        if (f.getType() == type)
          throw new IllegalStateException("Sections cannot use self-referencing fields");

        f.setAccessible(true);
        affectedFields.add(f);
      }
      c = c.getSuperclass();
    }

    Iterator<Field> fieldI = affectedFields.stream()
      .sorted((a, b) -> {
        if (a.getType() == Object.class && b.getType() == Object.class)
          return 0;

        // Objects are "greater", so they'll be last when sorting ASC
        return a.getType() == Object.class ? 1 : -1;
      }).iterator();

    while (fieldI.hasNext()) {
      Field f = fieldI.next();
      String fName = f.getName();
      Class<?> fieldType = f.getType();

      if (fieldType == Object.class)
        fieldType = instance.runtimeDecide(fName);

      Object value = resolveFieldValue(root, f, fieldType, type);

      if (value == null)
        value = instance.defaultFor(f.getType(), fName);

      f.set(instance, value);
    }

    instance.afterParsing(affectedFields);
    return instance;
  }

  private Object resolveFieldValue(@Nullable String root, Field f, Class<?> type, Class<? extends IConfigSection> container) throws Exception {
    String path = joinPaths(root, f.getName());

    if (IConfigSection.class.isAssignableFrom(type))
      return mapSection(path, type.asSubclass(IConfigSection.class));

    Object value = config.get(path);

    if (value == null)
      return null;

    if (Map.class.isAssignableFrom(type)) {
      CSMap mapMeta = f.getAnnotation(CSMap.class);

      if (mapMeta == null)
        throw new IllegalStateException("Map fields need to be annotated by @CSMap");

      if (!(value instanceof Map))
        return null;

      throw new UnsupportedOperationException("This feature has yet to be implemented");
    }

    if (List.class.isAssignableFrom(type)) {
      CSList listMeta = f.getAnnotation(CSList.class);

      if (listMeta == null)
        throw new IllegalStateException("List fields need to be annotated by @CSList");

      if (!(value instanceof List))
        return null;

      throw new UnsupportedOperationException("This feature has yet to be implemented");
    }

    if (IEvaluable.class.isAssignableFrom(type))
      return new ConfigValue(value, evaluator);

    throw new UnsupportedOperationException("Unsupported field type encountered: " + type);
  }

  private String joinPaths(@Nullable String a, @Nullable String b) {
    if (a == null || a.isBlank())
      return b;

    if (b == null || b.isBlank())
      return a;

    if (a.endsWith(".") && b.startsWith("."))
      return a + b.substring(1);

    if (a.endsWith(".") || b.startsWith("."))
      return a + b;

    return a + "." + b;
  }

  private<T> Constructor<T> findDefaultConstructor(Class<T> type) {
    try {
      Constructor<T> ctor = type.getDeclaredConstructor();
      ctor.setAccessible(true);
      return ctor;
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Please specify an empty default constructor");
    }
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
        input instanceof Byte ||
        input instanceof Short
    );
  }
}
