package com.hasz.lang.lox;

class StringRendering {
  private Object object;

  StringRendering(Object object) {
    this.object = object;
  }

  @Override
  public String toString() {
    if (object == null) {
      return "nil";
    }

    if (object instanceof Double) {
      String text = object.toString();

      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }

      return text;
    }

    return object.toString();
  }
}
