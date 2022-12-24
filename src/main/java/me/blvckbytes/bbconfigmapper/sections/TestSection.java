package me.blvckbytes.bbconfigmapper.sections;

import lombok.Getter;
import me.blvckbytes.bbconfigmapper.IEvaluable;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@Getter
public class TestSection implements IConfigSection {

  private IEvaluable a;
  private IEvaluable b;
  private Test2Section c;

  @Override
  public Class<?> runtimeDecide(String field) {
    return null;
  }

  @Override
  public @Nullable Object defaultFor(Class<?> type, String field) {
    return null;
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {}

  @Override
  public String toString() {
    return "TestSection{" +
      "a=" + a +
      ", b=" + b +
      ", c=" + c +
      '}';
  }
}
