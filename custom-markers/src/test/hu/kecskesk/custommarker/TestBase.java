package hu.kecskesk.custommarker;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.State;
import org.eclipse.ui.handlers.RadioState;
import org.junit.jupiter.api.BeforeEach;

import hu.kecskesk.custommarker.handlers.RadioHandler;

public class TestBase {
	protected ExecutionEvent radioEvent;
	protected RadioHandler radioHandler;
	protected Map<String, String> parameterMap;
	protected Command command;
	protected State commandState;
	protected String defaultState = "defaultState";
	
	@BeforeEach
	protected void setup() {
		parameterMap = new HashMap<>();
		radioHandler = new RadioHandler();	
		commandState = new State();	
		commandState.setValue(defaultState);
		command = new CommandManager().getCommand("commandID");
		command.addState(RadioState.STATE_ID, commandState);
	}
}
