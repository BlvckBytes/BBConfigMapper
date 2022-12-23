package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.gpeee.GPEEE;
import me.blvckbytes.gpeee.IExpressionEvaluator;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;

public class Main {

  public static void main(String[] args) {
    IExpressionEvaluator evaluator = new GPEEE(null);
    IEvaluationEnvironment environment = GPEEE.EMPTY_ENVIRONMENT;

    FakeConfig config = new FakeConfig();
    ConfigReader reader = new ConfigReader(config, evaluator);

    IEvaluable value = reader.get("lore");

    if (value != null) {
      System.out.println(String.join("|", value.asStringList(environment)));
      return;
    }

    System.err.println("Could not find the target key");
  }
}
