package me.blvckbytes.bbconfigmapper.sections;

import lombok.Getter;
import me.blvckbytes.bbconfigmapper.IEvaluable;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

@Getter
public class InlinedSection implements IConfigSection {

  private IEvaluable inlinedA;
  private IEvaluable inlinedB;

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
      "inlinedA=" + inlinedA +
      ", inlinedB=" + inlinedB +
      '}';
  }
}
