package me.blvckbytes.bbconfigmapper.sections;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

@Getter
public class QuestSection implements IConfigSection {

  private String type;

  @CSInlined
  private Object parameter;

  @Override
  public Class<?> runtimeDecide(String field) {
    if (field.equals("parameter")) {
      switch (type) {
        case "block-break":
          return BlockBreakQuestParameterSection.class;
        case "entity-kill":
          return EntityKillQuestParameterSection.class;
      }
    }

    return null;
  }

  @Override
  public @Nullable Object defaultFor(Class<?> type, String field) {
    return null;
  }

  @Override
  public void afterParsing(List<Field> fields) {}
}
