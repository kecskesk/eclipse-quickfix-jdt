package hu.kecskesk.custommarker.processor;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hu.kecskesk.custommarker.TestBase;
import hu.kecskesk.utils.ImageCollector;

class QuickFixTest extends TestBase {
	QuickFixBase quickFixBase;
	IInvocationContext context;
	IProblemLocation[] locations = {};
	AST ast;
	IProblemLocation mockProblem;
		
	// https://stackoverflow.com/questions/19449040/modification-with-astrewrite-not-recursive
	
	/* ASTRewrite.createCopyTarget(...) creates only a placeholder node. 
	 * The actual copy of the expression is not created until you call ASTRewrite.rewriteAST(). 
	 * This is why you see new MISSING() when you inspect fragment.
	 * If you want to force an immediate copy then you have to use ASTNode.copySubtree(AST target, ASTNode node). */
	
	@BeforeEach
	public void setup() {
		ImageCollector mockimageCollector = mock(ImageCollector.class);
		QuickFixBase.imageCollector = mockimageCollector;
		mockProblem = mock(IProblemLocation.class);
		locations = new IProblemLocation[] { mockProblem };
		
		context = mock(IInvocationContext.class);
		when(context.getCompilationUnit()).thenReturn(mock(ICompilationUnit.class));

		ast = AST.newAST(AST.JLS9);
	}

	@SuppressWarnings("unchecked")
	@Test
	void diamondProcessorTest() throws CoreException {		
		quickFixBase = new QuickFixDiamondOperator(); 
		
		SimpleName typeName = ast.newSimpleName("typeName");
		typeName.setSourceRange(1, 1);
		
		SimpleName parameterName = ast.newSimpleName("parameterName");
		typeName.setSourceRange(2, 1);
		
		SimpleType simpleType = ast.newSimpleType(typeName);
		simpleType.setSourceRange(0, 2);
		
		SimpleType parameterType = ast.newSimpleType(parameterName);
		simpleType.setSourceRange(2, 2);
		
		ParameterizedType selectedNode = ast.newParameterizedType(simpleType);
		selectedNode.setFlags(ASTNode.PARAMETERIZED_TYPE);
		selectedNode.typeArguments().add(parameterType);
		selectedNode.setSourceRange(0, 3);
		
		ast.newClassInstanceCreation().setType(selectedNode);
		
		when(mockProblem.getCoveredNode(any())).thenReturn(selectedNode);
		
		IJavaCompletionProposal[] corrections = quickFixBase.getCorrections(context, locations);	
		
		assertEquals(1, corrections.length);
		assertEquals(6, corrections[0].getRelevance());
		
		ASTRewriteCorrectionProposal proposal = (ASTRewriteCorrectionProposal) corrections[0];
		
		assertEquals("Remove redundant type parameter", proposal.getName());
	}

	@Test
	void immutableProcessorTestList1() throws CoreException {		 
		immutableTestWithMethod("Arrays", "asList");
	}

	@Test
	void immutableProcessorTestList2() throws CoreException {		 
		immutableTestWithMethod("Collections", "unmodifiableList");
	}

	@Test
	void immutableProcessorTestMap() throws CoreException {		 
		immutableTestWithMethod("Collections", "unmodifiableMap");
	}

	@Test
	void immutableProcessorTestSet() throws CoreException {		 
		immutableTestWithMethod("Collections", "unmodifiableSet");
	}

	@SuppressWarnings("unchecked")
	@Test
	void forEachProcessorTest() throws CoreException {		 
		quickFixBase = new QuickFixStreamForEach();
				
		EnhancedForStatement selectedNode = ast.newEnhancedForStatement();
		selectedNode.setSourceRange(0, 0);
		
		SingleVariableDeclaration singleVariableDeclaration = ast.newSingleVariableDeclaration();
		selectedNode.setSourceRange(0, 0);
		
		SimpleName variableName = ast.newSimpleName("forEachVariableName");
		variableName.setSourceRange(0, 0);
		
		singleVariableDeclaration.setName(variableName);
		selectedNode.setParameter(singleVariableDeclaration);
		
		MethodInvocation bodyExpression = ast.newMethodInvocation();
		bodyExpression.setSourceRange(0, 0);
		
		bodyExpression.setName(ast.newSimpleName("a"));
		bodyExpression.setExpression(ast.newSimpleName("b"));
		
		Statement bodyStatement = ast.newExpressionStatement(bodyExpression);
		bodyStatement.setSourceRange(0, 0);
		
		Block tryBlock = ast.newBlock();
		tryBlock.setSourceRange(0, 0);
		tryBlock.statements().add(bodyStatement);
		selectedNode.setBody(tryBlock);
		
		MethodInvocation tryExpression = ast.newMethodInvocation();
		tryExpression.setSourceRange(0, 0);
		tryExpression.setName(ast.newSimpleName("c"));
		tryExpression.setExpression(ast.newSimpleName("d"));
		selectedNode.setExpression(tryExpression);
		
		ast.newBlock().statements().add(selectedNode);
		
		when(mockProblem.getCoveredNode(any())).thenReturn(selectedNode);
		
		IJavaCompletionProposal[] corrections = quickFixBase.getCorrections(context, locations);	
		
		assertEquals(1, corrections.length);
		assertEquals(6, corrections[0].getRelevance());
		
		ASTRewriteCorrectionProposal proposal = (ASTRewriteCorrectionProposal) corrections[0];
		
		assertEquals("Use Stream API", proposal.getName());
	}

