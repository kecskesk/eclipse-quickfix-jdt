package hu.kecskesk.custommarker.handlers.markervisitors;

import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import hu.kecskesk.custommarker.handlers.MarkerVisitor;
import hu.kecskesk.utils.Utils;

public class OptionalBindingTraverserVisitor extends MarkerVisitor { 
	private Map<MethodDeclaration, Map<SingleVariableDeclaration, Boolean>> variables;
	
	public void setVariables(Map<MethodDeclaration, Map<SingleVariableDeclaration, Boolean>> variables) {
		this.variables = variables;
	}

	public boolean visit(InfixExpression infixExpression) {
		Optional<SimpleName> variable = Utils.getVariableIfNullCheck(infixExpression);
		if (variable.isPresent()) {
			variables.forEach((method, parameterList) -> {
				parameterList.keySet().forEach((singleVariable) -> {
					IBinding binding = singleVariable.getName().resolveBinding();
					IBinding binding2 =  variable.get().resolveBinding();
					
					if (binding.equals(binding2) && !parameterList.get(singleVariable)) {
						try {
							Utils.addNewMarker(singleVariable, iCompilationUnit, compilationUnit);
							markerCounter++;
						} catch (CoreException e) {
							e.printStackTrace();
						}
						parameterList.put(singleVariable, true);
					}
				});
			});
		}
		return true;
	}
}
