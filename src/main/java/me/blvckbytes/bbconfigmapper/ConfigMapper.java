/*
 * MIT License
 *
 * Copyright (c) 2023 BlvckBytes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.bbconfigmapper.logging.DebugLogSource;
import me.blvckbytes.bbconfigmapper.sections.*;
import me.blvckbytes.gpeee.GPEEE;
import me.blvckbytes.gpeee.IExpressionEvaluator;
import me.blvckbytes.gpeee.Tuple;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ConfigMapper implements IConfigMapper {

  private final IConfig config;

  private final Logger logger;
  private final IExpressionEvaluator evaluator;
  private final @Nullable IValueConverterRegistry converterRegistry;

  /**
   * Create a new config reader on a {@link IConfig}
   * @param config Configuration to read from
   * @param logger Logger to use for logging events
   * @param evaluator Expression evaluator instance to use when parsing expressions
   * @param converterRegistry Optional registry of custom value converters
   */
  public ConfigMapper(
    IConfig config,
    Logger logger,
    IExpressionEvaluator evaluator,
    @Nullable IValueConverterRegistry converterRegistry
  ) {
    this.config = config;
    this.logger = logger;
    this.evaluator = evaluator;
    this.converterRegistry = converterRegistry;
  }

  @Override
  public IConfig getConfig() {
    return config;
  }

  @Override
  public <T extends AConfigSection> T mapSection(@Nullable String root, Class<T> type) throws Exception {
    logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "At the entry point of mapping path=" + root + " to type=" + type);
    return mapSectionSub(root, null, type);
  }

  /**
   * Recursive, parameterized subroutine for creating an empty config section and then assigning values
   * to it's mapped fields automatically, based on their names and types by making use of
   * {@link #resolveFieldValue}. Fields of type object will be decided at
   * runtime, null values may get a default value assigned and incompatible values are tried to be
   * converted before invoking the field setter. If a value still is null after all calls, the field
   * remains unchanged.
   * @param root Root node of this section (null means config root)
   * @param source Alternative value source (map instead of config lookup)
   * @param type Class of the config section to instantiate
   * @return Instantiated class with mapped fields
   */
  private <T extends AConfigSection> T mapSectionSub(@Nullable String root, @Nullable Map<?, ?> source, Class<T> type) throws Exception {
      logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "At the subroutine of mapping path=" + root + " to type=" + type + " using source=" + source);
      T instance = findStandardConstructor(type).newInstance(evaluator.getBaseEnvironment());

      Tuple<List<Field>, Iterator<Field>> fields = findApplicableFields(type);

      while (fields.b.hasNext()) {
        Field f = fields.b.next();
        String fName = f.getName();

        try {
          Class<?> fieldType = f.getType();

          Class<?> finalFieldType = fieldType;
          logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Processing field=" + fName + " of type=" + finalFieldType);

          // Object fields trigger a call to runtime decide their type based on previous fields
          if (fieldType == Object.class) {
            Class<?> decidedType = instance.runtimeDecide(fName);

            if (decidedType == null)
              throw new MappingError("Requesting plain objects is disallowed");

            logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Called runtimeDecide on field=" + fName + ", yielded type=" + decidedType);

            fieldType = decidedType;
          }

          FValueConverter converter = null;
          if (converterRegistry != null) {
            Class<?> requiredType = converterRegistry.getRequiredTypeFor(fieldType);
            converter = converterRegistry.getConverterFor(fieldType);

            if (requiredType != null && converter != null) {
              logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Using custom converter for type=" + finalFieldType);

              fieldType = requiredType;
            }
          }

          Object value = resolveFieldValue(root, source, f, fieldType);

          // Couldn't resolve a non-null value, try to ask for a default value
          if (value == null)
            value = instance.defaultFor(f);

          if (value != null && converter != null)
            value = converter.apply(value, evaluator);

          // Only set if the value isn't null, as the default constructor
          // might have already assigned some default value earlier
          if (value == null)
            continue;

          f.set(instance, value);
        } catch (MappingError error) {
          IllegalStateException exception = new IllegalStateException(error.getMessage() + " (at path '" + joinPaths(root, fName) + "')");
          exception.addSuppressed(error);
          throw exception;
        }
      }

      // This instance won't have any more changes applied to it, call with the list of affected fields
      instance.afterParsing(fields.a);

      return instance;
  }

  /**
   * Find all fields of a class which automated mapping applies to, including inherited fields
   * @param type Class to look through
   * @return A tuple containing the unsorted list as well as an iterator of fields in
   *         the order that fields of type Object come after known types
   */
  private Tuple<List<Field>, Iterator<Field>> findApplicableFields(Class<?> type) {
    List<Field> affectedFields = new ArrayList<>();

    // Walk the class' hierarchy
    Class<?> c = type;
    while (c != Object.class) {
      for (Field f : c.getDeclaredFields()) {
        if (Modifier.isStatic(f.getModifiers()))
          continue;

        if (f.isAnnotationPresent(CSIgnore.class))
          continue;

        if (f.getType() == type)
          throw new IllegalStateException("Sections cannot use self-referencing fields (" + type + ", " + f.getName() + ")");

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

    return new Tuple<>(affectedFields, fieldI);
  }

  /**
   * Resolve a path by either looking it up in the config itself or by resolving it
   * from a previous config response which occurred in the form of a map
   * @param path Path to resolve
   * @param source Map to resolve from instead of querying the config, optional
   * @return Resolved value, null if either the value was null or if it wasn't available
   */
  private @Nullable Object resolvePath(String path, @Nullable Map<?, ?> source) {
    // No object to look in specified, retrieve this path from the config
    if (source == null) {
      logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "No resolving source provided, looking up in config");
      return config.get(path);
    }

    logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Resolving source provided, walking map");

    int dotIndex = path.indexOf('.');

    while (path.length() > 0) {
      String key = dotIndex < 0 ? path : path.substring(0, dotIndex);

      if (StringUtils.isBlank(key))
        throw new MappingError("Cannot resolve a blank key");

      path = dotIndex < 0 ? "" : path.substring(dotIndex + 1);
      dotIndex = path.indexOf('.');

      Object value = source.get(key);

      // Last iteration, respond with the current value
      if (path.length() == 0) {
        logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Walk ended, returning value=" + value);
        return value;
      }

      // Reached a dead end and not yet at the last iteration
      if (!(value instanceof Map)) {
        logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Path part key=" + key + " wasn't a map, returning null");
        return null;
      }

      // Swap out the current map reference to navigate forwards
      source = (Map<?, ?>) value;
    }

    // Path was blank, which means root
    return source;
  }

  /**
   * Tries to convert the input object to the specified type, by either stringifying,
   * wrapping the value as an {@link IEvaluable} or by parsing a {@link AConfigSection}
   * if the input is of type map and returning null otherwise. Unsupported types throw.
   * @param input Input object to convert
   * @param type Type to convert to
   */
  private @Nullable Object convertType(@Nullable Object input, Class<?> type) throws Exception {

    Class<?> finalType = type;
    logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Trying to convert a value to type: " + finalType);

    if (input == null) {
      logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Is null, returning null");
      return null;
    }

    FValueConverter converter = null;
    if (converterRegistry != null) {
      Class<?> requiredType = converterRegistry.getRequiredTypeFor(type);
      converter = converterRegistry.getConverterFor(type);

      if (requiredType != null && converter != null) {
        logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Using custom converter for type=" + finalType);

        type = requiredType;
      }
    }

    // Requested plain object
    if (type == Object.class) {
      if (converter != null)
        input = converter.apply(input, evaluator);

      return input;
    }

    if (type.isEnum()) {
      String upperInput = input.toString().toUpperCase(Locale.ROOT);
      Object[] enumConstants = type.getEnumConstants();

      for (Object enumConstant : enumConstants) {
        if (((Enum<?>)enumConstant).name().equals(upperInput))
          return enumConstant;
      }

      String existingConstants = Arrays.stream(enumConstants)
        .map(it -> ((Enum<?>) it).name())
        .collect(Collectors.joining(", "));

      throw new MappingError("Value \"" + input + "\" was not one of " + existingConstants);
    }

    if (AConfigSection.class.isAssignableFrom(type)) {
      logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Parsing value as config-section");

      if (!(input instanceof Map)) {
        logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Value was null, falling back on empty section");
        input = new HashMap<>();
      }

      Object value = mapSectionSub(null, (Map<?, ?>) input, type.asSubclass(AConfigSection.class));

      if (converter != null)
        value = converter.apply(value, evaluator);

      return value;
    }

    logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Wrapping value in evaluable");

    IEvaluable evaluable = new ConfigValue(input, this.evaluator);

    if (IEvaluable.class.isAssignableFrom(type)) {
      logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Returning evaluable");
      return evaluable;
    }

    if (type == String.class)
      return evaluable.asScalar(ScalarType.STRING, GPEEE.EMPTY_ENVIRONMENT);

    if (type == int.class || type == Integer.class)
      return evaluable.asScalar(ScalarType.LONG, GPEEE.EMPTY_ENVIRONMENT).intValue();

    if (type == long.class || type == Long.class)
      return evaluable.asScalar(ScalarType.LONG, GPEEE.EMPTY_ENVIRONMENT);

    if (type == double.class || type == Double.class)
      return evaluable.asScalar(ScalarType.DOUBLE, GPEEE.EMPTY_ENVIRONMENT);

    if (type == float.class || type == Float.class)
      return evaluable.asScalar(ScalarType.DOUBLE, GPEEE.EMPTY_ENVIRONMENT).floatValue();

    if (type == boolean.class || type == Boolean.class)
      return evaluable.asScalar(ScalarType.BOOLEAN, GPEEE.EMPTY_ENVIRONMENT);

    throw new MappingError("Unsupported type specified: " + type);
  }

  /**
   * Handles resolving a field of type map based on a previously looked up value
   * @param f Map field which has to be assigned to
   * @param value Previously looked up value
   * @return Value to assign to the field
   */
  private Object handleResolveMapField(Field f, Object value) throws Exception {
    logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Resolving map field");

    List<Class<?>> genericTypes = getGenericTypes(f);
    assert genericTypes != null && genericTypes.size() == 2;

    Map<Object, Object> result = new HashMap<>();

    if (!(value instanceof Map)) {
      logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Not a map, returning empty map");
      return result;
    }

    logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Mapping values individually");

    for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
      Object resultKey;
      try {
        resultKey = convertType(entry.getKey(), genericTypes.get(0));
      } catch (MappingError error) {
        throw new MappingError(error.getMessage() + " (at the key of a map)");
      }

      Object resultValue;
      try {
        resultValue = convertType(entry.getValue(), genericTypes.get(1));
      } catch (MappingError error) {
        throw new MappingError(error.getMessage() + " (at value for key=" + resultKey + " of a map)");
      }

      result.put(resultKey, resultValue);
    }

    return result;
  }

  /**
   * Handles resolving a field of type list based on a previously looked up value
   * @param f List field which has to be assigned to
   * @param value Previously looked up value
   * @return Value to assign to the field
   */
  private Object handleResolveListField(Field f, Object value) throws Exception {
    logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Resolving list field");

    List<Class<?>> genericTypes = getGenericTypes(f);
    assert genericTypes != null && genericTypes.size() == 1;

    List<Object> result = new ArrayList<>();

    if (!(value instanceof List)) {
      logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Not a list, returning empty list");
      return result;
    }

    List<?> list = (List<?>) value;
    for (int i = 0; i < list.size(); i++) {
      Object itemValue;
      try {
        itemValue = convertType(list.get(i), genericTypes.get(0));
      } catch (MappingError error) {
        throw new MappingError(error.getMessage() + " (at index " + i + " of a list)");
      }

      result.add(itemValue);
    }

    return result;
  }

  /**
   * Handles resolving a field of type array based on a previously looked up value
   * @param f List field which has to be assigned to
   * @param value Previously looked up value
   * @return Value to assign to the field
   */
  private Object handleResolveArrayField(Field f, Object value) throws Exception {
    logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Resolving array field");

    Class<?> arrayType = f.getType().getComponentType();

    if (!(value instanceof List)) {
      logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Not a list, returning empty array");
      return Array.newInstance(arrayType, 0);
    }

    List<?> list = (List<?>) value;
    Object array = Array.newInstance(arrayType, list.size());

    for (int i = 0; i < list.size(); i++) {
      Object itemValue;
      try {
        itemValue = convertType(list.get(i), arrayType);
      } catch (MappingError error) {
        throw new MappingError(error.getMessage() + " (at index " + i + " of an array)");
      }

      Array.set(array, i, itemValue);
    }

    return array;
  }

  /**
   * Tries to resolve a field's value based on it's type, it's annotations, it's name and
   * the source (either a path or a source map).
   * @param root Root node of this section (null means config root)
   * @param source Map to resolve from instead of querying the config, optional
   * @param f Field which has to be assigned to
   * @return Value to be assigned to the field
   */
  private @Nullable Object resolveFieldValue(@Nullable String root, @Nullable Map<?, ?> source, Field f, Class<?> type) throws Exception {
    String path = f.isAnnotationPresent(CSInlined.class) ? root : joinPaths(root, f.getName());
    boolean always = f.isAnnotationPresent(CSAlways.class) || f.getDeclaringClass().isAnnotationPresent(CSAlways.class);

    logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Resolving value for field=" + f.getName() + " at path=" + path + " using source=" + source);

    Object value = resolvePath(path, source);

    // It's not marked as always and the current path doesn't exist: return null
    if (!always && value == null) {
      logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Returning null for absent path");
      return null;
    }

    if (AConfigSection.class.isAssignableFrom(type)) {
      logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Type is of another section");
      return mapSectionSub(path, source, type.asSubclass(AConfigSection.class));
    }

    logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Resolving path value as plain object");

    // Requested plain object
    if (type == Object.class)
      return value;

    logger.log(Level.FINEST, () -> DebugLogSource.MAPPER + "Resolved value=" + value);

    if (Map.class.isAssignableFrom(type))
      return handleResolveMapField(f, value);

    if (List.class.isAssignableFrom(type))
      return handleResolveListField(f, value);

    if (type.isArray())
      return handleResolveArrayField(f, value);

    return convertType(value, type);
  }

  /**
   * Join two config paths and account for all possible cases
   * @param a Path A (or null/empty)
   * @param b Path B (or null/empty)
   * @return Path A joined with path B
   */
  private String joinPaths(@Nullable String a, @Nullable String b) {
    if (a == null || StringUtils.isBlank(a))
      return b;

    if (b == null || StringUtils.isBlank(b))
      return a;

    if (a.endsWith(".") && b.startsWith("."))
      return a + b.substring(1);

    if (a.endsWith(".") || b.startsWith("."))
      return a + b;

    return a + "." + b;
  }

  /**
   * Find the standard constructor of a class: constructor(EvaluationEnvironmentBuilder)
   * or throw a runtime exception otherwise.
   * @param type Type of the target class
   * @return Standard constructor
   */
  private<T> Constructor<T> findStandardConstructor(Class<T> type) {
    try {
      Constructor<T> constructor = type.getDeclaredConstructor(EvaluationEnvironmentBuilder.class);

      if (!Modifier.isPublic(constructor.getModifiers()))
        throw new IllegalStateException("The standard-constructor of a config-section has to be public");

      return constructor;
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Please specify a standard-constructor taking an EvaluationEnvironmentBuilder on " + type);
    }
  }

  /**
   * Get a list of generic types a field's type declares
   * @param f Target field
   * @return List of generic fields, null if the field's type is not generic
   */
  private @Nullable List<Class<?>> getGenericTypes(Field f) {
    Type genericType = f.getGenericType();

    if (!(genericType instanceof ParameterizedType))
      return null;

    Type[] types = ((ParameterizedType) genericType).getActualTypeArguments();
    List<Class<?>> result = new ArrayList<>();

    for (Type type : types)
      result.add(unwrapType(type));

    return result;
  }

  /**
   * Attempts to unwrap a given type to its raw type class
   * @param type Type to unwrap
   * @return Unwrapped type
   */
  private Class<?> unwrapType(Type type) {
    if (type instanceof Class)
      return (Class<?>) type;

    if (type instanceof ParameterizedType)
      return unwrapType(((ParameterizedType) type).getRawType());

    throw new MappingError("Cannot unwrap type of class=" + type.getClass());
  }
}
