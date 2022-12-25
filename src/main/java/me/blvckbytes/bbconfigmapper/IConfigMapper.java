package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.bbconfigmapper.sections.IConfigSection;
import org.jetbrains.annotations.Nullable;

public interface IConfigMapper {

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
