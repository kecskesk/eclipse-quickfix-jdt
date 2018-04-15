package hu.kecskesk.custommarker.handlers;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import hu.kecskesk.custommarker.Activator;
import hu.kecskesk.utils.CUParser;

class MarkerGeneratorTest {
	MarkerGenerator markerGenerator;
	CUParser cuParser;
	CompilationUnit compilationUnit;
	
	
	@BeforeEach
	void setup() {
		new Activator();
		cuParser = mock(CUParser.class);
		compilationUnit = AST.newAST(AST.JLS9).newCompilationUnit();
		when(cuParser.parse()).thenReturn(compilationUnit);
		markerGenerator = new MarkerGenerator();
		markerGenerator.setCuParser(cuParser);
	}
	
	@Test
	void testAddMarkersInUnit() throws ExecutionException {
		markerGenerator.addMarkersInUnit(null);
		assertEquals("I have added 0 markers for you.", markerGenerator.getMessage());
	}
}
