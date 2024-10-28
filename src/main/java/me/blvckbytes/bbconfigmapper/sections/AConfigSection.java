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

package me.blvckbytes.bbconfigmapper.sections;

import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class AConfigSection {

  private static class DefaultSupplier {
    private final Supplier<Object> supplier;
    private final Set<String> fieldExceptions;

    public DefaultSupplier(Supplier<Object> supplier, Set<String> fieldExceptions) {
      this.supplier = supplier;
      this.fieldExceptions = fieldExceptions;
    }
  }

  private final EvaluationEnvironmentBuilder baseEnvironment;
  public final IEvaluationEnvironment builtBaseEnvironment;
  private final Map<Class<?>, DefaultSupplier> fieldDefaultSuppliers;

  public AConfigSection(EvaluationEnvironmentBuilder baseEnvironment) {
    this.fieldDefaultSuppliers = new HashMap<>();
    this.baseEnvironment = baseEnvironment;
    this.builtBaseEnvironment = baseEnvironment.build();
  }

  public EvaluationEnvironmentBuilder getBaseEnvironment() {
    return baseEnvironment.duplicate();
  }

  /**
   * Called to decide the type of Object fields at runtime,
   * based on previously parsed values of that instance, as
   * it's patched one field at a time. Decidable fields are
   * always read last, so that they have access to other,
   * known type fields in order to decide properly.
   * @param field Target field in question
   * @return Decided type, Object.class means skip
   */
  public @Nullable Class<?> runtimeDecide(String field) {
    return null;
  }

  /**
   * Called when a field wasn't found within the config and a default could be set
   * @param field Target field
   * @return Value to use as a default
   */
  public @Nullable Object defaultFor(Field field) {
    return null;
  }

  /**
   * Called when parsing of the section is completed
   * and no more changes will be applied
   */
  public void afterParsing(List<Field> fields) throws Exception {
    for (Field field : fields) {
      if (field.get(this) != null)
        continue;

      DefaultSupplier defaultSupplier = fieldDefaultSuppliers.get(field.getType());
      CSNamed nameAnnotation = field.getAnnotation(CSNamed.class);

      if (
        defaultSupplier == null ||
        defaultSupplier.fieldExceptions.contains(field.getName()) ||
        (nameAnnotation != null && defaultSupplier.fieldExceptions.contains(nameAnnotation.name()))
      )
        continue;

      field.set(this, defaultSupplier.supplier.get());
    }
  }

  protected void registerFieldDefault(Class<?> type, Supplier<Object> supplier, String... fieldExceptions) {
    fieldDefaultSuppliers.put(type, new DefaultSupplier(supplier, Arrays.stream(fieldExceptions).collect(Collectors.toSet())));
  }
}
