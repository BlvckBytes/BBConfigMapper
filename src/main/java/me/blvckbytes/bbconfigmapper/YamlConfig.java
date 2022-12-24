package me.blvckbytes.bbconfigmapper;

import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.*;
import java.util.function.BiConsumer;

public class YamlConfig implements IConfig {

  private static final LoaderOptions LOADER_OPTIONS;
  private static final DumperOptions DUMPER_OPTIONS;

  private final Yaml yaml;
  private final Map<MappingNode, Map<String, @Nullable NodeTuple>> locateKeyCache;
  private MappingNode rootNode;

  static {
    LOADER_OPTIONS = new LoaderOptions();
    LOADER_OPTIONS.setProcessComments(true);
    LOADER_OPTIONS.setAllowDuplicateKeys(false);

    DUMPER_OPTIONS = new DumperOptions();
    DUMPER_OPTIONS.setProcessComments(true);
  }

  public YamlConfig() {
    this.yaml = new Yaml(new Constructor(LOADER_OPTIONS), new Representer(DUMPER_OPTIONS), DUMPER_OPTIONS, LOADER_OPTIONS);
    this.locateKeyCache = new HashMap<>();
  }

  public void load(Reader reader) {
    Iterator<Node> nodes = yaml.composeAll(reader).iterator();

    if (!nodes.hasNext())
      throw new IllegalStateException("No node available");

    Node root = nodes.next();

    if (nodes.hasNext())
      throw new IllegalStateException("Encountered multiple nodes");

    if (!(root instanceof MappingNode))
      throw new IllegalStateException("The top level of a config has to be a map.");

    // Swap out root node and reset key cache
    this.rootNode = (MappingNode) root;
    this.locateKeyCache.clear();
  }

  public void save(Writer writer) throws IOException {
    if (this.rootNode == null) {
      writer.write("");
      return;
    }

    yaml.serialize(this.rootNode, writer);
  }

  @Override
  public @Nullable Object get(String path) {
    Node target = locateNode(path, false, false);
    return target == null ? null : unwrapNode(target);
  }

  @Override
  public void set(String path, @Nullable Object value) {
    updatePathValue(path, wrapValue(value), true);
  }

  @Override
  public void remove(String path) {
    updatePathValue(path, null, false);
  }

  @Override
  public boolean exists(String path) {
    // For a key to exist, it's path has to exist within the
    // config, even if it points at a null value
    return locateNode(path, true, false) != null;
  }

  @Override
  public void attachComment(String path, List<String> lines, boolean self) {
    Node target = locateNode(path, self, false);

    if (target == null)
      throw new IllegalStateException("Cannot attach a comment to a non-existing path");

    List<CommentLine> comments = new ArrayList<>();

    for (String line : lines) {
      CommentType type = line.isBlank() ? CommentType.BLANK_LINE : CommentType.BLOCK;
      comments.add(new CommentLine(null, null, line, type));
    }

    target.setBlockComments(comments);
  }

  @Override
  public @Nullable List<String> readComment(String path, boolean self) {
    Node target = locateNode(path, self, false);

    if (target == null)
      return null;

    List<String> comments = new ArrayList<>();
    List<CommentLine> targetComments = target.getBlockComments();

    if (targetComments == null)
      return comments;

    for (CommentLine comment : targetComments) {
      if (comment.getCommentType() == CommentType.BLANK_LINE) {
        comments.add("\n");
        continue;
      }

      comments.add(comment.getValue());
    }

    return comments;
  }

  /**
   * Update the value at the key a given path points to within the tree
   * @param keyPath Path to change the value at
   * @param value New value node, leave null to just remove this node
   */
  private void updatePathValue(String keyPath, @Nullable Node value, boolean forceCreateMappings) {
    int lastDotIndex = keyPath.lastIndexOf('.');
    String keyPart;
    MappingNode container;

    // No dot in the path, the container is root and the key-part is just the path value
    if (lastDotIndex < 0) {
      container = rootNode;
      keyPart = keyPath;
    }

    // Get the value after the last dot as a substring and use that as the key-part
    // Look up the container by the path provided before the last dot (in force mapping creation mode)
    else {
      keyPart = keyPath.substring(lastDotIndex + 1);
      container = (MappingNode) locateNode(keyPath.substring(0, lastDotIndex), false, forceCreateMappings);
    }

    if (container == null || keyPart.isBlank())
      throw new IllegalStateException("Invalid path specified: " + keyPath);

    // Check if there's an existing tuple
    NodeTuple existingTuple = locateKey(container, keyPart);
    Node existingKey = null;

    invalidateLocateKeyCacheFor(container, keyPart);

    // Remove an existing tuple from the map
    if (existingTuple != null) {
      existingKey = existingTuple.getKeyNode();
      container.getValue().remove(existingTuple);

      // If the just removed tuple held a mapping node as it#s value, invalidate
      // all children mappings within that tuple recursively
      Node valueNode = existingTuple.getValueNode();
      if (valueNode instanceof MappingNode)
        forAllMappingsRecursively((MappingNode) valueNode, this::invalidateLocateKeyCacheFor);
    }

    // Create a new tuple for this value, if provided
    if (value != null)
      container.getValue().add(createNewTuple(existingKey, keyPart, value));
  }

