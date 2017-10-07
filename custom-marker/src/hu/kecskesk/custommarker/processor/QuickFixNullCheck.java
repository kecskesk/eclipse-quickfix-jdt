package hu.kecskesk.custommarker.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
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
public class QuickFixNullCheck implements IQuickFixProcessor {

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return MarkerGenerator.MY_JDT_PROBLEM_ID == problemId;
	}

	@Override
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context,
			IProblemLocation[] locations) throws CoreException {
		HashSet<Integer> handledProblems= new HashSet<Integer>(locations.length);
		ArrayList<IJavaCompletionProposal> resultingCollections= new ArrayList<IJavaCompletionProposal>();
		for (int i= 0; i < locations.length; i++) {
			IProblemLocation curr= locations[i];
			Integer id= new Integer(curr.getProblemId());
			if (handledProblems.add(id)) {
				process(context, curr, resultingCollections);
			}
		}
		return resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
	}
	
	private void process(final IInvocationContext context,
			final IProblemLocation problem,
			final ArrayList<IJavaCompletionProposal> proposals)
			throws CoreException {
		final int id = problem.getProblemId();
		if (id == 0) { // no proposals for none-problem locations
			return;
		}
		switch (id) {
			// which proposal(s) do we want. Call a method to handle each proposal.
			case MarkerGenerator.MY_JDT_PROBLEM_ID:
				QuickFixNullCheck.addNullCheckProposal(context, problem, proposals);
				break;
		}
	}
	
	public static void addNullCheckProposal(IInvocationContext context, IProblemLocation problem, Collection<IJavaCompletionProposal> proposals){
		ICompilationUnit cu= context.getCompilationUnit();
		ASTNode selectedNode= problem.getCoveredNode(context.getASTRoot());
		AST ast = selectedNode.getAST();
		if (ast == null) {
			return;
		}
		
		ASTRewrite rewrite = ASTRewrite.create(ast);
		String label= "Use Optional class instead of nullable parameter";
		
		// Make sure we grabbed a method parameter
		if (!(selectedNode instanceof SingleVariableDeclaration)) {
			return;
		}
		SingleVariableDeclaration oldParameter = (SingleVariableDeclaration)selectedNode;
		if (!oldParameter.getType().isSimpleType()) {
			return;
		}
		
		ParameterizedType optionalType = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName("Optional")));
		SimpleType oldType = (SimpleType) oldParameter.getType();
		SimpleName oldTypeName = (SimpleName) oldType.getName();
		SimpleType baseType = ast.newSimpleType(ast.newSimpleName(oldTypeName.getIdentifier()));
		
		optionalType.typeArguments().add(baseType);

		SingleVariableDeclaration newParameter = ast.newSingleVariableDeclaration();
		newParameter.setName(ast.newSimpleName(oldParameter.getName().getIdentifier()));
		newParameter.setType(optionalType);
				
		// replace the dereferencing statement with the if statement using the 'rewrite' object.
		rewrite.replace(oldParameter, newParameter, null);
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
		
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
			
		proposals.add(proposal);
	}

}
