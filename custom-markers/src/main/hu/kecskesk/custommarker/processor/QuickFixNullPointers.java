package hu.kecskesk.custommarker.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
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
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import hu.kecskesk.utils.CUParser;
import hu.kecskesk.utils.Constants;

public class QuickFixNullPointers extends QuickFixBase {
	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return Constants.OPTIONAL_CONSTANT.problemId == problemId;
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
		
		// 3.1 Check inside the class
		ASTNode typeNode = method.getParent();
		if (!(typeNode instanceof TypeDeclaration)) {
			return;
		}
		TypeDeclaration type = (TypeDeclaration) typeNode;
		type.accept(new MethodUsageHandlerVisitor(oldParameter, method, rewrite, new ArrayList<>()));

		// 3.2 Check outside this class
		List<ASTRewrite> rewriteOtherClasses = new ArrayList<>();
		if (cu.getJavaProject() != null) {
			checkOtherClasses(proposals, cu, label, oldParameter, method, rewriteOtherClasses);			
		}

		// 4. Send the proposal
		proposals.add(createProposalFromRewrite(cu, rewrite, label));	
	}

	private void checkOtherClasses(Collection<IJavaCompletionProposal> proposals, ICompilationUnit cu, String label,
			SingleVariableDeclaration oldParameter, MethodDeclaration method, List<ASTRewrite> rewriteOtherClasses)
			throws JavaModelException {
		IPackageFragmentRoot[] allPackageRoots = cu.getJavaProject().getPackageFragmentRoots();
		for (IPackageFragmentRoot iPackageFragmentRoot : allPackageRoots) {
			if (iPackageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
				for (IJavaElement child : iPackageFragmentRoot.getChildren()) {
					if (child instanceof IPackageFragment) {
						IPackageFragment iPackageFragment = (IPackageFragment)child;							
						ICompilationUnit[] compUnits = iPackageFragment.getCompilationUnits();
						for (ICompilationUnit iCompilationUnit: compUnits) {
							CompilationUnit otherClass = new CUParser(iCompilationUnit).parse();
							if (!iCompilationUnit.getElementName().equals(cu.getElementName())) {	
								ASTRewrite rewriteOtherClass = ASTRewrite.create(otherClass.getAST());
								List<Integer> counter = new ArrayList<>();
								otherClass.accept(new MethodUsageHandlerVisitor(oldParameter, method, rewriteOtherClass, counter));
								
								if (!counter.isEmpty()) {
									proposals.add(createProposalFromRewrite(iCompilationUnit, rewriteOtherClass, label));
									rewriteOtherClasses.add(rewriteOtherClass);
								}
							}								
						}
					}
				}
			}
		}
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
