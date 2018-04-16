package hu.kecskesk.custommarker.handlers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.State;
import org.eclipse.ui.handlers.RadioState;
import org.junit.jupiter.api.BeforeEach;

public class TestBase {
	ExecutionEvent radioEvent;
	RadioHandler radioHandler;
	Map<String, String> parameterMap;
	Command command;
	State commandState;
	String defaultState = "defaultState";
	
	@BeforeEach
	void setup() {
		parameterMap = new HashMap<>();
		radioHandler = new RadioHandler();	
		commandState = new State();	
		commandState.setValue(defaultState);
		command = new CommandManager().getCommand("commandID");
		command.addState(RadioState.STATE_ID, commandState);
	}
}
