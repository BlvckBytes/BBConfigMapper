package me.blvckbytes.bbconfigmapper;

import me.blvckbytes.gpeee.GPEEE;
import me.blvckbytes.gpeee.IExpressionEvaluator;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;

import java.io.FileReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

  public static void main(String[] args) throws Exception {
    FileReader fr = new FileReader("/Users/blvckbytes/Desktop/test.yml");

    YamlConfig cfg = new YamlConfig();
    cfg.load(fr);

    cfg.attachComment("my_keys.hello", List.of(
      " Comment line one",
      " Comment line two"
    ), true);

    cfg.set("my_keys.another", 55);
    cfg.set("my_keys.hello", "goodbye");

    long nanosStart = System.nanoTime();
    cfg.set("my_keys.a.b.c.d", .12);
    long nanosEnd = System.nanoTime();

    System.out.println((nanosEnd - nanosStart) / 1000);

    nanosStart = System.nanoTime();
    for (int i = 0; i < 10; i++)
      cfg.get("my_keys.a.b.c.d");
    nanosEnd = System.nanoTime();

    System.out.println((nanosEnd - nanosStart) / 1000);

    cfg.remove("just_a_key");

    nanosStart = System.nanoTime();
    cfg.remove("my_keys.a.b");
    nanosEnd = System.nanoTime();

    System.out.println((nanosEnd - nanosStart) / 1000);

    Map<Object, Object> myTest = new HashMap<>();
    myTest.put("a", 5);
    myTest.put("b", .2);
    myTest.put("c", "hello");

    Map<Object, Object> mapD = new HashMap<>();
    myTest.put("d", mapD);

    mapD.put("e", 22);
    mapD.put("f", 21);
    mapD.put("g", List.of(19, 18, .22, "Hello"));

    nanosStart = System.nanoTime();
    cfg.set("my_test", myTest);
    nanosEnd = System.nanoTime();

    System.out.println((nanosEnd - nanosStart) / 1000);

    StringWriter sw = new StringWriter();
    cfg.save(sw);
    System.out.println(sw);

    // So far: not too shabby if I may say so myself

    IExpressionEvaluator evaluator = new GPEEE(null);
    IEvaluationEnvironment environment = GPEEE.EMPTY_ENVIRONMENT;

    ConfigReader reader = new ConfigReader(cfg, evaluator);

    IEvaluable value = reader.get("expr_test.lore");

    if (value != null) {
      System.out.println(String.join("|", value.asStringList(environment)));
      return;
    }

    System.err.println("Could not find the target key");
  }
}
