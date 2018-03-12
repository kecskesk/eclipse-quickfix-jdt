package hu.kecskesk.custommarker.handlers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarkerGeneratorTest {

	private MarkerGenerator markerGenerator;

	@BeforeEach
	void setUp() {
		markerGenerator = new MarkerGenerator();
	}

	@Test
	void test() throws ExecutionException {
		//IProject project = new IProject
		IJavaProject javaProject = JavaCore.create(project);
		IEvaluationContext context = new EvaluationContext(null, javaProject);
		context.addVariable(ISources.ACTIVE_WORKBENCH_WINDOW_NAME,
				PlatformUI.getWorkbench().getActiveWorkbenchWindow());

		// context.addVariable( ISources.ACTIVE_PART_NAME, myPart );
		context.addVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME, new StructuredSelection());
		//context.addVariable("project", value);
		Map<String, String> parameters = new HashMap<>();
		ExecutionEvent event = new ExecutionEvent(null, parameters, null, context);

		markerGenerator.execute(event);
	}

}
