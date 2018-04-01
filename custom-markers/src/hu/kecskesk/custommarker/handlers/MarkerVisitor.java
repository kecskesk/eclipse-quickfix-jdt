package hu.kecskesk.custommarker.handlers;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public abstract class MarkerVisitor extends ASTVisitor {
	protected int markerCounter = 0;
	protected ICompilationUnit compilationUnit;
	protected CompilationUnit cu;
	
	public int getMarkerCounter() {
		return markerCounter;
	}

	public void setCompilationUnit(ICompilationUnit compilationUnit) {
		this.compilationUnit = compilationUnit;
	}

	public void setCu(CompilationUnit cu) {
		this.cu = cu;
	}
}
