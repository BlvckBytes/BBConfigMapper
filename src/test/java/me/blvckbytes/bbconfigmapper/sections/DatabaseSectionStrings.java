package me.blvckbytes.bbconfigmapper.sections;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

@Getter
public class DatabaseSectionStrings implements IConfigSection {

  private String host, port, database, username, password;

  @Override
  public Class<?> runtimeDecide(String field) {
    return null;
  }

  @Override
  public @Nullable Object defaultFor(Class<?> type, String field) {
    switch (field) {
      case "host":
        return "host_default";
      case "port":
        return "port_default";
    }
    return null;
  }

  @Override
  public void afterParsing(List<Field> fields) {}
}
