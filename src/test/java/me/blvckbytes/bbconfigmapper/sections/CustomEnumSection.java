package me.blvckbytes.bbconfigmapper.sections;

import lombok.Getter;

@Getter
public class CustomEnumSection implements IConfigSection {

  private ECustomEnum customEnumA;
  private ECustomEnum customEnumB;
  private ECustomEnum customEnumC;
  private ECustomEnum customEnumInvalid;

}
