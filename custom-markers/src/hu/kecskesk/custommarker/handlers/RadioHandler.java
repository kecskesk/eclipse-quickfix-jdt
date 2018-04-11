package hu.kecskesk.custommarker.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.RadioState;

import hu.kecskesk.custommarker.Activator;
import hu.kecskesk.custommarker.handlers.markervisitors.AnonymusClassVisitor;
import hu.kecskesk.custommarker.handlers.markervisitors.ForEachVisitor;
import hu.kecskesk.custommarker.handlers.markervisitors.OptionalBindingTraverserVisitor;
import hu.kecskesk.custommarker.handlers.markervisitors.OptionalVariableCollectorVisitor;
import hu.kecskesk.custommarker.handlers.markervisitors.TryResourceVisitor;
import hu.kecskesk.utils.Constants;

public class RadioHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {

		if (HandlerUtil.matchesRadioState(event)) {
			return null; // we are already in the updated state - do nothing
		}

		String currentState = event.getParameter(RadioState.PARAMETER_ID);

		switch (currentState) {
			case "Immutable":
				Activator.ACTIVE_CONSTANT = Constants.IMMUTABLE_CONSTANT;
				Activator.activeMarkerVisitor = List.of();
				break;
			case "Optional": 
				Activator.ACTIVE_CONSTANT = Constants.OPTIONAL_CONSTANT;
				Activator.activeMarkerVisitor = List.of(new OptionalVariableCollectorVisitor(), 
						new OptionalBindingTraverserVisitor());
				break;
			case "Diamond Operator": 
				Activator.ACTIVE_CONSTANT = Constants.ANONYM_CONSTANT;
				Activator.activeMarkerVisitor = List.of(new AnonymusClassVisitor());
				break;
			case "Try with Resources":
				Activator.ACTIVE_CONSTANT = Constants.TRY_RES_CONSTANT; 
				Activator.activeMarkerVisitor = List.of(new TryResourceVisitor());
				break;
			case "For Each": 
				Activator.ACTIVE_CONSTANT = Constants.FOR_EACH_CONSTANT;
				Activator.activeMarkerVisitor = List.of(new ForEachVisitor());
				break;
		}
		
		// do whatever having "currentState" implies
		System.out.println(currentState);

		// and finally update the current state
		HandlerUtil.updateRadioState(event.getCommand(), currentState);

		return null;
	}

}