package hu.kecskesk.custommarker.processor;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.junit.jupiter.api.Test;

import hu.kecskesk.custommarker.TestBase;
import hu.kecskesk.utils.ImageCollector;

class QuickFixBaseTest extends TestBase {
	QuickFixBase quickFixBase;
	IInvocationContext context;
	IProblemLocation[] locations = {};
	AST ast;
	
	@SuppressWarnings("unchecked")
	@Test
	void testProcessor() throws CoreException {
		quickFixBase = new QuickFixDiamondOperator();
		
		ImageCollector mockimageCollector = mock(ImageCollector.class);
		
		QuickFixBase.imageCollector = mockimageCollector;
		IProblemLocation mockProblem = mock(IProblemLocation.class);
		locations = new IProblemLocation[] { mockProblem };
		context = mock(IInvocationContext.class);
		when(context.getCompilationUnit()).thenReturn(mock(ICompilationUnit.class));

		ast = AST.newAST(AST.JLS9);
		Block newBlock = ast.newBlock();
		newBlock.setSourceRange(0, 4);
		 
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
	}
}
