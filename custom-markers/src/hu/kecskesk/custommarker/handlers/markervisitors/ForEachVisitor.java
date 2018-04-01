package hu.kecskesk.custommarker.handlers.markervisitors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.EnhancedForStatement;

import hu.kecskesk.custommarker.handlers.MarkerVisitor;
import hu.kecskesk.utils.Utils;

public class ForEachVisitor extends MarkerVisitor {
	public boolean visit(EnhancedForStatement enhancedForStatement) {
		try {
			Utils.addNewMarker(enhancedForStatement, compilationUnit, cu);
			markerCounter++;
		} catch(CoreException cEx) {
			cEx.printStackTrace();
		}
		return true;
	}
}
