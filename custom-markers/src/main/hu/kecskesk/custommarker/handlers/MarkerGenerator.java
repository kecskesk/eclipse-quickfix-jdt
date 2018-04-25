package hu.kecskesk.custommarker.handlers;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import hu.kecskesk.custommarker.Activator;
import hu.kecskesk.custommarker.handlers.markervisitors.ImmutableDetectorVisitor;
import hu.kecskesk.custommarker.handlers.markervisitors.OptionalBindingTraverserVisitor;
import hu.kecskesk.custommarker.handlers.markervisitors.OptionalVariableCollectorVisitor;
import hu.kecskesk.utils.CUParser;

public class MarkerGenerator {
	private int markerCounter;
	private StringBuilder messageBuilder;
	private CUParser cuParser;

	public MarkerGenerator() {
		messageBuilder = new StringBuilder();
		markerCounter = 0;
	}

	public void addMarkersInUnit(ICompilationUnit iCompilationUnit) {
		CompilationUnit compilationUnit = cuParser.parse();
		List<MarkerVisitor> visitors = Activator.getActiveMarkerVisitor();
		if (visitors.size() == 1) {
			MarkerVisitor packageVisitor = visitors.get(0);
			packageVisitor.setICompilationUnit(iCompilationUnit);
			packageVisitor.setCompaliationUnit(compilationUnit);

			compilationUnit.accept(packageVisitor);
			markerCounter += packageVisitor.getMarkerCounter();
		} else if (visitors.size() == 2) {
			OptionalVariableCollectorVisitor collectorVisitor = (OptionalVariableCollectorVisitor) visitors.get(0);
			OptionalBindingTraverserVisitor traverserVisitor = (OptionalBindingTraverserVisitor) visitors.get(1);

			collectorVisitor.setICompilationUnit(iCompilationUnit);
			collectorVisitor.setCompaliationUnit(compilationUnit);
			traverserVisitor.setICompilationUnit(iCompilationUnit);
			traverserVisitor.setCompaliationUnit(compilationUnit);

			compilationUnit.accept(collectorVisitor);
			traverserVisitor.setVariables(collectorVisitor.getVariables());
			compilationUnit.accept(traverserVisitor);

			markerCounter += traverserVisitor.getMarkerCounter();
		} else {
			ImmutableDetectorVisitor packageVisitor = new ImmutableDetectorVisitor();
			packageVisitor.setICompilationUnit(iCompilationUnit);
			packageVisitor.setCompaliationUnit(compilationUnit);

			compilationUnit.accept(packageVisitor);
			messageBuilder.append(packageVisitor.getMessage());
		}
	}
	
	public String getMessage() {
		if (messageBuilder.length() == 0) {
			messageBuilder.append("I have added " + markerCounter + " markers for you.");
		}
		return messageBuilder.toString();
	}
	
	public void setCuParser(CUParser cuParser) {
		this.cuParser = cuParser;
	}
}
