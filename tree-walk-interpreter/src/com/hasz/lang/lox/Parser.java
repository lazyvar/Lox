package com.hasz.lang.lox;

import com.sun.xml.internal.bind.v2.model.core.ID;

import java.util.List;

import static com.hasz.lang.lox.TokenType.*;

class Parser {
  private static class ParseError extends RuntimeException { }

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  Expr parse() {
    try {
      return expression();
    } catch (ParseError error) {
      return null;
    }
  }

  private Expr expression() {
    return equality();
  }

  private Expr equality() {
    Expr expr = comparison();

    while (matchAndAdvance(BANG_EQUAL, EQUAL_EQUAL)) {
      expr = new Expr.Binary(expr, previous(), comparison());
    }

    return expr;
  }

  private Expr comparison() {
    Expr expr = addition();

    while (matchAndAdvance(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      expr = new Expr.Binary(expr, previous(), addition());
    }

    return expr;
  }

  private Expr addition() {
    Expr expr = multiplication();

    while (matchAndAdvance(MINUS, PLUS)) {
      expr = new Expr.Binary(expr, previous(), multiplication());
    }

    return expr;
  }

  private Expr multiplication() {
    Expr expr = unary();

    while (matchAndAdvance(STAR, SLASH)) {
      expr = new Expr.Binary(expr, previous(), unary());
    }

    return expr;
  }

  private Expr unary() {
    if (matchAndAdvance(BANG, MINUS)) {
      return new Expr.Unary(previous(), unary());
    } else {
      return primary();
    }
  }

  private Expr primary() {
    if (matchAndAdvance(FALSE)) {
      return new Expr.Literal(false);
    }

    if (matchAndAdvance(TRUE)) {
      return new Expr.Literal(false);
    }

    if (matchAndAdvance(NIL)) {
      return new Expr.Literal(false);
    }

    if (matchAndAdvance(STRING, NUMBER)) {
      return new Expr.Literal(previous().literal);
    }

    if (matchAndAdvance(LEFT_PAREN)) {
      Expr expr = expression();

      consume(RIGHT_PAREN, "Expected ')' after expression.");

      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expected expression.");
  }

  private boolean matchAndAdvance(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();

        return true;
      }
    }

    return false;
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) {
      return advance();
    } else {
      throw error(peek(), message);
    }
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) {
      return false;
    }

    return peek().type == type;
  }

  private Token advance() {
    if (!isAtEnd()) {
      current++;
    }

    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private ParseError error(Token token, String message) {
    Main.error(token, message);

    return new ParseError();
  }

  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) {
        return;
      }

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }
}
