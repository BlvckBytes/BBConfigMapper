package me.blvckbytes.bbconfigmapper.sections;

import lombok.Getter;
import me.blvckbytes.bbconfigmapper.IEvaluable;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class DatabaseSection implements IConfigSection {

  private IEvaluable username;
  private IEvaluable password;
  private IEvaluable host;
  private IEvaluable port;
  private IEvaluable database;

  @CSMap(k=IEvaluable.class, v=TestSection.class)
  private Map<IEvaluable, TestSection> myMap;

  @Override
  public Class<?> runtimeDecide(String field) {
    return null;
  }

  @Override
  public @Nullable Object defaultFor(Class<?> type, String field) {
    if (field.equals("myMap"))
      return new HashMap<>();
    return null;
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {}

  @Override
  public String toString() {
    return "DatabaseSection{" +
      "\nusername=" + username +
      "\npassword=" + password +
      "\nhost=" + host +
      "\nport=" + port +
      "\ndatabase=" + database +
      "\nmyMap=" + myMap +
      "\n}";
  }
}
