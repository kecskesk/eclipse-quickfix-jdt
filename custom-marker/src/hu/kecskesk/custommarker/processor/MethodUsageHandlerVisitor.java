package hu.kecskesk.custommarker.processor;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class MethodUsageHandlerVisitor extends ASTVisitor {
	private final SingleVariableDeclaration oldParameter;
	private final MethodDeclaration method;
	private final ASTRewrite rewrite;

	public MethodUsageHandlerVisitor(SingleVariableDeclaration oldParameter, MethodDeclaration method,
			ASTRewrite rewrite) {
		this.oldParameter = oldParameter;
		this.method = method;
		this.rewrite = rewrite;
	}

	@Override
	public boolean visit(MethodInvocation methodInvocationNode) {
		IMethodBinding visited = methodInvocationNode.resolveMethodBinding();
		IMethodBinding methodBinding = method.resolveBinding();
		if (visited == methodBinding) {
			handleMethodCall(methodInvocationNode);
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	private void handleMethodCall(MethodInvocation methodInvocationNode) {
		AST ast = rewrite.getAST();
		int argumentPlace = getArgumentPlace(oldParameter, method);
		ASTNode originalArgument = (ASTNode) methodInvocationNode.arguments().get(argumentPlace);

		MethodInvocation ofWrapper = ast.newMethodInvocation();
		ofWrapper.setName(ast.newSimpleName("of"));
		ofWrapper.setExpression(ast.newSimpleName("Optional"));
		ofWrapper.arguments().add(rewrite.createMoveTarget(originalArgument));

		rewrite.replace(originalArgument, ofWrapper, null);
	}

	private int getArgumentPlace(SingleVariableDeclaration oldParameter, MethodDeclaration method) {
		return method.parameters().indexOf(oldParameter);
	}
}