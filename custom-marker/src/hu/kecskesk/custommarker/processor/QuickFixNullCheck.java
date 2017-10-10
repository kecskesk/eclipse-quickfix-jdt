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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
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
				addNullCheckProposal(context, curr, resultingCollections);
			}
		}
		return resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
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

		// 0. Make sure we grabbed a method parameter
		if (!(selectedNode instanceof SingleVariableDeclaration)) {
			return;
		}

		// 1. Replace the old parameter with the new one
		SingleVariableDeclaration oldParameter = (SingleVariableDeclaration) selectedNode;
		if (!oldParameter.getType().isSimpleType()) {
			return;
		}

		SingleVariableDeclaration newParameter = optionalizeSimpleMethodParameter(ast, oldParameter);
		rewrite.replace(oldParameter, newParameter, null);
		importOptional(selectedNode, ast, rewrite);

		// 2. Find usages of the parameter in the method
		ASTNode parentNode = oldParameter.getParent();
		if (!(parentNode instanceof MethodDeclaration)) {
			return;
		}

		MethodDeclaration method = (MethodDeclaration) parentNode;
		method.accept(new ParameterUsageHandlerVisitor(oldParameter, rewrite));

		// 3. Find usages on the method and correct its calls
		//if (Modifier.isPrivate(method.getModifiers())) {
			ASTNode typeNode = method.getParent();
			if (!(typeNode instanceof TypeDeclaration)) {
				return;
			}
			TypeDeclaration type = (TypeDeclaration) typeNode;
			type.accept(new MethodUsageHandlerVisitor(oldParameter, method, rewrite));
		//}

		// 4. Send the proposal
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
}
