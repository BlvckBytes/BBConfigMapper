package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.gpeee.GPEEE;
import me.blvckbytes.gpeee.IExpressionEvaluator;
import me.blvckbytes.gpeee.logging.ILogger;
import me.blvckbytes.gpeee.logging.NullLogger;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class TestHelper {

  private final IExpressionEvaluator evaluator;
  private final String expressionMarkerSuffix;
  private final ILogger logger;

  public TestHelper() {
    this.expressionMarkerSuffix = "$";
    this.logger = new NullLogger();
    this.evaluator = new GPEEE(logger);
  }

  public YamlConfig makeConfig(String fileName) throws FileNotFoundException {
    YamlConfig config = new YamlConfig(this.evaluator, this.logger, this.expressionMarkerSuffix);
    config.load(new FileReader("src/test/resources/" + fileName));
    return config;
  }
}
