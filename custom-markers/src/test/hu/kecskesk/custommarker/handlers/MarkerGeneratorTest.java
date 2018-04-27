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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.ui.handlers.RadioState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hu.kecskesk.custommarker.Activator;
import hu.kecskesk.custommarker.TestBase;
import hu.kecskesk.utils.CUParser;
import hu.kecskesk.utils.Constants;

class MarkerGeneratorTest extends TestBase {
	MarkerGenerator markerGenerator;
	CUParser cuParser;
	CompilationUnit compilationUnit;
	AST ast;

	@BeforeEach
	protected void setup() {
		super.setup();
		new Activator();
		cuParser = mock(CUParser.class);
		markerGenerator = new MarkerGenerator();
		markerGenerator.setCuParser(cuParser);
	}
	
	@Test
	void testAddMarkersDefault() throws ExecutionException {
		testWithAst();
		
		markerGenerator.addMarkersInUnit(null);
		assertEquals("I have added 0 markers for you.", markerGenerator.getMessage());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	void testAddMarkersImmutable() throws ExecutionException, CoreException {
		switchRadioHandler("Immutable");
		testWithAst();

		TypeDeclaration typeDeclaration = ast.newTypeDeclaration();
		compilationUnit.types().add(typeDeclaration);
		
		MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
		typeDeclaration.bodyDeclarations().add(methodDeclaration);
		
		Block newBlock = ast.newBlock();
		methodDeclaration.setBody(newBlock);
		
		MethodInvocation methodInvocationList = ast.newMethodInvocation();
		methodInvocationList.setName(ast.newSimpleName("unmodifiableList"));
		methodInvocationList.setExpression(ast.newSimpleName("Collections"));
		
		MethodInvocation methodInvocationMap = ast.newMethodInvocation();
		methodInvocationMap.setName(ast.newSimpleName("unmodifiableMap"));
		methodInvocationMap.setExpression(ast.newSimpleName("Collections"));
		
		MethodInvocation methodInvocationSet = ast.newMethodInvocation();
		methodInvocationSet.setName(ast.newSimpleName("unmodifiableSet"));
		methodInvocationSet.setExpression(ast.newSimpleName("Collections"));

		newBlock.statements().add(ast.newExpressionStatement(methodInvocationList));
		newBlock.statements().add(ast.newExpressionStatement(methodInvocationMap));
		newBlock.statements().add(ast.newExpressionStatement(methodInvocationSet));
		
		ICompilationUnit iCompilationUnit = setUpMocksForMarkerType(
				Constants.MARKER_TYPE_LIST, Constants.MARKER_TYPE_MAP, Constants.MARKER_TYPE_SET);
		markerGenerator.addMarkersInUnit(iCompilationUnit);
		assertEquals("\n" + 
				"I have added a new marker: Collections.unmodifiableList()\n" + 
				"I have added a new marker: Collections.unmodifiableMap()\n" + 
				"I have added a new marker: Collections.unmodifiableSet()", markerGenerator.getMessage());
	}

	private void testWithAst() {
		ast = AST.newAST(AST.JLS9);
		compilationUnit = ast.newCompilationUnit();
		when(cuParser.parse()).thenReturn(compilationUnit);
	}
	
	@Test
	void testAddMarkersOptional() throws ExecutionException, CoreException {
		switchRadioHandler("Optional");
		char[] source = ("package bar;\r\n" + 
				"import bar.i18n.Language;\r\n" + 
				"public class Demo {\r\n" + 
				"	private static String getHelloMessage(Language language, String parameter2) {\r\n" + 
				"		if (language == null) {\r\n" + 
				"			return \"null\";\r\n" + 
				"		}\r\n" + 
				"		if (\"defaultString\" == null) {\r\n" + 
				"			return \"null\";\r\n" + 
				"		}\r\n" + 			
				"	}\r\n" + 
				"}\r\n").toCharArray();
		testWithParser(source);

		ICompilationUnit iCompilationUnit = setUpMocksForMarkerType(Constants.OPTIONAL_CONSTANT.markerType);
		markerGenerator.addMarkersInUnit(iCompilationUnit);
		assertEquals("I have added 1 markers for you.", markerGenerator.getMessage());
	}
	
	@Test
	void testAddMarkersForEach() throws ExecutionException, CoreException {
		switchRadioHandler("For Each");
		
		char[] source = ("package bar;\r\n" + 
				"public class Demo {\r\n"+ 
				"   public void testOne() {\r\n" + 
				"		for (Integer integer : numbers) {\r\n" + 
				"			useFloat(integer.floatValue());\r\n" + 
				"		}\r\n" + 
				"	}" + 
				"}\r\n").toCharArray();
		testWithParser(source);

		ICompilationUnit iCompilationUnit = setUpMocksForMarkerType(Constants.FOR_EACH_CONSTANT.markerType);
		markerGenerator.addMarkersInUnit(iCompilationUnit);
		assertEquals("I have added 1 markers for you.", markerGenerator.getMessage());
	}
	
	@Test
	void testAddMarkersAnonym() throws ExecutionException, CoreException {
		switchRadioHandler("Try with Resources");
		
		char[] source = ("package bar;\r\n" + 
				"import bar.i18n.Language;\r\n" + 
				"public class Demo {\r\n\r\n" + 
				"	public void loadDataFromDB() throws SQLException {\r\n" + 
				"	    Connection dbCon = DriverManager.getConnection(\"url\", \"user\", \"password\");\r\n" + 
				"	    try (ResultSet rs = dbCon.createStatement().executeQuery(\"select * from emp\")) {\r\n" + 
				"	        while (rs.next()) {\r\n" + 
				"	            System.out.println(\"In loadDataFromDB() =====>>>>>>>>>>>> \" + rs.getString(1));\r\n" + 
				"	        }\r\n" + 
				"	    } catch (SQLException e) {\r\n" + 
				"	        System.out.println(\"Exception occurs while reading the data from DB ->\" + e.getMessage());\r\n" + 
				"	    } finally {\r\n" + 
				"	        if (null != dbCon)\r\n" + 
				"	            dbCon.close();\r\n" + 
				"	    }\r\n" + 
				"	}"+ 
				"}\r\n").toCharArray();
		testWithParser(source);

		ICompilationUnit iCompilationUnit = setUpMocksForMarkerType(Constants.TRY_RES_CONSTANT.markerType);
		markerGenerator.addMarkersInUnit(iCompilationUnit);
		assertEquals("I have added 1 markers for you.", markerGenerator.getMessage());
	}
	
	@Test
	void testAddMarkersTryResources() throws ExecutionException, CoreException {
		switchRadioHandler("Diamond Operator");
		
		char[] source = ("package bar;\r\n" + 
				"import bar.i18n.Language;\r\n" + 
				"public class Demo {\r\n"+ 
				"  public void initTestClassWithInteger() {\r\n" + 
				"		ArrayList<Integer> my_list = new ArrayList<Integer>() {\r\n" + 
				"			private static final long serialVersionUID = 8838387908912573006L;\r\n" + 
				"			\r\n" + 
				"		};\r\n" + 
				"	}" + 
				"}\r\n").toCharArray();
		testWithParser(source);

		ICompilationUnit iCompilationUnit = setUpMocksForMarkerType(Constants.ANONYM_CONSTANT.markerType);
		markerGenerator.addMarkersInUnit(iCompilationUnit);
		assertEquals("I have added 1 markers for you.", markerGenerator.getMessage());
	}

	private void switchRadioHandler(String value) throws ExecutionException {
		parameterMap = new HashMap<>();
		parameterMap.put(RadioState.PARAMETER_ID, value);
		radioEvent = new ExecutionEvent(command, parameterMap, null, null);
		radioHandler.execute(radioEvent);
	}

	private void testWithParser(char[] source) {		
		when(cuParser.parse()).thenReturn((CompilationUnit) testParse(source));
	}

	private ICompilationUnit setUpMocksForMarkerType(String... markerTypes) throws CoreException {
		IMarker iMarker = mock(IMarker.class);
		IResource iResource = mock(IResource.class);
		for (String markerType: markerTypes) {
			when(iResource.createMarker(markerType)).thenReturn(iMarker);			
		}
		ICompilationUnit iCompilationUnit = mock(ICompilationUnit.class);
		when(iCompilationUnit.getResource()).thenReturn(iResource);
		return iCompilationUnit;
	}
}
