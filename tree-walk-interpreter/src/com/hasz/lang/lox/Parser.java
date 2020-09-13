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
      if (matchAndAdvance(FUN)) {
        return funDeclaration("function");
      } else if (matchAndAdvance(VAR)) {
        return varDeclaration();
      } else {
        return statement();
      }
    } catch (ParseError error) {
      synchronize();

      return null;
    }
  }

  private Stmt funDeclaration(String kind) {
    Token name = consume(IDENTIFIER, "Expected a variable name.");
    List<Token> parameters = new ArrayList<>();

    consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");

    if (!check(RIGHT_PAREN)) {
      do {
        parameters.add(consume(IDENTIFIER, "Expect parameter name."));
      } while (matchAndAdvance(COMMA));
    }

    consume(RIGHT_PAREN, "Expect ')' after parameters.");
    consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");

    List<Stmt> body = block();

    return new Stmt.Function(name, parameters, body);
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
    if (matchAndAdvance(FOR)) {
      return forStatement();
    }

    if (matchAndAdvance(IF)) {
      return ifStatement();
    }

    if (matchAndAdvance(PRINT)) {
      return printStatement();
    }

    if (matchAndAdvance(RETURN)) {
      return returnStatement();
    }

    if (matchAndAdvance(WHILE)) {
      return whileStatement();
    }

    if (matchAndAdvance(LEFT_BRACE)) {
      return new Stmt.ScopedBlock(block());
    }

    if (matchAndAdvance(SEMICOLON)) {
      return new Stmt.Expression(new Expr.Literal(null));
    }

    return expressionStatement();
  }

  private Stmt printStatement() {
    Expr value = expression();

    consume(SEMICOLON, "Expected ';' after value.");

    return new Stmt.Print(value);
  }

  private Stmt returnStatement() {
    Token keyword = previous();
    Expr value = null;

    if (!check(SEMICOLON)) {
      value = expression();
    }

    consume(SEMICOLON, "Expected ';' after return value.");

    return new Stmt.Return(keyword, value);
  }

  private Stmt forStatement() {
    consume(LEFT_PAREN, "Expected '(' after 'for'.");

    Stmt initializer;

    if (matchAndAdvance(SEMICOLON)) {
      initializer = null;
    } else if (matchAndAdvance(VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }

    Expr condition = null;

    if (!check(SEMICOLON)) {
      condition = expression();
    }

    consume(SEMICOLON, "Expected ';' after loop condition.");

    Expr increment = null;

    if (!check(RIGHT_PAREN)) {
      increment = expression();
    }

    consume(RIGHT_PAREN, "Expected ')' after for clauses.");
    consume(LEFT_BRACE, "Expected '{' after ')'.");

    Stmt body = new Stmt.Block(block());

    return new Stmt.For(initializer, condition, increment, body);
  }

  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expected '(' after 'if'.");

    Expr condition = expression();

    consume(RIGHT_PAREN, "Expected ')' after 'if'.");
    consume(LEFT_BRACE, "Expected '{' after ')'.");

    Stmt thenBranch = new Stmt.Block(block());
    Stmt elseBranch = null;

    if (matchAndAdvance(ELSE)) {
      consume(LEFT_BRACE, "Expected '(' after 'else'.");
      elseBranch = new Stmt.Block(block());
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  private Stmt whileStatement() {
    consume(LEFT_PAREN, "Expected '(' after 'while'.");

    Expr condition = expression();

    consume(RIGHT_PAREN, "Expected ')' after 'while'.");
    consume(LEFT_BRACE, "Expected '{' after ')'.");

    Stmt body = new Stmt.Block(block());

    return new Stmt.While(condition, body);
  }

  private Stmt expressionStatement() {
    Expr expr = expression();

    consume(SEMICOLON, "Expected ';' after expression.");

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
    Expr equality = or();

    if (matchAndAdvance(QUESTION_MARK)) {
      Expr ifBranch = expression();

      consume(COLON, "Expected ':' after if branch of conditional expression.");

      return new Expr.Conditional(equality, ifBranch, conditional());
    }

    return equality;
  }

  private Expr or() {
    Expr expr = and();

    while (matchAndAdvance(OR)) {
      expr = new Expr.Logical(expr, previous(), and());
    }

    return expr;
  }

  private Expr and() {
    Expr expr = equality();

    while (matchAndAdvance(AND)) {
      expr = new Expr.Logical(expr, previous(), equality());
    }

    return expr;
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
      return call();
    }
  }

  private Expr call() {
    Expr expr = primary();

    while (true) {
      if (matchAndAdvance(LEFT_PAREN)) {
        expr = finishCall(expr);
      } else {
        break;
      }
    }

    return expr;
  }

  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();

    if (!check(RIGHT_PAREN)) {
      do {
        arguments.add(conditional());
      } while (matchAndAdvance(COMMA));
    }

    Token paren = consume(RIGHT_PAREN, "Expected ')' after arguments.");

    return new Expr.Call(callee, paren, arguments);
  }

  private Expr primary() {
    if (matchAndAdvance(FALSE)) {
      return new Expr.Literal(false);
    }

    if (matchAndAdvance(TRUE)) {
      return new Expr.Literal(true);
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