  /**
   * Invalidate a {@link #locateKey(MappingNode, String)} request cache entry for
   * a specific node targeting a specific key lookup
   * @param node Target node
   * @param key Target key
   */
  private void invalidateLocateKeyCacheFor(MappingNode node, String key) {
    Map<String, @Nullable NodeTuple> containerCache = this.locateKeyCache.get(node);
    if (containerCache != null)
      containerCache.remove(key);
  }

  /**
   * Call the consumer on all instances of a {@link MappingNode} within the recursive children of
   * the provided parent mapping node if that child is registered using a string key
   * @param node Parent mapping node
   * @param consumer Consumer of a recursive child and it's string key
   */
  private void forAllMappingsRecursively(MappingNode node, BiConsumer<MappingNode, String> consumer) {
    for (NodeTuple tuple : node.getValue()) {
      Node valueNode = tuple.getValueNode();
      Node keyNode = tuple.getKeyNode();

      if (valueNode instanceof MappingNode && keyNode instanceof ScalarNode) {
        String keyString = ((ScalarNode) keyNode).getValue();
        consumer.accept(node, keyString);
        forAllMappingsRecursively((MappingNode) valueNode, consumer);
      }
    }
  }

  /**
   * Create a new tuple by mapping either an existing key node or a new
   * {@link ScalarNode} key from a string to a node value
   * @param keyNode Key node to use
   * @param key Key string to use to create a new key node
   * @param value Node value to assign to this key
   * @return Node tuple containing the key-value pair
   */
  private NodeTuple createNewTuple(@Nullable Node keyNode, @Nullable String key, Node value) {
    if (keyNode == null) {

      if (key == null)
        throw new IllegalStateException("Cannot omit both the keyNode and the key");

      keyNode = new ScalarNode(Tag.STR, key, null, null, DumperOptions.ScalarStyle.PLAIN);
    }

    return new NodeTuple(keyNode, value);
  }

  /**
   * Locates a target node by it's identifying path
   * @param path Path to search for
   * @param self Whether to locate the containing key or the value (self means the key)
   * @return Target node or null if the target node didn't exist
   */
  private @Nullable Node locateNode(String path, boolean self, boolean forceCreateMappings) {
    // Keys should never contain any whitespace
    path = path.trim();

    Node node = rootNode;

    int endIndex = path.indexOf('.'), beginIndex = 0;

    while (true) {

      // No next dot available, go until the end of the path string
      if (endIndex < 0)
        endIndex = path.length();

      // Substring between the current begin (inclusive) and the dot position (exclusive)
      String pathPart = path.substring(beginIndex, endIndex);

      // Not a mapping node, cannot look up a path-part, the key has to be invalid
      if (!(node instanceof MappingNode))
        return null;

      MappingNode mapping = (MappingNode) node;
      NodeTuple keyValueTuple = locateKey(mapping, pathPart);

      // Target tuple could not be located or is of wrong value type, create a
      // new tuple of value type mapping and set it within the tree
      if (forceCreateMappings && (keyValueTuple == null || !(keyValueTuple.getValueNode() instanceof MappingNode))) {
        // Try to reuse already present key nodes
        Node tupleKey = keyValueTuple == null ? null : keyValueTuple.getKeyNode();

        keyValueTuple = createNewTuple(tupleKey, pathPart, new MappingNode(Tag.MAP, true, new ArrayList<>(), null, null, DumperOptions.FlowStyle.AUTO));
        mapping.getValue().add(keyValueTuple);

        // Invalidate the (null) cache for this newly added tuple
        invalidateLocateKeyCacheFor(mapping, pathPart);
      }

      // Current path-part does not exist
      if (keyValueTuple == null)
        return null;

      // On the last iteration and the key itself has been requested
      if (endIndex == path.length() && self)
        node = keyValueTuple.getKeyNode();
      else
        node = keyValueTuple.getValueNode();

      // Just processed until the end of the path string, exit
      if (endIndex == path.length())
        break;

      // Go to the next key
      beginIndex = endIndex + 1;
      endIndex = path.indexOf('.', beginIndex);

      if (node == null)
        break;
    }

    return node;
  }

