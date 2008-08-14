package com.google.gson;

import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Class that provides information relevant to different parts of a type.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
class TypeInfo {
  protected final Type actualType;
  protected final Class<?> rawClass;

  TypeInfo(Type actualType) {
    this.actualType = actualType;
    rawClass = TypeUtils.toRawClass(actualType);
  }

  public final Type getActualType() {
    return actualType;
  }

  /**
   * Returns the corresponding wrapper type of {@code type} if it is a primitive
   * type; otherwise returns {@code type} itself. Idempotent.
   * <pre>
   *     wrap(int.class) == Integer.class
   *     wrap(Integer.class) == Integer.class
   *     wrap(String.class) == String.class
   * </pre>
   */
  public final Class<?> getWrappedClass() {
    return Primitives.wrap(rawClass);
  }

  /**
   * @return the raw class associated with this type
   */
  public final Class<?> getRawClass() {
    return rawClass;
  }

  public final boolean isCollectionOrArray() {
    return Collection.class.isAssignableFrom(rawClass) || isArray();
  }

  public final boolean isArray() {
    return TypeUtils.isArray(rawClass);
  }

  public final boolean isEnum() {
    return rawClass.isEnum();
  }

  public final boolean isPrimitive() {
    return Primitives.isWrapperType(Primitives.wrap(rawClass));
  }

  public final boolean isString() {
    return rawClass == String.class;
  }

  public final boolean isPrimitiveOrStringAndNotAnArray() {
    return (isPrimitive() || isString()) && !isArray();
  }
}