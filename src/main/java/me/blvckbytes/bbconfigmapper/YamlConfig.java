package me.blvckbytes.bbconfigmapper;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class YamlConfig implements IConfig {

  private final YamlConfiguration config;

  public YamlConfig(File file) {
    this.config = YamlConfiguration.loadConfiguration(file);
  }

  @Override
  public @Nullable Object get(String key) {
    return config.get(key);
  }

  @Override
  public void set(String key, Object value) {
    config.set(key, value);
  }

  @Override
  public boolean exists(String key) {
    return config.isSet(key);
  }

  public boolean save(File file) throws IOException {
    if (this.config == null)
      return false;

    this.config.save(file);
    return true;
  }
}
