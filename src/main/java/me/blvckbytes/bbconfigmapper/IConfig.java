package me.blvckbytes.bbconfigmapper;

import org.jetbrains.annotations.Nullable;

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

}
