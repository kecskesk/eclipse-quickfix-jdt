package hu.kecskesk.custommarker.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.swt.graphics.Image;

import hu.kecskesk.custommarker.handlers.MarkerGenerator;

@SuppressWarnings("restriction")
public class QuickFixSteamForEach implements IQuickFixProcessor {

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return MarkerGenerator.MY_JDT_PROBLEM_ID == problemId;
	}

	@Override
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations)
			throws CoreException {
		HashSet<Integer> handledProblems = new HashSet<Integer>(locations.length);
		ArrayList<IJavaCompletionProposal> resultingCollections = new ArrayList<IJavaCompletionProposal>();
		for (int i = 0; i < locations.length; i++) {
			IProblemLocation curr = locations[i];
			Integer id = curr.getProblemId();
			if (handledProblems.add(id)) {
				addNullCheckProposal(context, curr, resultingCollections);
			}
		}
		return resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
	}

	@SuppressWarnings("unchecked")
	public static void addNullCheckProposal(IInvocationContext context, IProblemLocation problem,
			Collection<IJavaCompletionProposal> proposals) throws JavaModelException {
		ICompilationUnit cu = context.getCompilationUnit();
		ASTNode selectedNode = problem.getCoveredNode(context.getASTRoot());
		AST ast = selectedNode.getAST();
		if (ast == null) {
			return;
		}

		ASTRewrite rewrite = ASTRewrite.create(ast);
		String label = "Use Stream API";

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

	private static ASTRewriteCorrectionProposal createProposalFromRewrite(ICompilationUnit cu, ASTRewrite rewrite,
			String label) {
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
		return proposal;
	}
}
