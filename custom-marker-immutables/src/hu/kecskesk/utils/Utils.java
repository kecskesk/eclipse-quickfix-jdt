package hu.kecskesk.utils;

import java.util.Optional;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.SimpleName;

public class Utils {
	public static Optional<SimpleName> getVariableIfNullCheck(InfixExpression infixExpression) {
		if (isNull(infixExpression.getLeftOperand())) {
			// null == variable
			if (isSimpleName(infixExpression.getRightOperand())) {
				return Optional.of((SimpleName) infixExpression.getRightOperand());
			}
		} else if (isNull(infixExpression.getRightOperand())) {
			// variable == null
			if (isSimpleName(infixExpression.getLeftOperand())) {
				return Optional.of((SimpleName) infixExpression.getLeftOperand());
			}
		}
		return Optional.empty();
	}

	public static boolean isNull(Expression exp) {
		return ASTNode.NULL_LITERAL == exp.getNodeType();
	}

	public static boolean isSimpleName(Expression exp) {
		return ASTNode.SIMPLE_NAME == exp.getNodeType();
	}
	
	public static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS9);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}
}
