package hu.kecskesk.custommarker.handlers.markervisitors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ParameterizedType;

import hu.kecskesk.custommarker.handlers.MarkerVisitor;
import hu.kecskesk.utils.Utils;
	
public class AnonymusClassVisitor extends MarkerVisitor {
	public boolean visit(ParameterizedType parameterizedType) {
		if (!parameterizedType.typeArguments().isEmpty()) {
			if (parameterizedType.getParent() instanceof ClassInstanceCreation) {
				ClassInstanceCreation classCreation = (ClassInstanceCreation) parameterizedType.getParent();
				if (classCreation.getAnonymousClassDeclaration() != null) {
					try {
						Utils.addNewMarker(parameterizedType, compilationUnit, cu);
						markerCounter++;
					} catch(CoreException cEx) {
						cEx.printStackTrace();
					}
				}
			}
		}
		return true;
	}
	
}
