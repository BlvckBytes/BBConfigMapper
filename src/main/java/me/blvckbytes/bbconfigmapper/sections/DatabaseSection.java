package me.blvckbytes.bbconfigmapper.sections;

import lombok.Getter;
import me.blvckbytes.bbconfigmapper.IEvaluable;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

@Getter
public class DatabaseSection implements IConfigSection {

  private IEvaluable username;
  private IEvaluable password;
  private IEvaluable host;
  private IEvaluable port;
  private IEvaluable database;

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
    return "DatabaseSection{" +
      "username=" + username +
      ", password=" + password +
      ", host=" + host +
      ", port=" + port +
      ", database=" + database +
      '}';
  }
}
