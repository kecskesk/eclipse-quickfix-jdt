package hu.kecskesk.custommarker.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.RadioState;
import org.junit.jupiter.api.Test;

import hu.kecskesk.custommarker.Activator;
import hu.kecskesk.custommarker.handlers.markervisitors.AnonymusClassVisitor;
import hu.kecskesk.custommarker.handlers.markervisitors.ForEachVisitor;
import hu.kecskesk.custommarker.handlers.markervisitors.TryResourceVisitor;
import hu.kecskesk.utils.Constants;

class RadioHandlerTest extends TestBase {
	@Test
	void testRadioHandleImmutable() throws ExecutionException {
		// Arrange
		String immutableValue = "Immutable";
		parameterMap.put(RadioState.PARAMETER_ID, immutableValue);
		
		// Act
		radioEvent = new ExecutionEvent(command, parameterMap, null, null);
		radioHandler.execute(radioEvent);
		
		// Assert
		assertEquals(Activator.ACTIVE_CONSTANT, Constants.IMMUTABLE_CONSTANT);
		assertEquals(Activator.activeMarkerVisitor.size(), 0);
	}
	
	@Test
	void testRadioHandleOptional() throws ExecutionException {
		// Arrange
		String optionalValue = "Optional";
		parameterMap.put(RadioState.PARAMETER_ID, optionalValue);

		// Act
		radioEvent = new ExecutionEvent(command, parameterMap, null, null);
		radioHandler.execute(radioEvent);

		// Assert
		assertEquals(Activator.ACTIVE_CONSTANT, Constants.OPTIONAL_CONSTANT);
		assertEquals(Activator.activeMarkerVisitor.size(), 2);
	}
	
	@Test
	void testRadioHandleForEach() throws ExecutionException {
		// Arrange
		String optionalValue = "For Each";
		parameterMap.put(RadioState.PARAMETER_ID, optionalValue);

		// Act
		radioEvent = new ExecutionEvent(command, parameterMap, null, null);
		radioHandler.execute(radioEvent);

		// Assert
		assertEquals(Activator.ACTIVE_CONSTANT, Constants.FOR_EACH_CONSTANT);
		assertTrue(Activator.activeMarkerVisitor.get(0) instanceof ForEachVisitor);
	}
	
	@Test
	void testRadioHandleTryResource() throws ExecutionException {
		// Arrange
		String optionalValue = "Try with Resources";
		parameterMap.put(RadioState.PARAMETER_ID, optionalValue);

		// Act
		radioEvent = new ExecutionEvent(command, parameterMap, null, null);
		radioHandler.execute(radioEvent);

		// Assert
		assertEquals(Activator.ACTIVE_CONSTANT, Constants.TRY_RES_CONSTANT);
		assertTrue(Activator.activeMarkerVisitor.get(0) instanceof TryResourceVisitor);
	}
	
	@Test
	void testRadioHandleDiamond() throws ExecutionException {
		// Arrange
		String optionalValue = "Diamond Operator";
		parameterMap.put(RadioState.PARAMETER_ID, optionalValue);

		// Act
		radioEvent = new ExecutionEvent(command, parameterMap, null, null);
		radioHandler.execute(radioEvent);

		// Assert
		assertEquals(Activator.ACTIVE_CONSTANT, Constants.ANONYM_CONSTANT);
		assertTrue(Activator.activeMarkerVisitor.get(0) instanceof AnonymusClassVisitor);
	}
}
