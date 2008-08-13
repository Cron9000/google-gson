package com.google.gson;

import com.google.gson.TestTypes.BagOfPrimitives;
import com.google.gson.reflect.TypeToken;

import junit.framework.TestCase;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Small test for the serialization/deserialization support of parameterized types in Gson.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public class ParameterizedTypesTests  extends TestCase {

  private Gson gson;

  private static class MyParameterizedType<T> {
    final T value;
    public MyParameterizedType(T value) {
      this.value = value;
    }
    public T getValue() {
      return value;
    }

    String getExpectedJson() {
      String valueAsJson = getExpectedJson(value);
      return String.format("{\"value\":%s}", valueAsJson);
    }

    private String getExpectedJson(Object obj) {
      Class<?> clazz = obj.getClass();
      if (Primitives.isWrapperType(Primitives.wrap(clazz))) {
        return obj.toString();
      } else if (obj.getClass().equals(String.class)) {
        return "\"" + obj.toString() + "\"";
      } else {
        // Try invoking a getExpectedJson() method if it exists
        try {
          Method method = clazz.getMethod("getExpectedJson");
          Object results = method.invoke(obj);
          return (String) results;
        } catch (SecurityException e) {
          throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
          throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
          throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public int hashCode() {
      return value == null ? 0 : value.hashCode();
    }
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      MyParameterizedType other = (MyParameterizedType) obj;
      if (value == null) {
        if (other.value != null)
          return false;
      } else if (!value.equals(other.value))
        return false;
      return true;
    }
  }

  private static class MyParameterizedTypeAdapter<T>
      implements JsonSerializer<MyParameterizedType<T>>, JsonDeserializer<MyParameterizedType<T>> {
    @SuppressWarnings("unchecked")
    public static<T> String getExpectedJson(MyParameterizedType<T> obj) {
      Class<T> clazz = (Class<T>) obj.value.getClass();
      boolean addQuotes = !clazz.isArray() && !Primitives.unwrap(clazz).isPrimitive();
      StringBuilder sb = new StringBuilder("{\"");
      sb.append(obj.value.getClass().getSimpleName()).append("\":");
      if (addQuotes) {
        sb.append("\"");
      }
      sb.append(obj.value.toString());
      if (addQuotes) {
        sb.append("\"");
      }
      sb.append("}");
      return sb.toString();
    }
    public JsonElement serialize(MyParameterizedType<T> src, Type classOfSrc,
            JsonSerializationContext context) {
      JsonObject json = new JsonObject();
      T value = src.getValue();
      json.add(value.getClass().getSimpleName(), context.serialize(value));
      return json;
    }
    @SuppressWarnings("unchecked")
    public MyParameterizedType<T> deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException {
      Type genericClass = new TypeInfo(typeOfT).getGenericClass();
      TypeInfo typeInfo = new TypeInfo(genericClass);
      String className = typeInfo.getTopLevelClass().getSimpleName();
      T value = (T) json.getAsJsonObject().get(className).getAsObject();
      if (typeInfo.isPrimitive()) {
        PrimitiveTypeAdapter typeAdapter = new PrimitiveTypeAdapter();
        value = (T) typeAdapter.adaptType(value, typeInfo.getTopLevelClass());
      }
      return new MyParameterizedType<T>(value);
    }
  }

  private static class MyParameterizedTypeInstanceCreator<T>
      implements InstanceCreator<MyParameterizedType<T>>{
    private final T instanceOfT;
    /**
     * Caution the specified instance is reused by the instance creator for each call.
     * This means that the fields of the same objects will be overwritten by Gson.
     * This is usually fine in tests since there we deserialize just once, but quite
     * dangerous in practice.
     *
     * @param instanceOfT
     */
    public MyParameterizedTypeInstanceCreator(T instanceOfT) {
      this.instanceOfT = instanceOfT;
    }
    public MyParameterizedType<T> createInstance(Type type) {
      return new MyParameterizedType<T>(instanceOfT);
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    gson = new Gson();
  }

  public void testDeserializeParameterizedType() throws Exception {
    BagOfPrimitives bag = new BagOfPrimitives();
    MyParameterizedType<BagOfPrimitives> expected = new MyParameterizedType<BagOfPrimitives>(bag);
    Type expectedType = new TypeToken<MyParameterizedType<BagOfPrimitives>>() {}.getType();
    BagOfPrimitives bagDefaultInstance = new BagOfPrimitives();
    gson.registerInstanceCreator(expectedType,
        new MyParameterizedTypeInstanceCreator<BagOfPrimitives>(bagDefaultInstance));

    String json = expected.getExpectedJson();
    MyParameterizedType<Integer> actual = gson.fromJson(json, expectedType);
    assertEquals(expected, actual);
  }

  public void testSerializeParameterizedTypes() throws Exception {
    MyParameterizedType<Integer> src = new MyParameterizedType<Integer>(10);
    Type typeOfSrc = new TypeToken<MyParameterizedType<Integer>>() {}.getType();
    String json = gson.toJson(src, typeOfSrc);
    assertEquals(src.getExpectedJson(), json);
  }

  public void testSerializeParameterizedTypeWithCustomSerializer() {
    Type ptIntegerType = new TypeToken<MyParameterizedType<Integer>>() {}.getType();
    Type ptStringType = new TypeToken<MyParameterizedType<String>>() {}.getType();
    gson.registerSerializer(ptIntegerType, new MyParameterizedTypeAdapter<Integer>());
    gson.registerSerializer(ptStringType, new MyParameterizedTypeAdapter<String>());
    MyParameterizedType<Integer> intTarget = new MyParameterizedType<Integer>(10);
    String json = gson.toJson(intTarget, ptIntegerType);
    assertEquals(MyParameterizedTypeAdapter.<Integer>getExpectedJson(intTarget), json);

    MyParameterizedType<String> stringTarget = new MyParameterizedType<String>("abc");
    json = gson.toJson(stringTarget, ptStringType);
    assertEquals(MyParameterizedTypeAdapter.<String>getExpectedJson(stringTarget), json);
  }

  public void testDeserializeParameterizedTypesWithCustomDeserializer() {
    Type ptIntegerType = new TypeToken<MyParameterizedType<Integer>>() {}.getType();
    Type ptStringType = new TypeToken<MyParameterizedType<String>>() {}.getType();
    gson.registerDeserializer(ptIntegerType, new MyParameterizedTypeAdapter<Integer>());
    gson.registerInstanceCreator(ptIntegerType,
        new MyParameterizedTypeInstanceCreator<Integer>(new Integer(0)));

    MyParameterizedType<Integer> src = new MyParameterizedType<Integer>(10);
    String json = MyParameterizedTypeAdapter.<Integer>getExpectedJson(src);
    MyParameterizedType<Integer> intTarget = gson.fromJson(json, ptIntegerType);
    assertEquals(10, (int) intTarget.value);

    gson.registerDeserializer(ptStringType, new MyParameterizedTypeAdapter<String>());
    gson.registerInstanceCreator(ptStringType,
            new MyParameterizedTypeInstanceCreator<String>(""));

    MyParameterizedType<String> srcStr = new MyParameterizedType<String>("abc");
    json = MyParameterizedTypeAdapter.<String>getExpectedJson(srcStr);
    MyParameterizedType<String> stringTarget = gson.fromJson(json, ptStringType);
    assertEquals("abc", stringTarget.value);
  }

  public void testDeserializeParameterizedTypeWithReader() throws Exception {
    BagOfPrimitives bag = new BagOfPrimitives();
    MyParameterizedType<BagOfPrimitives> expected = new MyParameterizedType<BagOfPrimitives>(bag);
    Type expectedType = new TypeToken<MyParameterizedType<BagOfPrimitives>>() {}.getType();
    BagOfPrimitives bagDefaultInstance = new BagOfPrimitives();
    gson.registerInstanceCreator(expectedType,
        new MyParameterizedTypeInstanceCreator<BagOfPrimitives>(bagDefaultInstance));

    Reader json = new StringReader(expected.getExpectedJson());
    MyParameterizedType<Integer> actual = gson.fromJson(json, expectedType);
    assertEquals(expected, actual);
  }

  public void testSerializeParameterizedTypesWithWriter() throws Exception {
    Writer writer = new StringWriter();
    MyParameterizedType<Integer> src = new MyParameterizedType<Integer>(10);
    Type typeOfSrc = new TypeToken<MyParameterizedType<Integer>>() {}.getType();
    gson.toJson(src, typeOfSrc, writer);
    assertEquals(src.getExpectedJson(), writer.toString());
  }
}
