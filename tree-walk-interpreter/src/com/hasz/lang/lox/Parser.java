package com.hasz.lang.lox;

import java.util.List;
import java.util.ArrayList;

import static com.hasz.lang.lox.TokenType.*;

class Parser {
  private static class ParseError extends RuntimeException { }

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();

    while (!isAtEnd()) {
      statements.add(declaration());
    }

    return statements;
  }

  private Stmt declaration() {
    try {
      if (matchAndAdvance(VAR)) {
        return varDeclaration();
      } else {
        return statement();
      }
    } catch (ParseError error) {
      synchronize();

      return null;
    }
  }

  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expected a variable name.");
    Expr initializer = null;

    if (matchAndAdvance(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Expected ';' after variable declaration.");

    return new Stmt.Var(name, initializer);
  }

  private Stmt statement() {
    if (matchAndAdvance(PRINT)) {
      return printStatement();
    }

    if (matchAndAdvance(LEFT_BRACE)) {
      return new Stmt.Block(block());
    }

    if (matchAndAdvance(SEMICOLON)) {
      return new Stmt.Expression(new Expr.Literal(null));
    }

    return expressionStatement();
  }

  private Stmt printStatement() {
    Expr value = expression();

    consume(SEMICOLON, "Expect ';' after value.");

    return new Stmt.Print(value);
  }

  private Stmt expressionStatement() {
    Expr expr = expression();

    consume(SEMICOLON, "Expect ';' after expression.");

    return new Stmt.Expression(expr);
  }

  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Expected '}' after block.");

    return statements;
  }

  private Expr expression() {
    return assignment();
  }

  private Expr assignment() {
    Expr expr = comma();

    if (matchAndAdvance(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable) expr).name;

        return new Expr.Assign(name, value);
      } else {
        error(equals, "Invalid assignment target.");
      }
    }

    return expr;
  }

  private Expr comma() {
    Expr expr = conditional();

    while (matchAndAdvance(COMMA)) {
      expr = new Expr.Binary(expr, previous(), conditional());
    }

    return expr;
  }

  private Expr conditional() {
    Expr equality = equality();

    if (matchAndAdvance(QUESTION_MARK)) {
      Expr ifBranch = expression();

      consume(COLON, "Expected ':' after if branch of conditional expression.");

      return new Expr.Conditional(equality, ifBranch, conditional());
    }

    return equality;
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
      return new Expr.Literal(null);
    }

    if (matchAndAdvance(STRING, NUMBER)) {
      return new Expr.Literal(previous().literal);
    }

    if (matchAndAdvance(IDENTIFIER)) {
      return new Expr.Variable(previous());
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
