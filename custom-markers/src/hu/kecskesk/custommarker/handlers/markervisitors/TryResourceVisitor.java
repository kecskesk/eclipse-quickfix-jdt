package hu.kecskesk.custommarker.handlers.markervisitors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;

import hu.kecskesk.custommarker.handlers.MarkerVisitor;
import hu.kecskesk.utils.Utils;

public class TryResourceVisitor extends MarkerVisitor {
	public boolean visit(TryStatement tryStatement) {
		if (tryStatement.resources().size() == 1) {
			if (tryStatement.resources().get(0) instanceof VariableDeclarationExpression) {
				try {
					Utils.addNewMarker((VariableDeclarationExpression) tryStatement.resources().get(0), compilationUnit, cu);
					markerCounter++;
				} catch (CoreException e) {		
					e.printStackTrace();
				}
			}
		} else {
			System.out.println(tryStatement.resources());
		}
		return true;
	}
}
