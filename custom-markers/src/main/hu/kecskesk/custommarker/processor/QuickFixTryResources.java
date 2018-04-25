package hu.kecskesk.custommarker.processor;

import java.util.Collection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import hu.kecskesk.utils.Constants;

public class QuickFixTryResources extends QuickFixBase {

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return Constants.TRY_RES_CONSTANT.problemId == problemId;
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
		String label = Constants.TRY_RES_CONSTANT.quickFixLabel;

		// 0. Make sure we grabbed a method parameter
		if (!(selectedNode instanceof VariableDeclarationExpression)) {
			return;
		}
		
		VariableDeclarationExpression expression = (VariableDeclarationExpression)selectedNode;
		VariableDeclarationFragment fragment = (VariableDeclarationFragment) expression.fragments().get(0);
		Expression baseExpression = fragment.getInitializer();
		while (!(baseExpression instanceof SimpleName)) {
			if (baseExpression == null) {
				return;
			}
			baseExpression = ((MethodInvocation) baseExpression).getExpression();
		}
		TryStatement tryStatement = (TryStatement) expression.getParent();
		
		TryStatement newTryStatement = ast.newTryStatement();
		newTryStatement.setBody((Block) rewrite.createCopyTarget(tryStatement.getBody()));
		tryStatement.catchClauses().forEach((catchClause) -> {
			newTryStatement.catchClauses().add(rewrite.createCopyTarget((ASTNode) catchClause));
		});
		
		newTryStatement.resources().add(rewrite.createCopyTarget(baseExpression));
		newTryStatement.resources().add(rewrite.createCopyTarget(expression));
		
		rewrite.replace(tryStatement, newTryStatement, null);

		// 4. Send the proposal
		proposals.add(createProposalFromRewrite(cu, rewrite, label));
	}
}
