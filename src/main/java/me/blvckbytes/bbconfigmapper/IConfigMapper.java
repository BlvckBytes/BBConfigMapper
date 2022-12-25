package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.bbconfigmapper.sections.IConfigSection;
import org.jetbrains.annotations.Nullable;

public interface IConfigMapper {

  /**
   * Get a wrapped value from the underlying {@link IConfig}
   * @param key Key to read from
   * @return Wrapped value, null if the key didn't exist
   */
  @Nullable IEvaluable get(String key);

  /**
   * Set a value within the config, identified by it's key. This
   * method should be preferred over {@link IConfig#set(String, Object)}, as
   * it also invalidates a possibly cached value within the reader
   * @param key Key to write to
   * @param value Value to write
   */
  void set(String key, @Nullable Object value);

  /**
   * Creates an empty instance of the provided type by invoking the default-constructor
   * and then traverses it's fields to assign them one after the other with available
   * configuration values while making use of the decision methods within {@link IConfigSection}
   * @param root Root node of this section (null means config root)
   * @param type Type of the class to map
   * @return Mapped instance of specified type
   */
  <T extends IConfigSection> T mapSection(@Nullable String root, Class<T> type) throws Exception;

}
