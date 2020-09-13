package com.hasz.lang.lox;

import java.util.ArrayList;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Object> {
  final Environment globals = new Environment();

  private Environment currentEnvironment = globals;

  Interpreter() {
    globals.define("clock", new LoxCallable() {
      @Override
      public int arity() {
        return 0;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double) System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });
  }

  Object interpret(List<Stmt> statements) {
    ArrayList<Object> values = new ArrayList<>();

    try {
      for (Stmt statement : statements) {
        values.add(execute(statement));
      }
    } catch (RuntimeError error) {
      Main.runtimeError(error);
    }

    if (values.isEmpty()) {
      return null;
    } else {
      return values.get(values.size() - 1);
    }
  }

  @Override
  public Object visitScopedBlockStmt(Stmt.ScopedBlock stmt) {
    return executeBlock(stmt.statements, new Environment(currentEnvironment));
  }

  @Override
  public Object visitBlockStmt(Stmt.Block stmt) {
    return executeBlock(stmt.statements, currentEnvironment);
  }

  @Override
  public Object visitExpressionStmt(Stmt.Expression stmt) {
    return evaluate(stmt.expression);
  }

  @Override
  public Object visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(new StringRendering(value));

    return null;
  }

  @Override
  public Object visitReturnStmt(Stmt.Return stmt) {
    Object value = null;

    if (stmt.value != null) {
      value = evaluate(stmt.value);
    }

    throw new Return(value);
  }

  @Override
  public Object visitFunctionStmt(Stmt.Function stmt) {
    LoxFunction fn = new LoxFunction(stmt, currentEnvironment);

    currentEnvironment.define(stmt.name.lexeme, fn);

    return fn;
  }

  @Override
  public Object visitIfStmt(Stmt.If stmt) {
    boolean condition = isTruthy(evaluate(stmt.condition));

    if (condition) {
      return execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      return execute(stmt.elseBranch);
    } else {
      return null;
    }
  }

  @Override
  public Object visitVarStmt(Stmt.Var stmt) {
    Object initialValue = null;

    if (stmt.initializer != null) {
      initialValue = evaluate(stmt.initializer);
    }

    currentEnvironment.define(stmt.name.lexeme, initialValue);

    return initialValue;
  }

  @Override
  public Object visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body);
    }

    return null;
  }

  @Override
  public Object visitForStmt(Stmt.For stmt) {
    if (stmt.initial != null) {
      execute(stmt.initial);
    }

    while (stmt.condition == null || isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body);

      if (stmt.increment != null) {
        evaluate(stmt.increment);
      }
    }

    return null;
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);

    currentEnvironment.assign(expr.name, value);

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
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);
    List<Object> arguments = new ArrayList<>();

    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }

    if (callee == null) {
      throw new RuntimeError(expr.paren, "Variable is nil.");
    }

    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren, "Can only call functions and classes.");
    }

    LoxCallable function = (LoxCallable) callee;

    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
    }

    return function.call(this, arguments);
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
    boolean condition = isTruthy(evaluate(expr.condition));

    return condition ? evaluate(expr.ifBranch) : evaluate(expr.elseBranch);
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return currentEnvironment.get(expr.name);
  }

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);
    boolean leftIsTruthy = isTruthy(left);

    switch (expr.operator.type) {
      case OR:
        if (!leftIsTruthy) {
          return evaluate(expr.right);
        }
        break;
      case AND:
        if (leftIsTruthy) {
          return evaluate(expr.right);
        }
        break;
    }

    return left;
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private Object execute(Stmt stmt) {
    return stmt.accept(this);
  }

  Object executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.currentEnvironment;
    ArrayList<Object> values = new ArrayList<>();

    try {
      this.currentEnvironment = environment;

      for (Stmt statement : statements) {
        values.add(execute(statement));
      }
    } finally {
      this.currentEnvironment = previous;
    }

    if (values.isEmpty()) {
      return null;
    } else {
      return values.get(values.size() - 1);
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