	@SuppressWarnings("unchecked")
	@Test
	void tryResourcesProcessorTest() throws CoreException {		 
		quickFixBase = new QuickFixTryResources();

		TryStatement tryStatement = ast.newTryStatement();
		tryStatement.setSourceRange(0, 0);
		
		VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		
		Expression baseExpression = ast.newSimpleName("forEachVariable");
		baseExpression.setSourceRange(0, 0);
		
		fragment.setInitializer(baseExpression);
		fragment.setSourceRange(0, 0);
		
		VariableDeclarationExpression selectedNode = ast.newVariableDeclarationExpression(fragment);
		selectedNode.setSourceRange(0, 0);
		
		Block tryBlock = ast.newBlock();
		tryBlock.setSourceRange(0, 0);
		
		tryStatement.setBody(tryBlock);
		
		CatchClause tryCatchClause = ast.newCatchClause();
		tryCatchClause.setSourceRange(0, 0);
		
		tryStatement.catchClauses().add(tryCatchClause);
		tryStatement.resources().add(selectedNode);
		
		ast.newBlock().statements().add(tryStatement);
		
		when(mockProblem.getCoveredNode(any())).thenReturn(selectedNode);
		
		IJavaCompletionProposal[] corrections = quickFixBase.getCorrections(context, locations);	
		
		assertEquals(1, corrections.length);
		assertEquals(6, corrections[0].getRelevance());
		
		ASTRewriteCorrectionProposal proposal = (ASTRewriteCorrectionProposal) corrections[0];
		
		assertEquals("Use the new try resource technique", proposal.getName());
	}

	@SuppressWarnings("unchecked")
	@Test
	void nullPointerProcessorTest() throws CoreException {		 
		quickFixBase = new QuickFixNullPointers();
		
		SimpleName nodeTypeName = ast.newSimpleName("NodeType");
		nodeTypeName.setSourceRange(0, 0);
		
		SimpleType nodeType = ast.newSimpleType(nodeTypeName);
		nodeType.setSourceRange(0, 0);
		
		SimpleName variableName = ast.newSimpleName("variableName");
		variableName.setSourceRange(0, 0);

		SingleVariableDeclaration selectedNode = ast.newSingleVariableDeclaration();
		selectedNode.setSourceRange(0, 0);
		selectedNode.setType(nodeType);
		selectedNode.setName(variableName);
		
		MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
		methodDeclaration.parameters().add(selectedNode);
		methodDeclaration.setSourceRange(0, 0);
		
		TypeDeclaration typeDeclaration = ast.newTypeDeclaration();
		typeDeclaration.setSourceRange(0, 0);
		typeDeclaration.bodyDeclarations().add(methodDeclaration);
		
		CompilationUnit cu = ast.newCompilationUnit();
		cu.types().add(typeDeclaration);
		
		when(mockProblem.getCoveredNode(any())).thenReturn(selectedNode);
		
		IJavaCompletionProposal[] corrections = quickFixBase.getCorrections(context, locations);	
		
		assertEquals(1, corrections.length);
		assertEquals(6, corrections[0].getRelevance());
		
		ASTRewriteCorrectionProposal proposal = (ASTRewriteCorrectionProposal) corrections[0];
		
		assertEquals("Use the new try resource technique", proposal.getName());
	}

	@SuppressWarnings("unchecked")
	private void immutableTestWithMethod(String expression, String name) throws CoreException {
		quickFixBase = new QuickFixImmutables();
		
		SimpleName methodName = ast.newSimpleName(name);
		methodName.setSourceRange(1, 1);
		
		SimpleName methodExpressionName = ast.newSimpleName(expression);
		methodExpressionName.setSourceRange(2, 1);
		
		MethodInvocation selectedNode = ast.newMethodInvocation();
		selectedNode.setName(methodName);
		selectedNode.setExpression(methodExpressionName);
		 
		ast.newBlock().statements().add(ast.newExpressionStatement(selectedNode));
		
		when(mockProblem.getCoveredNode(any())).thenReturn(selectedNode);
		
		IJavaCompletionProposal[] corrections = quickFixBase.getCorrections(context, locations);	
		
		assertEquals(1, corrections.length);
		assertEquals(6, corrections[0].getRelevance());
		
		ASTRewriteCorrectionProposal proposal = (ASTRewriteCorrectionProposal) corrections[0];
		
		assertEquals("Use factory method for immutable collections", proposal.getName());
	}	
}
