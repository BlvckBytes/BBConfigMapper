package me.blvckbytes.bbconfigmapper;

import org.jetbrains.annotations.Nullable;

public interface IValueConverterRegistry {

  @Nullable FValueConverter getConverterFor(Class<?> type);

}
