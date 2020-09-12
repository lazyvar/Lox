package com.hasz.lang.lox;

import com.hasz.lang.lox.Expr;

class Interpreter implements Expr.Visitor<Object> {
  void interpret(Expr expression) {
    try {
      Object value = evaluate(expression);
      System.out.println(stringify(value));
    } catch (RuntimeError error) {
      Main.runtimeError(error);
    }
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

  private String stringify(Object object) {
    if (object == null) return "nil";

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

