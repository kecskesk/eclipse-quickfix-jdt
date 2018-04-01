package hu.kecskesk.custommarker.processor;

import java.util.Collection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import hu.kecskesk.utils.Constants;

public class QuickFixStreamForEach extends QuickFixBase {

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return Constants.FOR_EACH_CONSTANT.problemId == problemId;
	}

	@SuppressWarnings("unchecked")
	public void addProposal(IInvocationContext context, IProblemLocation problem,
			Collection<IJavaCompletionProposal> proposals) throws JavaModelException {
		ICompilationUnit cu = context.getCompilationUnit();
		ASTNode selectedNode = problem.getCoveredNode(context.getASTRoot());
		AST ast = selectedNode.getAST();
		if (ast == null) {
			return;
		}

		ASTRewrite rewrite = ASTRewrite.create(ast);
		String label = Constants.FOR_EACH_CONSTANT.quickFixLabel;

		// 0. Make sure we grabbed a method parameter
		if (!(selectedNode instanceof EnhancedForStatement)) {
			return;
		}
		
		EnhancedForStatement enhancedFor = (EnhancedForStatement) selectedNode;
		
		VariableDeclarationFragment variableDeclarationFragment = ast.newVariableDeclarationFragment();
		variableDeclarationFragment.setName((SimpleName) rewrite.createCopyTarget(enhancedFor.getParameter().getName()));
		
		LambdaExpression lambdaExpression = ast.newLambdaExpression();
		lambdaExpression.parameters().add(variableDeclarationFragment);
		lambdaExpression.setBody(rewrite.createCopyTarget(enhancedFor.getBody()));

		MethodInvocation methodInvocation = ast.newMethodInvocation();
		methodInvocation.setExpression((Expression) rewrite.createCopyTarget(enhancedFor.getExpression()));
		methodInvocation.setName(ast.newSimpleName("forEach"));
		methodInvocation.arguments().add(lambdaExpression);
		
		ExpressionStatement expressionStatement = ast.newExpressionStatement(methodInvocation);
		
		rewrite.replace(selectedNode, expressionStatement, null);
		
		// 4. Send the proposal
		proposals.add(createProposalFromRewrite(cu, rewrite, label));
	}
}
