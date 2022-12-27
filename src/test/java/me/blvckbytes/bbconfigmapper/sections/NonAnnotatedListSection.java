package me.blvckbytes.bbconfigmapper.sections;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

@Getter
public class NonAnnotatedListSection implements IConfigSection {

  private List<String> c;

  @Override
  public Class<?> runtimeDecide(String field) {
    return null;
  }

  @Override
  public @Nullable Object defaultFor(Class<?> type, String field) {
    return null;
  }

  @Override
  public void afterParsing(List<Field> fields) {}
}
