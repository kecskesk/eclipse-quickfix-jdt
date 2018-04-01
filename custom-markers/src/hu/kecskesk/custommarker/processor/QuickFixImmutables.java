package hu.kecskesk.custommarker.processor;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import hu.kecskesk.utils.Constants;

public class QuickFixImmutables extends QuickFixBase {

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return List.of(Constants.JDT_PROBLEM_ID_LIST, Constants.JDT_PROBLEM_ID_MAP,
				Constants.JDT_PROBLEM_ID_SET).contains(problemId); 
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
		String label = Constants.IMMUTABLE_CONSTANT.quickFixLabel;

		// 0. Make sure we grabbed a method parameter
		if (!(selectedNode instanceof MethodInvocation)) {
			return;
		}
		
		MethodInvocation oldMethodInvocation = (MethodInvocation) selectedNode;
		if (!oldMethodInvocation.getName().getIdentifier().equals("asList")) {
			return;
		}
		
		MethodInvocation listFactory = ast.newMethodInvocation();
		listFactory.setName(ast.newSimpleName("of"));
		listFactory.setExpression(ast.newSimpleName("List"));
		
		oldMethodInvocation.arguments().forEach((argument) -> {
			listFactory.arguments().add(rewrite.createCopyTarget((ASTNode)argument));
		});
		
		rewrite.replace(selectedNode, listFactory, null);

		// 4. Send the proposal
		proposals.add(createProposalFromRewrite(cu, rewrite, label));
	}
}
