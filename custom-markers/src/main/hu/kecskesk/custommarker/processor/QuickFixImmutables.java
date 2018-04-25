package hu.kecskesk.custommarker.processor;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import hu.kecskesk.utils.Constants;

public class QuickFixImmutables extends QuickFixBase {

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return List.of(Constants.JDT_PROBLEM_ID_LIST, Constants.JDT_PROBLEM_ID_MAP, Constants.JDT_PROBLEM_ID_SET)
				.contains(problemId);
	}

	@SuppressWarnings({ "unchecked" })
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
		String methodIdentifier = oldMethodInvocation.getName().getIdentifier();
		if (!(methodIdentifier.equals("asList") || methodIdentifier.startsWith("unmodifiable"))) {
			return;
		}

		MethodInvocation listFactory = ast.newMethodInvocation();
		listFactory.setName(ast.newSimpleName("of"));
		if (methodIdentifier.contains("Map")) {
			listFactory.setExpression(ast.newSimpleName("Map"));
		} else if (methodIdentifier.contains("Set")) {
			listFactory.setExpression(ast.newSimpleName("Set"));
		} else {
			listFactory.setExpression(ast.newSimpleName("List"));
		}

		if (methodIdentifier.equals("asList")) {
			oldMethodInvocation.arguments().forEach((argument) -> {
				listFactory.arguments().add(rewrite.createCopyTarget((ASTNode) argument));
			});
		} else {
			if (oldMethodInvocation.arguments().size() == 1) {
				Object methodArgument = oldMethodInvocation.arguments().get(0);
				if (methodArgument instanceof SimpleName) {
					// IBinding argumentBinding = ((SimpleName) methodArgument).resolveBinding();	
					ITypeBinding typeBinding = ((SimpleName) methodArgument).resolveTypeBinding();
					if (typeBinding.getName().startsWith("List")) {
						// get list adds 
					} else if (typeBinding.getName().startsWith("Set")) {
						// get set adds
					} else if (typeBinding.getName().startsWith("Map")) {
						// get map adds
					}
				}
			}
			
			// find collection variables 
			// add them to list 
			// remove them from code
		}
		rewrite.replace(selectedNode, listFactory, null);

		// 4. Send the proposal
		proposals.add(createProposalFromRewrite(cu, rewrite, label));
	}
}
