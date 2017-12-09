package hu.kecskesk.custommarker.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;
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
public class QuickFixImmutableCollections implements IQuickFixProcessor {

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return List.of(MarkerGenerator.MY_JDT_PROBLEM_ID_LIST, MarkerGenerator.MY_JDT_PROBLEM_ID_MAP,
				MarkerGenerator.MY_JDT_PROBLEM_ID_SET).contains(problemId);
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
		String label = "Use factory method for immutable collections";

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

	private static ASTRewriteCorrectionProposal createProposalFromRewrite(ICompilationUnit cu, ASTRewrite rewrite,
			String label) {
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
		return proposal;
	}
}
