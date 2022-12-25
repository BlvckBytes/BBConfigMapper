package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.bbconfigmapper.sections.*;
import me.blvckbytes.gpeee.IExpressionEvaluator;
import me.blvckbytes.bbconfigmapper.logging.DebugLogSource;
import me.blvckbytes.gpeee.Tuple;
import me.blvckbytes.gpeee.logging.ILogger;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ConfigMapper implements IConfigMapper {

  private final IConfig config;
  private final ILogger logger;
  private final IExpressionEvaluator evaluator;

  /**
   * Create a new config reader on a {@link IConfig}
   * @param config Configuration to read from
   * @param logger Logger to use for logging events
   * @param evaluator Expression evaluator instance to use when parsing expressions
   */
  public ConfigMapper(IConfig config, ILogger logger, IExpressionEvaluator evaluator) {
    this.config = config;
    this.logger = logger;
    this.evaluator = evaluator;
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
      if (fieldType == Object.class)
        fieldType = instance.runtimeDecide(fName);

      Object value = resolveFieldValue(root, source, f);

      // Couldn't resolve a non-null value, try to ask for a default value
      if (value == null)
        value = instance.defaultFor(f.getType(), fName);

      // Only set if the value isn't null, as the default constructor
      // might have already assigned some default value earlier
      if (value == null)
        continue;

      // Try to convert the value when the field type mismatches
      if (!fieldType.isAssignableFrom(value.getClass()))
        value = tryConvertValue(value, fieldType);

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
   * Tries to convert an object to a given value type
   * @param value Value to convert
   * @param type Target value type
   * @return Converted value or just the passed-through value if no known conversion applied
   */
  private Object tryConvertValue(Object value, Class<?> type) {
    // Wrap a string into an evaluable for this config context
    if (type == IEvaluable.class && value instanceof String)
      return new ConfigValue(value, evaluator);

    // No suitable conversion applied
    return value;
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
   * Handles resolving a field of type map based on a previously looked up value
   * @param f Map field which has to be assigned to
   * @param value Previously looked up value
   * @return Value to assign to the field
   */
  private @Nullable Object handleResolveMapField(Field f, Object value) throws Exception {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "Resolving map field");
    //#endif

    CSMap mapMeta = f.getAnnotation(CSMap.class);

    if (mapMeta == null)
      throw new IllegalStateException("Map fields need to be annotated by @CSMap");

    if (!(value instanceof Map)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Not a map, returning null");
      //#endif
      return null;
    }

    if (mapMeta.k() != IEvaluable.class)
      throw new IllegalStateException("Unsupported map key type specified: " + mapMeta.k());

    Class<?> valueType = mapMeta.v();

    if (IConfigSection.class.isAssignableFrom(valueType)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Mapping values to config sections");
      //#endif

      Map<IEvaluable, Object> result = new HashMap<>();
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
        IEvaluable keyValue = new ConfigValue(entry.getKey(), evaluator);
        Object assignedValue = null;

        if (entry instanceof Map) {
          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.MAPPER, "Mapping key=" + entry.getKey() + " to a section");
          //#endif

          assignedValue = mapSectionSub(null, (Map<?, ?>) entry.getValue(), valueType.asSubclass(IConfigSection.class));
        }

        else {
          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.MAPPER, "The key=" + entry.getKey() + " is not a map, putting null");
          //#endif
        }

        result.put(keyValue, assignedValue);
      }
      return result;
    }

    if (IEvaluable.class.isAssignableFrom(valueType)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Mapping values to evaluables");
      //#endif

      Map<IEvaluable, IEvaluable> result = new HashMap<>();
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.MAPPER, "Mapping key=" + entry.getKey() + " to an evaluable");
        //#endif
        result.put(new ConfigValue(entry.getKey(), evaluator), new ConfigValue(entry.getValue(), evaluator));
      }

      return result;
    }

    throw new IllegalStateException("Unsupported mapping value type specified: " + valueType);
  }

  /**
   * Handles resolving a field of type list based on a previously looked up value
   * @param f List field which has to be assigned to
   * @param value Previously looked up value
   * @return Value to assign to the field
   */
  private @Nullable Object handleResolveListField(Field f, Object value) throws Exception {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "Resolving list field");
    //#endif

    CSList listMeta = f.getAnnotation(CSList.class);
    if (listMeta == null)
      throw new IllegalStateException("List fields need to be annotated by @CSList");

    if (!(value instanceof List)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Not a list, returning null");
      //#endif
      return null;
    }

    Class<?> listType = listMeta.type();
    List<Object> result = new ArrayList<>();

    // Is containing other sections as it's entries
    if (IConfigSection.class.isAssignableFrom(listType)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Mapping items to config sections");
      //#endif

      List<?> list = (List<?>) value;
      for (int i = 0; i < list.size(); i++) {
        Object item = list.get(i);
        Object assignedValue = null;

        if (item instanceof Map) {
          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.MAPPER, "Mapping item index=" + i + " to a section");
          //#endif

          assignedValue = mapSectionSub(null, (Map<?, ?>) item, listType.asSubclass(IConfigSection.class));
        }

        else {
          //#if mvn.project.property.production != "true"
          logger.logDebug(DebugLogSource.MAPPER, "The item index=" + i + " is not a map, putting null");
          //#endif
        }

        result.add(assignedValue);
      }

      return result;
    }

    // Is containing evaluables as it's entries
    if (IEvaluable.class.isAssignableFrom(listType)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Mapping items to evaluables");
      //#endif

      List<?> list = (List<?>) value;
      for (int i = 0; i < list.size(); i++) {
        Object item = list.get(i);
        //#if mvn.project.property.production != "true"
        logger.logDebug(DebugLogSource.MAPPER, "Mapping item index=" + i + " to an evaluable");
        //#endif
        result.add(new ConfigValue(item, evaluator));
      }

      return result;
    }

    throw new IllegalStateException("Unsupported list type specified: " + listType);
  }

  /**
   * Tries to resolve a field's value based on it's type, it's annotations, it's name and
   * the source (either a path or a source map).
   * @param root Root node of this section (null means config root)
   * @param source Map to resolve from instead of querying the config, optional
   * @param f Field which has to be assigned to
   * @return Value to be assigned to the field
   */
  private @Nullable Object resolveFieldValue(@Nullable String root, @Nullable Map<?, ?> source, Field f) throws Exception {
    String path = f.isAnnotationPresent(CSInlined.class) ? root : joinPaths(root, f.getName());
    Class<?> type = f.getType();

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "Resolving value for field=" + f.getName() + " at path=" + path + " using source=" + source);
    //#endif

    if (IConfigSection.class.isAssignableFrom(type)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Type is of another section");
      //#endif
      return mapSectionSub(path, source, type.asSubclass(IConfigSection.class));
    }

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "Resolving path value as plain object");
    //#endif

    Object value = resolvePath(path, source);

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.MAPPER, "Resolved value=" + value);
    //#endif

    if (value == null) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Value is null, returning null");
      //#endif
      return null;
    }

    if (Map.class.isAssignableFrom(type))
      return handleResolveMapField(f, value);

    if (List.class.isAssignableFrom(type))
      return handleResolveListField(f, value);

    if (IEvaluable.class.isAssignableFrom(type)) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.MAPPER, "Value is of type evaluable, returning wrapped value");
      //#endif
      return new ConfigValue(value, evaluator);
    }

    throw new UnsupportedOperationException("Unsupported field type encountered: " + type);
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
