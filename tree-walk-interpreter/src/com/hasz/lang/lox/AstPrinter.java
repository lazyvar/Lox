package com.hasz.lang.lox;

class AstPrinter implements Expr.Visitor<String> {
  void print(Expr expr) {
    System.out.println(describe(expr));
  }

  String describe(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public String visitAssignExpr(Expr.Assign expr) {
    return parenthesize("= " + expr.name, expr.value);
  }

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) {
      return "nil";
    }

    return expr.value.toString();
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  @Override
  public String visitConditionalExpr(Expr.Conditional expr) {
    return parenthesize("?", expr.equality, expr.ifBranch, expr.elseBranch);
  }

  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return parenthesize("access-var", expr);
  }

  private String parenthesize(String name, Expr... expressions) {
    StringBuilder builder = new StringBuilder();

    builder.append("(").append(name);

    for (Expr expr : expressions) {
      builder.append(" ");
      builder.append(expr.accept(this));
    }

    builder.append(")");

    return builder.toString();
  }
}

