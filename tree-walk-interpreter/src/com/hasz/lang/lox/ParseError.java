package com.hasz.lang.lox;

class ParseError extends RuntimeException {
  ParseError(int line, String message) {
    super(message(line, "", message));
  }

  ParseError(Token token, String message) {
    super(message(token, message));
  }

  private static String message(int line, String where, String message) {
    return "[line " + line + "] Error" + where + ": " + message;
  }

  private static String message(Token token, String message) {
    String returnMessage = null;

    if (token.type == TokenType.EOF) {
      returnMessage = message(token.line, " at end", message);
    } else {
      returnMessage = message(token.line, " at '" + token.lexeme + "'", message);
    }

    return returnMessage;
  }
}
