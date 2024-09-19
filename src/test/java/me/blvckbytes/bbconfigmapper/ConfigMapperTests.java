/*
 * MIT License
 *
 * Copyright (c) 2023 BlvckBytes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.bbconfigmapper.sections.*;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
  public void shouldMapNestedSectionValuesToNullIfNotAMapping() throws Exception {
    IConfigMapper mapper = helper.makeMapper("potion_simple_section_no_mapping.yml");
    PotionSimpleSection section = mapper.mapSection(null, PotionSimpleSection.class);

    assertEquals("throwable", section.getType());
    assertNull(section.getMainEffect().getEffect());
    assertNull(section.getMainEffect().getDuration());
    assertNull(section.getMainEffect().getAmplifier());
  }

  @Test
  public void shouldMapNestedSectionAlwaysValuesToNullIfAbsent() throws Exception {
    IConfigMapper mapper = helper.makeMapper("potion_simple_section_absent.yml");
    PotionSimpleSectionAlways section = mapper.mapSection(null, PotionSimpleSectionAlways.class);

    assertEquals("throwable", section.getType());
    assertNull(section.getMainEffect().getEffect());
    assertNull(section.getMainEffect().getDuration());
    assertNull(section.getMainEffect().getAmplifier());
  }

  @Test
  public void shouldMapNestedSectionToNullIfAbsent() throws Exception {
    IConfigMapper mapper = helper.makeMapper("potion_simple_section_absent.yml");
    PotionSimpleSection section = mapper.mapSection(null, PotionSimpleSection.class);

    assertEquals("throwable", section.getType());
    assertNull(section.getMainEffect());
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
  public void shouldMapSectionWithMapToEmptyIfYamlTypeMismatches() throws Exception {
    IConfigMapper mapper = helper.makeMapper("ui_layout_section_malformed.yml");
    UiLayoutSection section = mapper.mapSection(null, UiLayoutSection.class);

    assertNotNull(section.getLayout());
    assertEquals(0, section.getLayout().size());
  }

  @Test
  public void shouldMapAlwaysSectionWithMapToEmptyIfKeyIsAbsent() throws Exception {
    IConfigMapper mapper = helper.makeMapper("ui_layout_section_absent.yml");
    UiLayoutSectionAlways section = mapper.mapSection(null, UiLayoutSectionAlways.class);

    assertNotNull(section.getLayout());
    assertEquals(0, section.getLayout().size());
  }

  @Test
  public void shouldMapSectionWithMapToNullIfKeyIsAbsent() throws Exception {
    IConfigMapper mapper = helper.makeMapper("ui_layout_section_absent.yml");
    UiLayoutSection section = mapper.mapSection(null, UiLayoutSection.class);

    assertNull(section.getLayout());
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
  public void shouldMapSectionWithListToEmptyIfYamlTypeMismatches() throws Exception {
    IConfigMapper mapper = helper.makeMapper("potion_list_section_malformed.yml");
    PotionListSection section = mapper.mapSection(null, PotionListSection.class);

    assertNotNull(section.getEffects());
    assertEquals(0, section.getEffects().size());
  }

  @Test
  public void shouldMapAlwaysSectionWithListToEmptyIfKeyIsAbsent() throws Exception {
    IConfigMapper mapper = helper.makeMapper("potion_list_section_absent.yml");
    PotionListSectionAlways section = mapper.mapSection(null, PotionListSectionAlways.class);

    assertNotNull(section.getEffects());
    assertEquals(0, section.getEffects().size());
  }

  @Test
  public void shouldMapSectionWithListToNullIfKeyIsAbsent() throws Exception {
    IConfigMapper mapper = helper.makeMapper("potion_list_section_absent.yml");
    PotionListSection section = mapper.mapSection(null, PotionListSection.class);

    assertNull(section.getEffects());
  }

  @Test
  public void shouldMapSectionWithArray() throws Exception {
    IConfigMapper mapper = helper.makeMapper("potion_list_section.yml");
    PotionArraySection section = mapper.mapSection(null, PotionArraySection.class);

    assertEquals("throwable", section.getType());
    assertEquals("damage", section.getEffects()[0].getEffect());
    assertEquals("120", section.getEffects()[0].getDuration());
    assertEquals("2", section.getEffects()[0].getAmplifier());
    assertEquals("healing", section.getEffects()[1].getEffect());
    assertEquals("0", section.getEffects()[1].getDuration());
    assertEquals("1", section.getEffects()[1].getAmplifier());
    assertEquals("regeneration", section.getEffects()[2].getEffect());
    assertEquals("10", section.getEffects()[2].getDuration());
    assertEquals("3", section.getEffects()[2].getAmplifier());
  }

  @Test
  public void shouldMapSectionWithArrayToEmptyIfYamlTypeMismatches() throws Exception {
    IConfigMapper mapper = helper.makeMapper("potion_list_section_malformed.yml");
    PotionArraySection section = mapper.mapSection(null, PotionArraySection.class);

    assertNotNull(section.getEffects());
    assertEquals(0, section.getEffects().length);
  }

  @Test
  public void shouldMapAlwaysSectionWithArrayToEmptyIfKeyIsAbsent() throws Exception {
    IConfigMapper mapper = helper.makeMapper("potion_list_section_absent.yml");
    PotionArraySectionAlways section = mapper.mapSection(null, PotionArraySectionAlways.class);

    assertNotNull(section.getEffects());
    assertEquals(0, section.getEffects().length);
  }

  @Test
  public void shouldMapSectionWithArrayToNullIfKeyIsAbsent() throws Exception {
    IConfigMapper mapper = helper.makeMapper("potion_list_section_absent.yml");
    PotionArraySection section = mapper.mapSection(null, PotionArraySection.class);

    assertNull(section.getEffects());
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

  @Test
  public void shouldMapSectionAndLeaveNonDefaultNonExistingAsNull() throws Exception {
    IConfigMapper mapper = helper.makeMapper("database_section_partial2.yml");
    DatabaseSectionStrings section = mapper.mapSection("connection", DatabaseSectionStrings.class);

    assertEquals("host_default", section.getHost());
    assertEquals("port_default", section.getPort());
    assertNull(section.getDatabase());
    assertEquals("root", section.getUsername());
    assertEquals("abc123", section.getPassword());
  }

  @Test
  public void shouldThrowOnRuntimeDecideReturningNull() throws Exception {
    IConfigMapper mapper = helper.makeMapper("quest_invalid.yml");
    helper.assertThrowsWithMsg(IllegalStateException.class, () -> mapper.mapSection(null, QuestSection.class), "Requesting plain objects is disallowed");
  }

  @Test
  public void shouldThrowWhenSectionSelfReferences() throws Exception {
    IConfigMapper mapper = helper.makeMapper("quest_invalid.yml");
    helper.assertThrowsWithMsg(IllegalStateException.class, () -> mapper.mapSection(null, SelfRefSection.class), "Sections cannot use self-referencing fields");
  }

  @Test
  public void shouldThrowWhenNoStandardConstructorAvailable() throws Exception {
    IConfigMapper mapper = helper.makeMapper("mappings.yml");
    helper.assertThrowsWithMsg(IllegalStateException.class, () -> mapper.mapSection(null, NoStandardConstructorSection.class), "Please specify a standard-constructor taking an EvaluationEnvironmentBuilder on ");
  }

  @Test
  public void shouldIgnoreAnnotatedAndStaticFields() throws Exception {
    IConfigMapper mapper = helper.makeMapper("ignore_section.yml");
    IgnoreSection section = mapper.mapSection(null, IgnoreSection.class);

    assertNull(section.getIgnored1());
    assertEquals("missing CSIgnore annotation", section.getIgnored2());
    assertNull(section.getIgnored3());
    assertNull(IgnoreSection.getIgnored4());
  }

  @Test
  public void shouldMapEnumValues() throws Exception {
    IConfigMapper mapper = helper.makeMapper("enum_section.yml");
    EnumSection section = mapper.mapSection(null, EnumSection.class);

    assertEquals(ECustomEnum.HELLO, section.getCustomEnumA());
    assertEquals(ECustomEnum.WORLD, section.getCustomEnumB());
    assertEquals(ECustomEnum.ENUM, section.getCustomEnumC());
    assertNull(section.getCustomEnumInvalid());
  }

  @Test
  public void shouldThrowOnInvalidEnumValues() throws Exception {
    IConfigMapper mapper = helper.makeMapper("enum_section_with_invalid.yml");
    helper.assertThrowsWithMsg(IllegalStateException.class, () -> mapper.mapSection(null, EnumSection.class), "Value \"INVALID\" was not one of HELLO, WORLD, ENUM (at path 'customEnumInvalid')");
  }

  @Test
  public void shouldMapCustomObject() throws Exception {
    IConfigMapper mapper = helper.makeMapper("custom_object.yml", getCustomObjectConverterRegistry());
    CustomObjectSection section = mapper.mapSection(null, CustomObjectSection.class);

    assertEquals("Hello, world", section.getCustomObject().value);
  }

  private IValueConverterRegistry getCustomObjectConverterRegistry() {
    return new IValueConverterRegistry() {

      @Override
      public @Nullable Class<?> getRequiredTypeFor(Class<?> type) {
        if (type == CustomObject.class)
          return String.class;
        return null;
      }

      @Override
      public @Nullable FValueConverter getConverterFor(Class<?> type) {
        if (type == CustomObject.class)
          return (value, evaluator) -> new CustomObject(((String) value));
        return null;
      }
    };
  }
}
