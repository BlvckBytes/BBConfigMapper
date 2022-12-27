package me.blvckbytes.bbconfigmapper.sections;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

@Getter
@AllArgsConstructor
public class NoDefaultConstructorSection implements IConfigSection {

  private String a, b, c;

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
