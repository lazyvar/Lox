package com.hasz.lang.lox;

import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  private Environment currentEnvironment = new Environment();

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Main.runtimeError(error);
    }
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(currentEnvironment));

    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);

    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));

    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object initialValue = null;

    if (stmt.initializer != null) {
      initialValue = evaluate(stmt.initializer);
    }

    currentEnvironment.define(stmt.name.lexeme, initialValue);

    return null;
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);

    currentEnvironment.define(expr.name.lexeme, value);

    return value;
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case GREATER:
      case GREATER_EQUAL:
      case LESS:
      case LESS_EQUAL:
      case MINUS:
      case SLASH:
      case STAR:
        checkNumberOperands(expr.operator, left, right);
      case PLUS:
        checkConcatOperands(expr.operator, left, right);
    }

    switch (expr.operator.type) {
      case COMMA:
        return right;
      case GREATER:
        return (double) left > (double) right;
      case GREATER_EQUAL:
        return (double) left >= (double) right;
      case LESS:
        return (double) left < (double) right;
      case LESS_EQUAL:
        return (double) left <= (double) right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double) left + (double) right;
        }

        if (left instanceof String && right instanceof String) {
          return left + (String) right;
        }
      case MINUS:
        return (Double) left - (Double) right;
      case SLASH:
        return (Double) left / (Double) right;
      case STAR:
        return (Double) left * (Double) right;
      case BANG_EQUAL:  return !isEqual(left, right);
      case EQUAL_EQUAL: return isEqual(left, right);
      default: return null;
    }
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case MINUS:
        checkNumberOperand(expr.operator, right);

        return -(double) right;
      case BANG:
        return !isTruthy(right);
      default: return null;
    }
  }

  @Override
  public Object visitConditionalExpr(Expr.Conditional expr) {
    boolean equality = isTruthy(evaluate(expr.equality));

    return equality ? evaluate(expr.ifBranch) : evaluate(expr.elseBranch);
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return currentEnvironment.get(expr.name);
  }

  private String stringify(Object object) {
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

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.currentEnvironment;

    try {
      this.currentEnvironment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.currentEnvironment = previous;
    }
  }

  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean)object;

    return true;
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (!(operand instanceof Double)) {
      throw new RuntimeError(operator, "Expected operand to be a number.");
    }
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (!(left instanceof Double && right instanceof Double)) {
      throw new RuntimeError(operator, "Expected operands to be numbers.");
    }
  }

  private void checkConcatOperands(Token operator, Object left, Object right) {
    boolean bothNumbers = left instanceof Double && right instanceof Double;
    boolean bothStrings = left instanceof String && right instanceof String;

    if (!bothNumbers && !bothStrings) {
      throw new RuntimeError(operator, "Expected operands to both be numbers or both be strings");
    }
  }

  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) {
      return true;
    }

    if (a == null) {
      return false;
    }

    return a.equals(b);
  }
}

