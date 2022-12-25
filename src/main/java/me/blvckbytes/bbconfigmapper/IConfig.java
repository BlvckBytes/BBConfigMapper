package me.blvckbytes.bbconfigmapper;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IConfig {

  /**
   * Get a value by it's path
   * @param path Path to identify the value
   */
  @Nullable Object get(@Nullable String path);

  /**
   * Set a value by it's path
   * @param path Path to identify the value
   */
  void set(@Nullable String path, @Nullable Object value);

  /**
   * Remove a key and all of it's children by it's path
   * @param path Path to identify the key
   */
  void remove(@Nullable String path);

  /**
   * Check whether a given path exists within the configuration file
   * @param path Path to identify the value
   */
  boolean exists(@Nullable String path);

  /**
   * Attach a comment to a specific path
   * @param path Path to attach to
   * @param lines Lines of text in the comment
   * @param self Whether to attach to the key itself or to it's value
   */
  void attachComment(@Nullable String path, List<String> lines, boolean self);

  /**
   * Read a specific path's attached comment, if available
   * @param path Path to read from
   * @param self Whether to read from the key itself or from it's value
   * @return A list of the comment's lines, null if the path doesn't exist
   *         or there's no comment attached to it yet
   */
  @Nullable List<String> readComment(@Nullable String path, boolean self);

}
