package hu.kecskesk.custommarker.handlers.markervisitors;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

import hu.kecskesk.custommarker.handlers.MarkerVisitor;
import hu.kecskesk.utils.Constants;

public class ImmutableDetectorVisitor extends MarkerVisitor {
	private enum ResultType {
		SET, LIST, MAP
	}
	
	private final StringBuilder message = new StringBuilder();
	private static Map<ResultType, Map<String, String>> METHOD_CALLS;
	private static Map<String, String> SET_METHOD_CALLS;
	private static Map<String, String> LIST_METHOD_CALLS;
	private static Map<String, String> MAP_METHOD_CALLS;
	{
		SET_METHOD_CALLS = Map.of("Collections", "unmodifiableSet", "Arrays", "asList");
		LIST_METHOD_CALLS = Map.of("Collections", "unmodifiableList");
		MAP_METHOD_CALLS = Map.of("Collections", "unmodifiableMap");
		METHOD_CALLS = Map.of(ResultType.SET, SET_METHOD_CALLS, ResultType.LIST, LIST_METHOD_CALLS, ResultType.MAP, MAP_METHOD_CALLS);
	}
	
	
	@Override
	public boolean visit(MethodInvocation node) {
		Expression expression = node.getExpression();
		if (!(expression instanceof SimpleName)) {
			return true;
		}
		
		SimpleName expressionName = (SimpleName) expression;
		METHOD_CALLS.forEach((resultType, map)-> {
			String name = expressionName.getIdentifier();
			if (map.containsKey(name) && map.get(name).equals(node.getName().getIdentifier())) {
				foundResult(node, resultType);
			}
		});
				
		return true;
	}
	
	public StringBuilder getMessage() {
		return message;
	}	

	private void foundResult(MethodInvocation methodInvocation, ResultType resultType) {
		try {
			message.append("\nI have added a new marker: " + methodInvocation.toString());
			IMarker newMarker;
			Map<String, Object> attributes = new HashMap<String, Object>();
			attributes.put(IMarker.LOCATION, methodInvocation.toString());
			attributes.put(IMarker.MESSAGE, Constants.IMMUTABLE_CONSTANT.solutionText);
			attributes.put(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
			
			
			switch (resultType) {
			case SET:
				newMarker = iCompilationUnit.getResource().createMarker(Constants.MARKER_TYPE_SET);
				attributes.put(IJavaModelMarker.ID, Constants.JDT_PROBLEM_ID_SET);
				break;
			case LIST:
				newMarker = iCompilationUnit.getResource().createMarker(Constants.MARKER_TYPE_LIST);
				attributes.put(IJavaModelMarker.ID, Constants.JDT_PROBLEM_ID_LIST);
				break;
			case MAP:
				newMarker = iCompilationUnit.getResource().createMarker(Constants.MARKER_TYPE_MAP);
				attributes.put(IJavaModelMarker.ID, Constants.JDT_PROBLEM_ID_MAP);
				break;
			default:
				throw new UnsupportedOperationException("enums should not be other than the three predefined");
			}

			int startPosition = methodInvocation.getStartPosition();
			int length = methodInvocation.getLength();
			attributes.put(IMarker.CHAR_START, startPosition);
			attributes.put(IMarker.CHAR_END, startPosition + length);
			attributes.put(IMarker.LINE_NUMBER, compilationUnit.getLineNumber(startPosition));

			newMarker.setAttributes(attributes);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}