package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.bbconfigmapper.logging.DebugLogSource;
import me.blvckbytes.bbconfigmapper.sections.*;
import me.blvckbytes.gpeee.GPEEE;
import me.blvckbytes.gpeee.IExpressionEvaluator;
import me.blvckbytes.gpeee.Tuple;
import me.blvckbytes.gpeee.logging.ILogger;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ConfigMapper implements IConfigMapper {

  private final IConfig config;
  private final ILogger logger;
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
    ILogger logger,
    IExpressionEvaluator evaluator,
    @Nullable IValueConverterRegistry converterRegistry
  ) {
    this.config = config;
    this.logger = logger;
    this.evaluator = evaluator;
    this.converterRegistry = converterRegistry;
  }

  @Override
  public <T extends IConfigSection> T mapSection(@Nullable String root, Class<T> type) throws Exception {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "At the entry point of mapping path=" + root + " to type=" + type);
    //#endif
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
  private <T extends IConfigSection> T mapSectionSub(@Nullable String root, @Nullable Map<?, ?> source, Class<T> type) throws Exception {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "At the subroutine of mapping path=" + root + " to type=" + type + " using source=" + source);
    //#endif
    T instance = findDefaultConstructor(type).newInstance();

    Tuple<List<Field>, Iterator<Field>> fields = findApplicableFields(type);

    while (fields.getB().hasNext()) {
      Field f = fields.getB().next();
      String fName = f.getName();
      Class<?> fieldType = f.getType();

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Processing field=" + fName + " of type=" + fieldType);
      //#endif

      // Object fields trigger a call to runtime decide their type based on previous fields
      if (fieldType == Object.class) {
        Class<?> decidedType = instance.runtimeDecide(fName);

        if (decidedType != null) {
          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.MAPPER, "Called runtimeDecide on field=" + fName + ", yielded type=" + decidedType);
          //#endif
          fieldType = decidedType;
        }
      }

      Object value = resolveFieldValue(root, source, f, fieldType);

      // Couldn't resolve a non-null value, try to ask for a default value
      if (value == null)
        value = instance.defaultFor(f.getType(), fName);

      // Only set if the value isn't null, as the default constructor
      // might have already assigned some default value earlier
      if (value == null)
        continue;

      f.set(instance, value);
    }

    // This instance won't have any more changes applied to it, call with the list of affected fields
    instance.afterParsing(fields.getA());

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

    return Tuple.of(affectedFields, fieldI);
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
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "No resolving source provided, looking up in config");
      //#endif
      return config.get(path);
    }

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "Resolving source provided, walking map");
    //#endif

    int dotIndex = path.indexOf('.');

    while (path.length() > 0) {
      String key = dotIndex < 0 ? path : path.substring(0, dotIndex);

      if (key.isBlank())
        throw new IllegalStateException("Cannot resolve a blank key");

      path = dotIndex < 0 ? "" : path.substring(dotIndex + 1);
      dotIndex = path.indexOf('.');

      Object value = source.get(key);

      // Last iteration, respond with the current value
      if (path.length() == 0) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.MAPPER, "Walk ended, returning value=" + value);
        //#endif
        return value;
      }

      // Reached a dead end and not yet at the last iteration
      if (!(value instanceof Map)) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.MAPPER, "Path part key=" + key + " wasn't a map, returning null");
        //#endif
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
   * wrapping the value as an {@link IEvaluable} or by parsing a {@link IConfigSection}
   * if the input is of type map and returning null otherwise. Unsupported types throw.
   * @param input Input object to convert
   * @param type Type to convert to
   */
  private @Nullable Object convertType(@Nullable Object input, Class<?> type) throws Exception {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "Trying to convert a value to type: " + type);
    //#endif

    if (input == null) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Is null, returning null");
      //#endif
      return null;
    }

    if (IConfigSection.class.isAssignableFrom(type)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Parsing value as config-section");
      //#endif

      if (!(input instanceof Map)) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.MAPPER, "Value was null, falling back on empty section");
        //#endif
        input = new HashMap<>();
      }

      return mapSectionSub(null, (Map<?, ?>) input, type.asSubclass(IConfigSection.class));
    }

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "Wrapping value in evaluable");
    //#endif

    IEvaluable evaluable = new ConfigValue(input, this.evaluator);

    if (type == IEvaluable.class) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Returning evaluable");
      //#endif
      return evaluable;
    }

    if (type == String.class)
      return evaluable.<String>asScalar(ScalarType.STRING, GPEEE.EMPTY_ENVIRONMENT);

    if (type == int.class || type == Integer.class)
      return evaluable.<Long>asScalar(ScalarType.LONG, GPEEE.EMPTY_ENVIRONMENT).intValue();

    if (type == long.class || type == Long.class)
      return evaluable.<Long>asScalar(ScalarType.LONG, GPEEE.EMPTY_ENVIRONMENT);

    if (type == double.class || type == Double.class)
      return evaluable.<Double>asScalar(ScalarType.DOUBLE, GPEEE.EMPTY_ENVIRONMENT);

    if (type == float.class || type == Float.class)
      return evaluable.<Double>asScalar(ScalarType.DOUBLE, GPEEE.EMPTY_ENVIRONMENT).floatValue();

    if (type == boolean.class || type == Boolean.class)
      return evaluable.<Boolean>asScalar(ScalarType.BOOLEAN, GPEEE.EMPTY_ENVIRONMENT);

    // Look through the converter registry to find a custom converter for this type
    FValueConverter converter;
    if (converterRegistry != null && (converter = converterRegistry.getConverterFor(type)) != null) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Applying custom converter for type=" + type);
      //#endif
      return converter.apply(evaluable);
    }

    throw new IllegalStateException("Unsupported type specified: " + type);
  }

  /**
   * Handles resolving a field of type map based on a previously looked up value
   * @param f Map field which has to be assigned to
   * @param value Previously looked up value
   * @return Value to assign to the field
   */
  private Object handleResolveMapField(Field f, Object value) throws Exception {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "Resolving map field");
    //#endif

    CSMap mapMeta = f.getAnnotation(CSMap.class);

    if (mapMeta == null)
      throw new IllegalStateException("Map fields need to be annotated by @CSMap");

    Map<Object, Object> result = new HashMap<>();

    if (!(value instanceof Map)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Not a map, returning empty map");
      //#endif
      return result;
    }

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "Mapping values individually");
    //#endif

    for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet())
      result.put(convertType(entry.getKey(), mapMeta.k()), convertType(entry.getValue(), mapMeta.v()));

    return result;
  }

  /**
   * Handles resolving a field of type list based on a previously looked up value
   * @param f List field which has to be assigned to
   * @param value Previously looked up value
   * @return Value to assign to the field
   */
  private Object handleResolveListField(Field f, Object value) throws Exception {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "Resolving list field");
    //#endif

    CSList listMeta = f.getAnnotation(CSList.class);
    if (listMeta == null)
      throw new IllegalStateException("List fields need to be annotated by @CSList");

    List<Object> result = new ArrayList<>();

    if (!(value instanceof List)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Not a list, returning empty list");
      //#endif
      return result;
    }

    List<?> list = (List<?>) value;
    for (Object o : list)
      result.add(convertType(o, listMeta.type()));

    return result;
  }

  /**
   * Handles resolving a field of type array based on a previously looked up value
   * @param f List field which has to be assigned to
   * @param value Previously looked up value
   * @return Value to assign to the field
   */
  private Object handleResolveArrayField(Field f, Object value) throws Exception {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "Resolving array field");
    //#endif

    Class<?> arrayType = f.getType().getComponentType();

    if (!(value instanceof List)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Not a list, returning empty array");
      //#endif
      return Array.newInstance(arrayType, 0);
    }

    List<?> list = (List<?>) value;
    Object array = Array.newInstance(arrayType, list.size());

    for (int i = 0; i < list.size(); i++)
      Array.set(array, i, convertType(list.get(i), arrayType));

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

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "Resolving value for field=" + f.getName() + " at path=" + path + " using source=" + source);
    //#endif

    if (IConfigSection.class.isAssignableFrom(type)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Type is of another section");
      //#endif

      // It's not marked as always and the current path doesn't exist: return null
      if (!always && resolvePath(path, source) == null) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.MAPPER, "Returning null for absent section");
        //#endif
        return null;
      }

      return mapSectionSub(path, source, type.asSubclass(IConfigSection.class));
    }

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "Resolving path value as plain object");
    //#endif

    Object value = resolvePath(path, source);

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "Resolved value=" + value);
    //#endif

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

  /**
   * Find the default constructor of a class (no parameters required to instantiate it)
   * or throw a runtime exception otherwise.
   * @param type Type of the target class
   * @return Default constructor
   */
  private<T> Constructor<T> findDefaultConstructor(Class<T> type) {
    try {
      Constructor<T> ctor = type.getDeclaredConstructor();
      ctor.setAccessible(true);
      return ctor;
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Please specify an empty default constructor");
    }
  }
}
