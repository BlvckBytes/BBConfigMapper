package me.blvckbytes.bbconfigmapper.yaml;

import me.blvckbytes.bbconfigmapper.IConfig;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class YamlConfig implements IConfig {

  private final YamlMapping rootNode;

  public YamlConfig(String input) {
    // TODO: Parse from input
    rootNode = new YamlMapping(new HashMap<>());
  }

  @Override
  public @Nullable Object get(String key) {
    return locateNode(key);
  }

  @Override
  public void set(String key, @Nullable Object value) {
    YamlMapping targetNode = (YamlMapping) splitKeyAndWalkPath(key, (AYamlValue) rootNode, (part, node) -> {
      // Create non-existing nodes or override nodes of non-map type
      if (!(node instanceof YamlMapping))
        return new YamlMapping(new HashMap<>());
      return node;
    });

    targetNode.storeKey(key, wrapValue(value));
  }

  @Override
  public boolean exists(String key) {
    return locateNode(key) != null;
  }

  @Override
  public boolean attachComment(String key, List<String> lines) {
    AYamlValue target = locateNode(key);

    if (target == null)
      return false;

    YamlComment comment = target.getComment();

    if (comment == null) {
      comment = new YamlComment(lines);
      target.setComment(comment);
      return true;
    }

    comment.setLines(lines);
    return true;
  }

  @Override
  public @Nullable List<String> readComment(String key) {
    AYamlValue target = locateNode(key);

    if (target == null || target.getComment() == null)
      return null;

    return target.getComment().getLines();
  }

  /**
   * Tries to wrap an object in a yaml tree node, throws an {@link IllegalArgumentException}
   * for value types which cannot be depicted as a yaml value directly
   * @param value Value to wrap
   * @return Wrapped value
   */
  private AYamlValue wrapValue(@Nullable Object value) {
    if (value == null)
      return new YamlNull();

    if (value instanceof Float || value instanceof Double)
      return new YamlDouble(((Number) value).doubleValue());

    if (value instanceof Integer || value instanceof Long || value instanceof Byte || value instanceof Short)
      return new YamlLong(((Number) value).longValue());

    if (value instanceof List) {
      List<AYamlValue> values = new ArrayList<>();

      for (Object item : (List<?>) value)
        values.add(wrapValue(item));

      return new YamlSequence(values);
    }

    if (value instanceof Map) {
      Map<String, AYamlValue> values = new HashMap<>();

      for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet())
        values.put(String.valueOf(entry.getKey()), wrapValue(entry.getValue()));

      return new YamlMapping(values);
    }

    if (value instanceof String)
      return new YamlString(String.valueOf(value));

    throw new IllegalArgumentException("Cannot store a value of type " + value.getClass());
  }

  /**
   * Locates a target node by it's identifying key
   * @param key Key to search for
   * @return Target node or null if the target node didn't exist
   */
  private @Nullable AYamlValue locateNode(String key) {
    return splitKeyAndWalkPath(key, (AYamlValue) rootNode, (part, node) -> {
      // Not a mapping node, cannot look up a path-part, the key has to be invalid
      if (!(node instanceof YamlMapping))
        return null;

      return ((YamlMapping) node).lookupKey(part);
    });
  }

  /**
   * Splits the provided key at every dot and invokes the consumer with the current path-part
   * as well as the initial/last input value. This common routine is used to walk a key in
   * parts and handle the corresponding node at each iteration separately.
   * @param key Key to split and walk
   * @param input Initial input argument to the consumer
   * @param consumer Consumer of path-parts, receiving the last returned or the initial input argument
   * @return Last returned value by the consumer, or the initial input value on empty strings
   */
  private<T> T splitKeyAndWalkPath(String key, T input, BiFunction<String, T, T> consumer) {
    // Keys should never contain any whitespace
    key = key.trim();

    while (key.length() > 0) {
      int dotPos = key.indexOf('.');
      int pathPartEnd = dotPos < 0 ? key.length() : dotPos;
      String pathPart = key.substring(0, pathPartEnd);

      input = consumer.apply(pathPart, input);

      if (input == null)
        break;

      key = dotPos < 0 ? "" : key.substring(pathPartEnd + 1);
    }

    return input;
  }
}
