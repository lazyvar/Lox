package com.hasz.lang.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Stack;

public class Main {
  private static final Interpreter interpreter = new Interpreter();
  private static final AstPrinter printer = new AstPrinter();

  static ParseError parseError = null;
  static RuntimeError runtimeError = null;

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));

    run(new String(bytes, Charset.defaultCharset()));

    if (parseError != null) {
      System.exit(65);
    }

    if (runtimeError != null) {
      System.exit(70);
    }
  }

  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (;;) {
      System.out.print("\uD83E\uDD6F > ");

      String line = reader.readLine();

      if (line == null) break;

      StringRendering lastStatement = new StringRendering(run(line));

      if (parseError != null) {
        report(parseError);
      } else if (runtimeError != null) {
        report(runtimeError);
      } else {
        System.out.println("  => " + lastStatement);
      }

      parseError = null;
      runtimeError = null;
    }
  }

  private static Object run(String source) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();
    Parser parser = new Parser(tokens);
    List<Stmt> statements = parser.parse();
    Resolver resolver = new Resolver(interpreter);

    if (parseError != null) {
      return null;
    }

    resolver.resolve(statements);

    if (parseError != null) {
      return null;
    }

    return interpreter.interpret(statements);
  }

  static void error(int line, String message) {
    parseError = new ParseError(line, message);
  }

  static void error(Token token, String message) {
    parseError = new ParseError(token, message);
  }

  private static void report(RuntimeError error) {
    printError(error.getMessage() + "\n[line " + error.token.line + "]");
  }

  private static void report(ParseError error) {
    printError(error.getMessage());
  }

  private static void printError(String message) {
    System.out.println("\u001B[31m" + message + "\u001B[0m");
  }

  public static void runtimeError(RuntimeError error) {
    runtimeError = error;
  }
}
