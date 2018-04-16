package hu.kecskesk.custommarker.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.HashMap;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.ui.handlers.RadioState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.stubbing.answers.DoesNothing;

import hu.kecskesk.custommarker.Activator;
import hu.kecskesk.utils.CUParser;
import hu.kecskesk.utils.Constants;

class MarkerGeneratorTest extends TestBase {
	MarkerGenerator markerGenerator;
	CUParser cuParser;
	CompilationUnit compilationUnit;
	AST ast;
		
	@BeforeEach
	void setup() {
		super.setup();
		new Activator();
		cuParser = mock(CUParser.class);
		ast = AST.newAST(AST.JLS9);
		compilationUnit = ast.newCompilationUnit();
		when(cuParser.parse()).thenReturn(compilationUnit);
		markerGenerator = new MarkerGenerator();
		markerGenerator.setCuParser(cuParser);
	}
	
	@Test
	void testAddMarkersDefault() throws ExecutionException {
		markerGenerator.addMarkersInUnit(null);
		assertEquals("I have added 0 markers for you.", markerGenerator.getMessage());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	void testAddMarkersImmutable() throws ExecutionException, CoreException {
		String immutableValue = "Immutable";
		parameterMap = new HashMap<>();
		parameterMap.put(RadioState.PARAMETER_ID, immutableValue);
		radioEvent = new ExecutionEvent(command, parameterMap, null, null);
		radioHandler.execute(radioEvent);

		TypeDeclaration typeDeclaration = ast.newTypeDeclaration();
		compilationUnit.types().add(typeDeclaration);
		
		MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
		typeDeclaration.bodyDeclarations().add(methodDeclaration);
		
		Block newBlock = ast.newBlock();
		methodDeclaration.setBody(newBlock);
		
		MethodInvocation methodInvocation = ast.newMethodInvocation();
		methodInvocation.setName(ast.newSimpleName("unmodifiableList"));
		methodInvocation.setExpression(ast.newSimpleName("Collections"));

		ExpressionStatement expressionStatement = ast.newExpressionStatement(methodInvocation);
		newBlock.statements().add(expressionStatement);
		
		IMarker iMarker = mock(IMarker.class);
		IResource iResource = mock(IResource.class);
		when(iResource.createMarker(Constants.MARKER_TYPE_LIST)).thenReturn(iMarker);
		ICompilationUnit iCompilationUnit = mock(ICompilationUnit.class);
		when(iCompilationUnit.getResource()).thenReturn(iResource);
		
		markerGenerator.addMarkersInUnit(iCompilationUnit);
		assertEquals("\nI have added a new marker: Collections.unmodifiableList()", markerGenerator.getMessage());
	}
}
