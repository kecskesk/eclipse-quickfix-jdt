package hu.kecskesk.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.SimpleName;

import hu.kecskesk.custommarker.Activator;

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

	public static void addNewMarker(ASTNode variable, ICompilationUnit iCompilationUnit, CompilationUnit compilationUnit) throws CoreException {
		IMarker newMarker = iCompilationUnit.getResource().createMarker(Activator.ACTIVE_CONSTANT.markerType);

		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put(IMarker.LOCATION, variable.toString());
		attributes.put(IMarker.MESSAGE, Activator.ACTIVE_CONSTANT.solutionText);
		attributes.put(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
		attributes.put(IJavaModelMarker.ID, Activator.ACTIVE_CONSTANT.problemId);

		int startPosition = variable.getStartPosition();
		int length = variable.getLength();
		attributes.put(IMarker.CHAR_START, startPosition);
		attributes.put(IMarker.CHAR_END, startPosition + length);
		attributes.put(IMarker.LINE_NUMBER, compilationUnit.getLineNumber(startPosition));

		newMarker.setAttributes(attributes);
	}
}