  /**
   * Locates a target key's containing node tuple within a mapping node's list of tuples
   * @param node Node to search in
   * @param key Target key
   * @return Target tuple if found, null on absent key
   */
  private @Nullable NodeTuple locateKey(MappingNode node, String key) {
    Map<String, @Nullable NodeTuple> nodeCache = locateKeyCache.computeIfAbsent(node, k -> new HashMap<>());

    // Check cache before going through linear search
    if (nodeCache.containsKey(key))
      return nodeCache.get(key);

    // Loop all mappings of this key
    List<NodeTuple> entries = node.getValue();
    for (NodeTuple entry : entries) {
      Node keyNode = entry.getKeyNode();

      // Not a scalar key
      if (!(keyNode instanceof ScalarNode))
        continue;

      // Key mismatch
      if (!((ScalarNode) keyNode).getValue().equalsIgnoreCase(key))
        continue;

      // Remember this call's yielded entry
      nodeCache.put(key, entry);
      return entry;
    }

    // Also remember failed lookups
    nodeCache.put(key, null);
    return null;
  }

  /**
   * Unwraps any given node by unwrapping scalar values first, then - if applicable - collecting them
   * into maps or lists, as by the node's tag. Null tags will result in null values.
   * @param node Node to unwrap
   * @return Unwrapped node as a Java value
   */
  private @Nullable Object unwrapNode(Node node) {
    if (node instanceof ScalarNode)
      return unwrapScalarNode((ScalarNode) node);

    if (node instanceof SequenceNode) {
      List<Object> values = new ArrayList<>();

      for (Node item : ((SequenceNode) node).getValue())
        values.add(unwrapNode(item));

      return values;
    }

    if (node instanceof MappingNode) {
      Map<Object, Object> values = new HashMap<>();

      for (NodeTuple item : ((MappingNode) node).getValue())
        values.put(unwrapNode(item.getKeyNode()), unwrapNode(item.getValueNode()));

      return values;
    }

    throw new IllegalStateException("Encountered unknown node type >" + node.getType().getName() + "<");
  }

  /**
   * Tries to wrap an object in a yaml tree node, throws an {@link IllegalArgumentException}
   * for value types which cannot be depicted as a yaml value directly
   * @param value Value to wrap
   * @return Wrapped value
   */
  private Node wrapValue(@Nullable Object value) {
    Node node = wrapScalarNode(value);

    if (node != null)
      return node;

    if (value instanceof List) {
      List<Node> values = new ArrayList<>();
      node = new SequenceNode(Tag.SEQ, true, values, null, null, DumperOptions.FlowStyle.AUTO);

      for (Object item : (List<?>) value)
        values.add(wrapValue(item));

      return node;
    }

    if (value instanceof Map) {
      List<NodeTuple> tuples = new ArrayList<>();
      node = new MappingNode(Tag.MAP, true, tuples, null, null, DumperOptions.FlowStyle.AUTO);

      for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
        Node keyNode = createScalarNode(String.valueOf(entry.getKey()), Tag.STR);
        tuples.add(new NodeTuple(keyNode, wrapValue(entry.getValue())));
      }

      return node;
    }

    throw new IllegalArgumentException("Cannot store a value of type " + value.getClass());
  }

  /**
   * Unwraps a {@link ScalarNode} to a java type
   * @param node Node to unwrap
   * @return Unwrapped java value
   */
  private @Nullable Object unwrapScalarNode(ScalarNode node) {
    Tag tag = node.getTag();

    if (tag == Tag.NULL)
      return null;

    if (tag == Tag.STR)
      return node.getValue();

    if (tag == Tag.BOOL)
      return node.getValue().equalsIgnoreCase("true");

    if (tag == Tag.INT)
      return Long.parseLong(node.getValue());

    if (tag == Tag.FLOAT)
      return Float.parseFloat(node.getValue());

    throw new IllegalStateException("Encountered unknown scalar node type >" + tag + "<");
  }

  /**
   * Wraps a Java object into a suited containing {@link ScalarNode}, if possible
   * @param value Value to wrap
   * @return Wrapped value or null if this isn't a scalar value
   */
  private @Nullable Node wrapScalarNode(@Nullable Object value) {
    String stringValue = String.valueOf(value);

    if (value == null)
      return createScalarNode(stringValue, Tag.NULL);

    if (value instanceof Boolean)
      return createScalarNode(stringValue, Tag.BOOL);

    if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte)
      return createScalarNode(stringValue, Tag.INT);

    if (value instanceof Double || value instanceof Float)
      return createScalarNode(stringValue, Tag.FLOAT);

    if (value instanceof String)
      return createScalarNode(stringValue, Tag.STR);

    // Unknown scalar type
    return null;
  }

  /**
   * Create a new, parameterized scalar node
   * @param value String value (node content)
   * @param tag Type tag
   * @return Created scalar node
   */
  private ScalarNode createScalarNode(String value, Tag tag) {
    return new ScalarNode(tag, value, null, null, DumperOptions.ScalarStyle.PLAIN);
  }
}
