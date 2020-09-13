package com.hasz.lang.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
  final Environment enclosing;

  private final Map<String, Object> values = new HashMap<>();

  Environment() {
    enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  void define(String name, Object value) {
    values.put(name, value);
  }

  void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
    } else if (enclosing != null) {
      enclosing.assign(name, value);
    } else {
      throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
  }

  void assignAt(int distance, Token name, Object value) {
    ancestor(distance).values.put(name.lexeme, value);
  }

  Object getAt(int distance, String name) {
    return ancestor(distance).values.get(name);
  }

  Environment ancestor(int distance) {
    Environment environment = this;

    for (int i = 0; i < distance; i++) {
      assert environment != null;

      environment = environment.enclosing;
    }

    return environment;
  }

  Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    } else if (enclosing != null) {
      return enclosing.get(name);
    } else {
      throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
  }
}
