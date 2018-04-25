package hu.kecskesk.custommarker.processor;

import java.util.Collection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import hu.kecskesk.utils.Constants;

public class QuickFixDiamondOperator extends QuickFixBase {

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return Constants.ANONYM_CONSTANT.problemId == problemId;
	}

	public void addProposal(IInvocationContext context, IProblemLocation problem,
			Collection<IJavaCompletionProposal> proposals) throws JavaModelException {
		ICompilationUnit cu = context.getCompilationUnit();
		ASTNode selectedNode = problem.getCoveredNode(context.getASTRoot());
		AST ast = selectedNode.getAST();
		if (ast == null) {
			return;
		}

		ASTRewrite rewrite = ASTRewrite.create(ast);
		String label = Constants.ANONYM_CONSTANT.quickFixLabel;

		// 0. Make sure we grabbed a method parameter
		if (!(selectedNode instanceof ParameterizedType)) {
			return;
		}
		
		ParameterizedType oldType = (ParameterizedType) selectedNode;
		
		SimpleType parameterOfType = (SimpleType) rewrite.createCopyTarget(oldType.getType());
		ParameterizedType newType = ast.newParameterizedType(parameterOfType);
		rewrite.replace(selectedNode, newType, null);
		
		// 4. Send the proposal
		proposals.add(createProposalFromRewrite(cu, rewrite, label));
	}
}
