package hu.kecskesk.custommarker.handlers;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public abstract class MarkerVisitor extends ASTVisitor {
	protected int markerCounter = 0;
	protected ICompilationUnit iCompilationUnit;
	protected CompilationUnit compilationUnit;
	
	public int getMarkerCounter() {
		return markerCounter;
	}

	public void setICompilationUnit(ICompilationUnit iCompilationUnit) {
		this.iCompilationUnit = iCompilationUnit;
	}

	public void setCompaliationUnit(CompilationUnit compilationUnit) {
		this.compilationUnit = compilationUnit;
	}
}
