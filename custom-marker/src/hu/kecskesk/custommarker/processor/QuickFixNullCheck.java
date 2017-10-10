package hu.kecskesk.custommarker.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
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
import hu.kecskesk.utils.Utils;

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
		method.accept(new ParameterUsageHandlerVisitor(oldParameter, rewrite));

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
		for (ImportDeclaration iimport : imports) {
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
		private final ASTRewrite rewrite;

		private ParameterUsageHandlerVisitor(SingleVariableDeclaration oldParameter, ASTRewrite rewrite) {
			this.oldParameter = oldParameter;
			this.rewrite = rewrite;
		}

		@Override
		public boolean visit(SimpleName node) {
			IBinding bindingVisited = node.resolveBinding();
			IBinding bindingParameter = oldParameter.getName().resolveBinding();

			if (bindingVisited.equals(bindingParameter)) {
				handleUsageOfParameter(node, rewrite);
			}
			return false;
		}
	}

	private static void handleUsageOfParameter(SimpleName node, ASTRewrite rewrite) {
		ASTNode parentNode = node.getParent();
		if (parentNode instanceof InfixExpression) {
			handleInfixExpression(node, rewrite, (InfixExpression) parentNode);
		} else if (parentNode instanceof VariableDeclarationFragment || parentNode instanceof MethodInvocation
				|| parentNode instanceof SwitchStatement || parentNode instanceof ReturnStatement) {
			handleByReplaceWithGet(node, rewrite);
		} else if (parentNode instanceof Assignment) {
			handleAssignment(node, rewrite, (Assignment) parentNode);
		} else if (parentNode instanceof SingleVariableDeclaration) {
			// this is the parameter itself, we don't need to care about it
		} else {
			unknownCodePart(parentNode);
		}
	}

	private static void unknownCodePart(ASTNode parentNode) {
		System.out.println("!!!!!!!! WARNING NEW USECASE: ");
		System.out.println(parentNode);
		System.out.println(parentNode.getClass().getName());
	}

	private static void handleInfixExpression(SimpleName node, ASTRewrite rewrite, InfixExpression expression) {
		Optional<SimpleName> var = Utils.getVariableIfNullCheck(expression);
		if (var.isPresent() && var.get() == node) {

			AST ast = rewrite.getAST();
			MethodInvocation isPresentCall = ast.newMethodInvocation();
			isPresentCall.setName(ast.newSimpleName("isPresent"));
			isPresentCall.setExpression(ast.newSimpleName(node.getIdentifier()));

			if (expression.getOperator().equals(InfixExpression.Operator.EQUALS)) {
				rewrite.replace(expression, isPresentCall, null);
			} else if (expression.getOperator().equals(InfixExpression.Operator.NOT_EQUALS)) {
				PrefixExpression negationExpression = ast.newPrefixExpression();
				negationExpression.setOperator(PrefixExpression.Operator.NOT);
				negationExpression.setOperand(isPresentCall);
				rewrite.replace(expression, negationExpression, null);
			}
		} else {
			unknownCodePart(expression);
		}
	}

	private static void handleByReplaceWithGet(SimpleName node, ASTRewrite rewrite) {
		AST ast = rewrite.getAST();
		MethodInvocation getCall = ast.newMethodInvocation();
		getCall.setName(ast.newSimpleName("get"));
		getCall.setExpression(ast.newSimpleName(node.getIdentifier()));
		rewrite.replace(node, getCall, null);
	}

	@SuppressWarnings("unchecked")
	private static void handleAssignment(SimpleName node, ASTRewrite rewrite, Assignment expression) {
		if (expression.getLeftHandSide() == node) {
			AST ast = rewrite.getAST();
			Expression originalRightHandSide = expression.getRightHandSide();

			MethodInvocation ofWrapper = ast.newMethodInvocation();
			ofWrapper.setName(ast.newSimpleName("of"));
			ofWrapper.setExpression(ast.newSimpleName("Optional"));
			ofWrapper.arguments().add(rewrite.createCopyTarget(originalRightHandSide));

			rewrite.replace(originalRightHandSide, ofWrapper, null);
		} else {
			handleByReplaceWithGet(node, rewrite);
		}
	}
}
