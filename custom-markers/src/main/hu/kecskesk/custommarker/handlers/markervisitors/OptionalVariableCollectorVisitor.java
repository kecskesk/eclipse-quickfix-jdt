package hu.kecskesk.custommarker.handlers.markervisitors;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import hu.kecskesk.custommarker.handlers.MarkerVisitor;

public class OptionalVariableCollectorVisitor extends MarkerVisitor {
	private Map<MethodDeclaration, Map<SingleVariableDeclaration, Boolean>> variables = new HashMap<>();
	
	@SuppressWarnings("unchecked")
	public boolean visit(MethodDeclaration methodDeclaration) {
		Map<SingleVariableDeclaration, Boolean> parameterAdded = new HashMap<>();
		methodDeclaration.parameters().forEach(parameter -> parameterAdded.put((SingleVariableDeclaration) parameter, false)); 
		variables.put(methodDeclaration, parameterAdded);
		return false;
	}
	
	public Map<MethodDeclaration, Map<SingleVariableDeclaration, Boolean>> getVariables() {
		return variables;
	}
}
