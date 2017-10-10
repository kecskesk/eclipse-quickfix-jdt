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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
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
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations)
			throws CoreException {
		HashSet<Integer> handledProblems = new HashSet<Integer>(locations.length);
		ArrayList<IJavaCompletionProposal> resultingCollections = new ArrayList<IJavaCompletionProposal>();
		for (int i = 0; i < locations.length; i++) {
			IProblemLocation curr = locations[i];
			Integer id = new Integer(curr.getProblemId());
			if (handledProblems.add(id)) {
				process(context, curr, resultingCollections);
			}
		}
		return resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
	}

	private void process(final IInvocationContext context, final IProblemLocation problem,
			final ArrayList<IJavaCompletionProposal> proposals) throws CoreException {
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

	public static void addNullCheckProposal(IInvocationContext context, IProblemLocation problem,
			Collection<IJavaCompletionProposal> proposals) throws JavaModelException {
		ICompilationUnit cu = context.getCompilationUnit();
		ASTNode selectedNode = problem.getCoveredNode(context.getASTRoot());
		AST ast = selectedNode.getAST();
		if (ast == null) {
			return;
		}

		ASTRewrite rewrite = ASTRewrite.create(ast);
		String label = "Use Optional class instead of nullable parameter";

		// Make sure we grabbed a method parameter
		if (!(selectedNode instanceof SingleVariableDeclaration)) {
			return;
		}

		SingleVariableDeclaration oldParameter = (SingleVariableDeclaration) selectedNode;
		if (!oldParameter.getType().isSimpleType()) {
			return;
		}

		SingleVariableDeclaration newParameter = optionalizeSimpleMethodParameter(ast, oldParameter);
		
		// Replace the old parameter with the new one
		rewrite.replace(oldParameter, newParameter, null);

		importOptional(selectedNode, ast, rewrite);
		
		// Find usages of the parameter in the method
		ASTNode parentNode = oldParameter.getParent();
		if (!(parentNode instanceof MethodDeclaration)) {
			return;
		}

		MethodDeclaration method = (MethodDeclaration) parentNode;
		method.accept(new ParameterUsageHandlerVisitor(oldParameter));

		// Send the proposal
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);

		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
		
		proposals.add(proposal);
	}

	@SuppressWarnings("unchecked")
	private static void importOptional(ASTNode selectedNode, AST ast, ASTRewrite rewrite) {
		ListRewrite importRewrite = rewrite.getListRewrite(selectedNode.getRoot(), CompilationUnit.IMPORTS_PROPERTY);
		List<ImportDeclaration> imports = importRewrite.getOriginalList();

		boolean imported = false;
		for (ImportDeclaration iimport: imports) {
			if (iimport.getName().getFullyQualifiedName().contains("java.util.Optional")) {
				imported = true;
			}
		}
		
		if (!imported) {
			ImportDeclaration optionalImport = ast.newImportDeclaration();
			optionalImport.setName(ast.newName("java.util.Optional"));
			importRewrite.insertLast(optionalImport, null);
		}
	}

	private static void handleUsageOfParameter(SimpleName node) {
		ASTNode parentNode = node.getParent();
		if (parentNode instanceof InfixExpression) {
			// handleinfix 
		} else if (parentNode instanceof VariableDeclarationFragment) {
			// handlevariable
		} else if (parentNode instanceof MethodInvocation) {
			// handlemethod
		} else if (parentNode instanceof SwitchStatement) {
			// handle SwitchStatement
		} else {
			System.out.println("!!!!!!!! WARNING NEW USECASE: ");
			System.out.println(parentNode);
			System.out.println(parentNode.getClass().getName());	
		}
	}

	@SuppressWarnings("unchecked")
	private static SingleVariableDeclaration optionalizeSimpleMethodParameter(AST ast,
			SingleVariableDeclaration oldParameter) {		
		ParameterizedType optionalType = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName("Optional")));
		SimpleType oldType = (SimpleType) oldParameter.getType();
		SimpleName oldTypeName = (SimpleName) oldType.getName();
		SimpleType baseType = ast.newSimpleType(ast.newSimpleName(oldTypeName.getIdentifier()));

		optionalType.typeArguments().add(baseType);

		SingleVariableDeclaration newParameter = ast.newSingleVariableDeclaration();
		newParameter.setName(ast.newSimpleName(oldParameter.getName().getIdentifier()));
		newParameter.setType(optionalType);
	    
		return newParameter;
	}

	private static final class ParameterUsageHandlerVisitor extends ASTVisitor {
		private final SingleVariableDeclaration oldParameter;

		private ParameterUsageHandlerVisitor(SingleVariableDeclaration oldParameter) {
			this.oldParameter = oldParameter;
		}

		@Override
		public boolean visit(SimpleName node) {
			IBinding bindingVisited = node.resolveBinding();
			IBinding bindingParameter = oldParameter.getName().resolveBinding();

			if (bindingVisited.equals(bindingParameter)) {
				handleUsageOfParameter(node);
			}
			return false;
		}
	}
}
