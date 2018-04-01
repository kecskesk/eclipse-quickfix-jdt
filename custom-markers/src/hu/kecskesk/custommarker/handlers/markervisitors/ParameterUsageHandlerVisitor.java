package hu.kecskesk.custommarker.handlers.markervisitors;

import java.util.Optional;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import hu.kecskesk.utils.Utils;

public class ParameterUsageHandlerVisitor extends ASTVisitor {
	private final SingleVariableDeclaration oldParameter;
	private final ASTRewrite rewrite;

	public ParameterUsageHandlerVisitor(SingleVariableDeclaration oldParameter, ASTRewrite rewrite) {
		this.oldParameter = oldParameter;
		this.rewrite = rewrite;
	}

	@Override
	public boolean visit(SimpleName node) {
		IBinding bindingVisited = node.resolveBinding();
		IBinding bindingParameter = oldParameter.getName().resolveBinding();

		if (bindingVisited.equals(bindingParameter)) {
			handleUsageOfParameter(node, rewrite);
		}
		return false;
	}

	private void handleUsageOfParameter(SimpleName node, ASTRewrite rewrite) {
		ASTNode parentNode = node.getParent();
		if (parentNode instanceof InfixExpression) {
			handleInfixExpression(node, rewrite, (InfixExpression) parentNode);
		} else if (parentNode instanceof VariableDeclarationFragment || parentNode instanceof MethodInvocation
				|| parentNode instanceof SwitchStatement || parentNode instanceof ReturnStatement) {
			handleByReplaceWithGet(node, rewrite);
		} else if (parentNode instanceof Assignment) {
			handleAssignment(node, rewrite, (Assignment) parentNode);
		} else if (parentNode instanceof SingleVariableDeclaration) {
			// this is the parameter itself, we don't need to care about it
		} else {
			unknownCodePart(parentNode);
		}
	}

	private void unknownCodePart(ASTNode parentNode) {
		System.out.println("!!!!!!!! WARNING NEW USECASE: ");
		System.out.println(parentNode);
		System.out.println(parentNode.getClass().getName());
	}

	private void handleInfixExpression(SimpleName node, ASTRewrite rewrite, InfixExpression expression) {
		Optional<SimpleName> var = Utils.getVariableIfNullCheck(expression);
		if (var.isPresent() && var.get() == node) {

			AST ast = rewrite.getAST();
			MethodInvocation isPresentCall = ast.newMethodInvocation();
			isPresentCall.setName(ast.newSimpleName("isPresent"));
			isPresentCall.setExpression(ast.newSimpleName(node.getIdentifier()));

			if (expression.getOperator().equals(InfixExpression.Operator.EQUALS)) {
				rewrite.replace(expression, isPresentCall, null);
			} else if (expression.getOperator().equals(InfixExpression.Operator.NOT_EQUALS)) {
				PrefixExpression negationExpression = ast.newPrefixExpression();
				negationExpression.setOperator(PrefixExpression.Operator.NOT);
				negationExpression.setOperand(isPresentCall);
				rewrite.replace(expression, negationExpression, null);
			}
		} else {
			unknownCodePart(expression);
		}
	}

	private void handleByReplaceWithGet(SimpleName node, ASTRewrite rewrite) {
		AST ast = rewrite.getAST();
		MethodInvocation getCall = ast.newMethodInvocation();
		getCall.setName(ast.newSimpleName("get"));
		getCall.setExpression(ast.newSimpleName(node.getIdentifier()));
		rewrite.replace(node, getCall, null);
	}

	@SuppressWarnings("unchecked")
	private void handleAssignment(SimpleName node, ASTRewrite rewrite, Assignment expression) {
		if (expression.getLeftHandSide() == node) {
			AST ast = rewrite.getAST();
			Expression originalRightHandSide = expression.getRightHandSide();

			MethodInvocation ofWrapper = ast.newMethodInvocation();
			ofWrapper.setName(ast.newSimpleName("of"));
			ofWrapper.setExpression(ast.newSimpleName("Optional"));
			ofWrapper.arguments().add(rewrite.createCopyTarget(originalRightHandSide));

			rewrite.replace(originalRightHandSide, ofWrapper, null);
		} else {
			handleByReplaceWithGet(node, rewrite);
		}
	}
}