package me.blvckbytes.bbconfigmapper;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IConfig {

  /**
   * Get a value by it's key
   * @param key Key to identify the value
   */
  @Nullable Object get(String key);

  /**
   * Set a value by it's key
   * @param key Key to identify the value
   */
  void set(String key, @Nullable Object value);

  /**
   * Check whether a value exists, identified by it's key
   * @param key Key to identify the value
   */
  boolean exists(String key);

  /**
   * Attach a comment to a specific key
   * @param key Key to attach to
   * @param lines Lines of text in the comment
   * @return True on success, false if that key didn't yet exist
   */
  boolean attachComment(String key, List<String> lines);

  /**
   * Read a specific key's attached comment, if available
   * @param key Key to read from
   * @return A list of the comment's lines, null if the key doesn't exist
   *         or there's no comment attached to it yet
   */
  @Nullable List<String> readComment(String key);

}
