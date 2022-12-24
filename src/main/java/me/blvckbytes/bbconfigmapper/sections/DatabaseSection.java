package me.blvckbytes.bbconfigmapper.sections;

import lombok.Getter;
import me.blvckbytes.bbconfigmapper.IEvaluable;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
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

  @CSInlined
  private InlinedSection inlinedSection;

  @CSMap(k=IEvaluable.class, v=TestSection.class)
  private Map<IEvaluable, TestSection> myMap;

  @CSList(type = TestSection.class)
  private List<TestSection> myList;

  @CSList(type = IEvaluable.class)
  private List<IEvaluable> myScalars;

  @Override
  public Class<?> runtimeDecide(String field) {
    return null;
  }

  @Override
  public @Nullable Object defaultFor(Class<?> type, String field) {
    if (field.equals("myMap"))
      return new HashMap<>();

    if (field.equals("myList") || field.equals("myScalars"))
      return new ArrayList<>();

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
      "\nmyList=" + myList +
      "\nmyScalars=" + myScalars +
      "\ninlinedSection=" + inlinedSection +
      "\n}";
  }
}
