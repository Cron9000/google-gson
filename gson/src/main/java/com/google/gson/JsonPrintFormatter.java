/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Formats Json in a nicely indented way with a specified print margin.
 * This printer tries to keep elements on the same line as much as possible
 * while respecting right margin.
 *
 * @author Inderjeet Singh
 */
final class JsonPrintFormatter implements JsonFormatter {

  private final int printMargin;
  private final int indentationSize;
  private final int rightMargin;
  private final boolean escapeHtmlChars;

  public static final int DEFAULT_PRINT_MARGIN = 80;
  public static final int DEFAULT_INDENTATION_SIZE = 2;
  public static final int DEFAULT_RIGHT_MARGIN = 4;

  JsonPrintFormatter() {
    this(true);
  }
  
  JsonPrintFormatter(boolean escapeHtmlChars) {
    this(DEFAULT_PRINT_MARGIN, DEFAULT_INDENTATION_SIZE, DEFAULT_RIGHT_MARGIN, escapeHtmlChars);
  }

  JsonPrintFormatter(int printMargin, int indentationSize, int rightMargin,
      boolean escapeHtmlChars) {
    this.printMargin = printMargin;
    this.indentationSize = indentationSize;
    this.rightMargin = rightMargin;
    this.escapeHtmlChars = escapeHtmlChars;
  }

  private class JsonWriter {
    private final Appendable writer;
    private StringBuilder line;
    private int level;
    JsonWriter(Appendable writer) {
      this.writer = writer;
      level = 0;
      line = new StringBuilder();
    }

    void key(String key) throws IOException {
      breakLineIfThisToNextExceedsLimit(key.length() + 2);
      getLine().append('"');
      getLine().append(key);
      getLine().append('"');
    }

    void value(String value) throws IOException {
    	breakLineIfThisToNextExceedsLimit(value.length() + 2);
        getLine().append(value);
    }

    void fieldSeparator() throws IOException {
      getLine().append(':');
      breakLineIfNeeded();
    }

    void elementSeparator() throws IOException {
      getLine().append(',');
      breakLineIfNeeded();
    }

    void beginObject() throws IOException {
      breakLineIfNeeded();
      getLine().append('{');
      ++level;
    }

    void endObject() {
      getLine().append('}');
      --level;
    }

    void beginArray() throws IOException {
      breakLineIfNeeded();
      getLine().append('[');
      ++level;
    }

    void endArray() {
      getLine().append(']');
      --level;
    }

    private void breakLineIfNeeded() throws IOException {
      breakLineIfThisToNextExceedsLimit(0);
    }
    
    private void breakLineIfThisToNextExceedsLimit(int nextLength) throws IOException {
        if (getLine().length() + nextLength > printMargin - rightMargin) {
          finishLine();
        }
      }

    private void finishLine() throws IOException {
      if (line != null) {
        writer.append(line).append("\n");
      }
      line = null;
    }

    private StringBuilder getLine() {
      if (line == null) {
        createNewLine();
      }
      return line;
    }

    private void createNewLine() {
      line = new StringBuilder();
      for (int i = 0; i < level; ++i) {
        for (int j = 0; j < indentationSize; ++j) {
          line.append(' ');
        }
      }
    }
  }

  private class PrintFormattingVisitor implements JsonElementVisitor {
    private final Stack<Boolean> firstElementInLevel;
    private final JsonWriter writer;
    private final Escaper escaper;
    private final boolean serializeNulls;

    PrintFormattingVisitor(JsonWriter writer, Escaper escaper, boolean serializeNulls) {
      this.writer = writer;
      this.escaper = escaper;
      this.serializeNulls = serializeNulls;
      this.firstElementInLevel = new Stack<Boolean>();
    }

    private void addCommaCheckingFirst() throws IOException {
      if (firstElementInLevel.peek()) {
        // No longer first
        firstElementInLevel.pop();
        firstElementInLevel.push(false);
      } else {
        writer.elementSeparator();
      }
    }

    public void startArray(JsonArray array) throws IOException {
      firstElementInLevel.push(true);
      writer.beginArray();
    }

    public void visitArrayMember(JsonArray parent, JsonPrimitive member, 
        boolean isFirst) throws IOException {
      addCommaCheckingFirst();
      writer.value(escapeJsonPrimitive(member));
    }

    public void visitArrayMember(JsonArray parent, JsonArray member, 
        boolean first) throws IOException {
      addCommaCheckingFirst();
    }

    public void visitArrayMember(JsonArray parent, JsonObject member, 
        boolean first) throws IOException {
      addCommaCheckingFirst();
    }

    public void visitNullArrayMember(JsonArray parent, boolean isFirst) throws IOException {
      addCommaCheckingFirst();
    }

    public void endArray(JsonArray array) {
      writer.endArray();
      firstElementInLevel.pop();
    }

    public void startObject(JsonObject object) throws IOException {
      firstElementInLevel.push(true);
      writer.beginObject();
    }

    public void visitObjectMember(JsonObject parent, String memberName, JsonPrimitive member, 
        boolean isFirst) throws IOException {
      addCommaCheckingFirst();
      writer.key(memberName);
      writer.fieldSeparator();
      writer.value(escapeJsonPrimitive(member));
    }

    public void visitObjectMember(JsonObject parent, String memberName, JsonArray member, 
        boolean isFirst) throws IOException {
      addCommaCheckingFirst();
      writer.key(memberName);
      writer.fieldSeparator();
    }

    public void visitObjectMember(JsonObject parent, String memberName, JsonObject member, 
        boolean isFirst) throws IOException {
      addCommaCheckingFirst();
      writer.key(memberName);
      writer.fieldSeparator();
    }

    public void visitNullObjectMember(JsonObject parent, String memberName, 
        boolean isFirst) throws IOException {
      if (serializeNulls) {
        visitObjectMember(parent, memberName, (JsonObject) null, isFirst);
      }
    }
    
    public void endObject(JsonObject object) {
      writer.endObject();
      firstElementInLevel.pop();
    }

    public void visitPrimitive(JsonPrimitive primitive) throws IOException {
      writer.value(escapeJsonPrimitive(primitive));
    }

    public void visitNull() throws IOException {
      writer.value("null");
    }
    
    private String escapeJsonPrimitive(JsonPrimitive member) throws IOException {
      StringBuilder builder = new StringBuilder();
      member.toString(builder, escaper);
      return builder.toString();
    }
  }

  public void format(JsonElement root, Appendable writer, 
      boolean serializeNulls) throws IOException {
    if (root == null) {
      return;
    }
    JsonWriter jsonWriter = new JsonWriter(writer);
    JsonElementVisitor visitor = new PrintFormattingVisitor(
        jsonWriter, new Escaper(escapeHtmlChars), serializeNulls);    
    JsonTreeNavigator navigator = new JsonTreeNavigator(visitor, serializeNulls);
    navigator.navigate(root);
    jsonWriter.finishLine();
  }
}
