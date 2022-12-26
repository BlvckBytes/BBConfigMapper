package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.bbconfigmapper.sections.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigMapperTests {

  private final TestHelper helper = new TestHelper();

  @Test
  public void shouldMapSingleSectionFromRootWithNatives() throws Exception {
    IConfigMapper mapper = helper.makeMapper("database_section.yml");
    DatabaseSectionStrings section = mapper.mapSection(null, DatabaseSectionStrings.class);

    assertEquals("localhost", section.getHost());
    assertEquals("3306", section.getPort());
    assertEquals("config_mapper", section.getDatabase());
    assertEquals("root", section.getUsername());
    assertEquals("abc123", section.getPassword());
  }

  @Test
  public void shouldMapSingleSectionFromRootWithEvaluables() throws Exception {
    IConfigMapper mapper = helper.makeMapper("database_section.yml");
    DatabaseSectionEvaluables section = mapper.mapSection(null, DatabaseSectionEvaluables.class);

    assertEquals("localhost", section.getHost().asScalar(ScalarType.STRING, helper.getEnv()));
    assertEquals("3306", section.getPort().asScalar(ScalarType.STRING, helper.getEnv()));
    assertEquals("config_mapper", section.getDatabase().asScalar(ScalarType.STRING, helper.getEnv()));
    assertEquals("root", section.getUsername().asScalar(ScalarType.STRING, helper.getEnv()));
    assertEquals("abc123", section.getPassword().asScalar(ScalarType.STRING, helper.getEnv()));
  }

  @Test
  public void shouldMapSingleSectionFromPath() throws Exception {
    IConfigMapper mapper = helper.makeMapper("database_section.yml");
    DatabaseSectionStrings section = mapper.mapSection("connection", DatabaseSectionStrings.class);

    assertEquals("localhost", section.getHost());
    assertEquals("3306", section.getPort());
    assertEquals("config_mapper", section.getDatabase());
    assertEquals("root", section.getUsername());
    assertEquals("abc123", section.getPassword());
  }

  @Test
  public void shouldMapNestedSection() throws Exception {
    IConfigMapper mapper = helper.makeMapper("potion_simple_section.yml");
    PotionSimpleSection section = mapper.mapSection(null, PotionSimpleSection.class);

    assertEquals("throwable", section.getType());
    assertEquals("damage", section.getMainEffect().getEffect());
    assertEquals("120", section.getMainEffect().getDuration());
    assertEquals("2", section.getMainEffect().getAmplifier());
  }

  @Test
  public void shouldMapSectionWithMap() throws Exception {
    IConfigMapper mapper = helper.makeMapper("ui_layout_section.yml");
    UiLayoutSection section = mapper.mapSection(null, UiLayoutSection.class);

    assertEquals("workbench", section.getUiName());
    assertEquals(25L, section.getLayout().get("output").<Long>asScalar(ScalarType.LONG, helper.getEnv()));
    assertEquals(16L, section.getLayout().get("previous").<Long>asScalar(ScalarType.LONG, helper.getEnv()));
    assertEquals(34L, section.getLayout().get("next").<Long>asScalar(ScalarType.LONG, helper.getEnv()));
    assertEquals(24L, section.getLayout().get("indicator").<Long>asScalar(ScalarType.LONG, helper.getEnv()));
  }

  @Test
  public void shouldMapSectionWithList() throws Exception {
    IConfigMapper mapper = helper.makeMapper("potion_list_section.yml");
    PotionListSection section = mapper.mapSection(null, PotionListSection.class);

    assertEquals("throwable", section.getType());
    assertEquals("damage", section.getEffects().get(0).getEffect());
    assertEquals("120", section.getEffects().get(0).getDuration());
    assertEquals("2", section.getEffects().get(0).getAmplifier());
    assertEquals("healing", section.getEffects().get(1).getEffect());
    assertEquals("0", section.getEffects().get(1).getDuration());
    assertEquals("1", section.getEffects().get(1).getAmplifier());
    assertEquals("regeneration", section.getEffects().get(2).getEffect());
    assertEquals("10", section.getEffects().get(2).getDuration());
    assertEquals("3", section.getEffects().get(2).getAmplifier());
  }

  @Test
  public void shouldMapSectionWithRuntimeDecide() throws Exception {
    IConfigMapper mapper = helper.makeMapper("quest_block_break.yml");
    QuestSection section = mapper.mapSection(null, QuestSection.class);

    assertEquals("block-break", section.getType());
    BlockBreakQuestParameterSection blockBreakParameter = (BlockBreakQuestParameterSection) section.getParameter();
    assertEquals("STONE", blockBreakParameter.getMaterial());
    assertEquals("world", blockBreakParameter.getWorld());

    mapper = helper.makeMapper("quest_entity_kill.yml");
    section = mapper.mapSection(null, QuestSection.class);

    assertEquals("entity-kill", section.getType());
    EntityKillQuestParameterSection entityKillParameter = (EntityKillQuestParameterSection) section.getParameter();
    assertEquals("ZOMBIE", entityKillParameter.getEntityType());
    assertEquals("my zombie", entityKillParameter.getEntityName());
  }

  @Test
  public void shouldMapSectionWithDefaultValue() throws Exception {
    IConfigMapper mapper = helper.makeMapper("database_section_partial.yml");
    DatabaseSectionStrings section = mapper.mapSection("connection", DatabaseSectionStrings.class);

    assertEquals("host_default", section.getHost());
    assertEquals("port_default", section.getPort());
    assertEquals("config_mapper", section.getDatabase());
    assertEquals("root", section.getUsername());
    assertEquals("abc123", section.getPassword());
  }
}
