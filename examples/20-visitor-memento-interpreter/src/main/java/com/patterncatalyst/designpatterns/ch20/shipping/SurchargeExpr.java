package com.patterncatalyst.designpatterns.ch20.shipping;

/**
 * A tiny surcharge-expression grammar: a fixed cents amount, a sum of two
 * expressions, an expression scaled by a dimensionless factor, and a
 * weight-conditional choice between two expressions. {@link SurchargeExprs}
 * walks this grammar to evaluate it and to render it as text; how its
 * pieces are nested, not just which pieces are present, decides the
 * evaluated result.
 */
public sealed interface SurchargeExpr permits Literal, Add, Multiply, IfWeightAbove {
}
