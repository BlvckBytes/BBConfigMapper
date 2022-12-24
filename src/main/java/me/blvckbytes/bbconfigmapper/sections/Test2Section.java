package me.blvckbytes.bbconfigmapper.sections;

import lombok.Getter;
import me.blvckbytes.bbconfigmapper.IEvaluable;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

@Getter
public class Test2Section implements IConfigSection {

  private IEvaluable name;

  @Override
  public Class<?> runtimeDecide(String field) {
    return null;
  }

  @Override
  public @Nullable Object defaultFor(Class<?> type, String field) {
    if (field.equals("name"))
      return "idk";
    return null;
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {}

  @Override
  public String toString() {
    return "Test2Section{" +
      "name=" + name +
      '}';
  }
}
