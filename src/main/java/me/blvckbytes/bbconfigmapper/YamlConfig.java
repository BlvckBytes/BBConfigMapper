package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.bbconfigmapper.logging.DebugLogSource;
import me.blvckbytes.gpeee.IExpressionEvaluator;
import me.blvckbytes.gpeee.Tuple;
import me.blvckbytes.gpeee.logging.ILogger;
import org.jetbrains.annotations.NotNull;
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

  /*
    TODO: Add more debug logging calls to capture all details
   */

  private static final Yaml YAML;
  private static final DumperOptions DUMPER_OPTIONS;

  private final IExpressionEvaluator evaluator;
  private final ILogger logger;
  private final String expressionMarkerSuffix;
  private final Map<MappingNode, Map<String, @Nullable NodeTuple>> locateKeyCache;
  private MappingNode rootNode;

  static {
    LoaderOptions loaderOptions = new LoaderOptions();
    loaderOptions.setProcessComments(true);
    loaderOptions.setAllowDuplicateKeys(false);

    DUMPER_OPTIONS = new DumperOptions();
    DUMPER_OPTIONS.setProcessComments(true);
    DUMPER_OPTIONS.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

    YAML = new Yaml(new Constructor(loaderOptions), new Representer(DUMPER_OPTIONS), DUMPER_OPTIONS, loaderOptions);
  }

  public YamlConfig(IExpressionEvaluator evaluator, ILogger logger, String expressionMarkerSuffix) {
    this.evaluator = evaluator;
    this.logger = logger;
    this.expressionMarkerSuffix = expressionMarkerSuffix;
    this.locateKeyCache = new HashMap<>();
  }

  public void load(Reader reader) {
    Iterator<Node> nodes = YAML.composeAll(reader).iterator();

    if (!nodes.hasNext())
      throw new IllegalStateException("No node available");

    Node root = nodes.next();

    if (nodes.hasNext())
      throw new IllegalStateException("Encountered multiple nodes");

    if (!(root instanceof MappingNode))
      throw new IllegalStateException("The top level of a config has to be a map.");

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.YAML, "Successfully loaded the YAML root node using the provided reader");
    //#endif

    // Swap out root node and reset key cache
    this.rootNode = (MappingNode) root;
    this.locateKeyCache.clear();
  }

  public void save(Writer writer) throws IOException {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.YAML, "Serializing the YAML root node to the provided writer");
    //#endif

    if (this.rootNode == null || this.rootNode.getValue().size() == 0) {
      writer.write("");
      return;
    }

    YAML.serialize(this.rootNode, writer);
  }

  @Override
  public @Nullable Object get(@Nullable String path) {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.YAML, "Object at path=" + path + " has been requested");
    //#endif

    Tuple<@Nullable Node, Boolean> target = locateNode(path, false, false);
    Object value = target.getA() == null ? null : unwrapNode(target.getA(), target.getB());

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.YAML, "Returning content of path=" + path + " with value=" + value);
    //#endif

    return value;
  }

  @Override
  public void set(@Nullable String path, @Nullable Object value) {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.YAML, "An update of value=" + value + " at path=" + path + " has been requested");
    //#endif

    Node wrappedValue = wrapValue(value);

    if (path == null) {
      if (!(wrappedValue instanceof MappingNode))
        throw new IllegalArgumentException("Cannot exchange the root-node for a non-map node");

      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.YAML, "Swapped out the root node");
      //#endif

      rootNode = (MappingNode) wrappedValue;
      return;
    }

    updatePathValue(path, wrappedValue, true);
  }

  @Override
  public void remove(@Nullable String path) {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.YAML, "The removal of path=" + path + " has been requested");
    //#endif

    if (path == null) {
      //#if mvn.project.property.production != "true"
      logger.logDebug(DebugLogSource.YAML, "Reset the root node");
      //#endif

      rootNode = new MappingNode(Tag.MAP, true, new ArrayList<>(), null, null, DUMPER_OPTIONS.getDefaultFlowStyle());
      return;
    }

    updatePathValue(path, null, false);
  }

  @Override
  public boolean exists(@Nullable String path) {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.YAML, "An existence check of path=" + path + " has been requested");
    //#endif

    // For a key to exist, it's path has to exist within the
    // config, even if it points at a null value
    boolean exists = locateNode(path, true, false).getA() != null;

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.YAML, "Returning existence value for path=" + path + " of exists=" + exists);
    //#endif

    return exists;
  }

  @Override
  public void attachComment(@Nullable String path, List<String> lines, boolean self) {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.YAML, "Attaching a comment to path=" + path + " (self=" + self + ") of lines=" + lines + " has been requested");
    //#endif

    Node target = locateNode(path, self, false).getA();

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
  public @Nullable List<String> readComment(@Nullable String path, boolean self) {
    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.YAML, "Reading the comment at path=" + path + " (self=" + self + ") has been requested");
    //#endif

    Node target = locateNode(path, self, false).getA();

    if (target == null)
      return null;

    List<String> comments = new ArrayList<>();
    List<CommentLine> targetComments = target.getBlockComments();

    for (CommentLine comment : targetComments) {
      if (comment.getCommentType() == CommentType.BLANK_LINE) {
        comments.add("\n");
        continue;
      }

      comments.add(comment.getValue());
    }

    //#if mvn.project.property.production != "true"
    logger.logDebug(DebugLogSource.YAML, "Returning comments for path=" + path + " comments=" + comments);
    //#endif

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
      container = (MappingNode) locateNode(keyPath.substring(0, lastDotIndex), false, forceCreateMappings).getA();
    }

    if (container == null || keyPart.isBlank())
      throw new IllegalArgumentException("Invalid path specified: " + keyPath);

    // Check if there's an existing tuple
    NodeTuple existingTuple = locateKey(container, keyPart);
    Node existingKey = null;
    int existingIndex = -1;

    invalidateLocateKeyCacheFor(container, keyPart);

    // Remove an existing tuple from the map
    if (existingTuple != null) {
      existingKey = existingTuple.getKeyNode();
      existingIndex = container.getValue().indexOf(existingTuple);
      container.getValue().remove(existingIndex);

      // If the just removed tuple held a mapping node as it#s value, invalidate
      // all children mappings within that tuple recursively
      Node valueNode = existingTuple.getValueNode();
      if (valueNode instanceof MappingNode)
        forAllMappingsRecursively((MappingNode) valueNode, this::invalidateLocateKeyCacheFor);
    }

    // Create a new tuple for this value, if provided
    if (value != null) {
      NodeTuple newTuple = createNewTuple(existingKey, keyPart, value);

      // Preserve it's index within the list of tuples
      if (existingIndex >= 0)
        container.getValue().add(existingIndex, newTuple);
      else
        container.getValue().add(newTuple);
    }
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
      assert key != null;
      keyNode = new ScalarNode(Tag.STR, key, null, null, DumperOptions.ScalarStyle.PLAIN);
    }

    return new NodeTuple(keyNode, value);
  }

  /**
   * Locates a target node by it's identifying path
   * @param path Path to search for, null means root
   * @param self Whether to locate the containing key or the value (self means the key)
   * @return A tuple of the target node or null if the target node didn't exist
   *         as well as a boolean marking whether this path was marked for expressions
   */
  private @NotNull Tuple<@Nullable Node, Boolean> locateNode(@Nullable String path, boolean self, boolean forceCreateMappings) {
    if (path == null)
      return Tuple.of(rootNode, false);

    // Keys should never contain any whitespace
    path = path.trim();

    if (path.isBlank())
      throw new IllegalArgumentException("Invalid path specified: " + path);

    Node node = rootNode;
    boolean markedForExpressions = false;

    int endIndex = path.indexOf('.'), beginIndex = 0;

    while (true) {

      // No next dot available, go until the end of the path string
      if (endIndex < 0)
        endIndex = path.length();

      // Substring between the current begin (inclusive) and the dot position (exclusive)
      String pathPart = path.substring(beginIndex, endIndex);

      // Not a mapping node, cannot look up a path-part, the key has to be invalid
      if (!(node instanceof MappingNode))
        return Tuple.of(null, markedForExpressions);

      MappingNode mapping = (MappingNode) node;
      NodeTuple keyValueTuple = locateKey(mapping, pathPart);
      boolean markedAlready = pathPart.endsWith(expressionMarkerSuffix);

      // The k-v tuple could not be located and isn't marked for expressions already
      // Try to append the expression marker and check for a match again
      if (keyValueTuple == null && !markedAlready) {
        keyValueTuple = locateKey(mapping, pathPart + expressionMarkerSuffix);
        markedAlready = true;
      }

      // There was a tuple available and it carried the expression marker
      if (keyValueTuple != null && markedAlready)
        markedForExpressions = true;

      // Target tuple could not be located or is of wrong value type, create a
      // new tuple of value type mapping and set it within the tree
      if (forceCreateMappings && (keyValueTuple == null || !(keyValueTuple.getValueNode() instanceof MappingNode))) {
        // Try to reuse already present key nodes
        Node tupleKey = keyValueTuple == null ? null : keyValueTuple.getKeyNode();

        keyValueTuple = createNewTuple(tupleKey, pathPart, new MappingNode(Tag.MAP, true, new ArrayList<>(), null, null, DUMPER_OPTIONS.getDefaultFlowStyle()));
        mapping.getValue().add(keyValueTuple);

        // Invalidate the (null) cache for this newly added tuple
        invalidateLocateKeyCacheFor(mapping, pathPart);
      }

      // Current path-part does not exist
      if (keyValueTuple == null)
        return Tuple.of(null, markedForExpressions);

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

    return Tuple.of(node, markedForExpressions);
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
   * @param markedForExpressions Whether expressions should be parsed
   * @return Unwrapped node as a Java value
   */
  private @Nullable Object unwrapNode(Node node, boolean markedForExpressions) {
    if (node instanceof ScalarNode)
      return unwrapScalarNode((ScalarNode) node, markedForExpressions);

    if (node instanceof SequenceNode) {
      List<Object> values = new ArrayList<>();

      for (Node item : ((SequenceNode) node).getValue())
        values.add(unwrapNode(item, markedForExpressions));

      return values;
    }

    if (node instanceof MappingNode) {
      Map<Object, Object> values = new HashMap<>();

      for (NodeTuple item : ((MappingNode) node).getValue()) {
        boolean isItemMarkedForExpressions = markedForExpressions;

        // Expressions within keys are - of course - not supported
        Object key = unwrapNode(item.getKeyNode(), false);

        // If the key is a string, it might hold an attached marker which needs to be stripped off
        if (key instanceof String) {
          String keyS = (String) key;

          // Strip of trailing marker, also mark for expressions (if not marked already)
          if (keyS.endsWith(expressionMarkerSuffix)) {
            key = keyS.substring(0, keyS.length() - 1);
            isItemMarkedForExpressions = true;
          }
        }

        values.put(key, unwrapNode(item.getValueNode(), isItemMarkedForExpressions));
      }

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
      node = new SequenceNode(Tag.SEQ, true, values, null, null, DUMPER_OPTIONS.getDefaultFlowStyle());

      for (Object item : (List<?>) value)
        values.add(wrapValue(item));

      return node;
    }

    if (value instanceof Map) {
      List<NodeTuple> tuples = new ArrayList<>();
      node = new MappingNode(Tag.MAP, true, tuples, null, null, DUMPER_OPTIONS.getDefaultFlowStyle());

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
   * @param markedForExpressions Whether expressions should be parsed
   * @return Unwrapped java value
   */
  private @Nullable Object unwrapScalarNode(ScalarNode node, boolean markedForExpressions) {
    Tag tag = node.getTag();

    if (tag == Tag.NULL)
      return null;

    // If a node is marked for expression either itself or by a parent node, it
    // will be parsed as such, no matter it's tag, as it's a user-choice
    if (markedForExpressions)
      return evaluator.optimizeExpression(evaluator.parseString(node.getValue()));

    if (tag == Tag.STR)
      return node.getValue();

    if (tag == Tag.BOOL)
      return node.getValue().equalsIgnoreCase("true");

    if (tag == Tag.INT)
      return Long.parseLong(node.getValue());

    if (tag == Tag.FLOAT)
      return Double.parseDouble(node.getValue());

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
