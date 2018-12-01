package com.google.refine.tests.grel.ast;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.refine.grel.ast.LiteralExpr;

public class LiteralExprTest {
	@Test
	public void intLiteralToString() {
		LiteralExpr expr = new LiteralExpr(42);
		assertEquals("42", expr.toString());
	}
	
	@Test
	public void stringLiteralToString() {
		LiteralExpr expr = new LiteralExpr("string with \"\\backslash\"");
		assertEquals("\"string with \\\"\\\\backslash\\\"\"", expr.toString());
	}
}
