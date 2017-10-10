package hu.kecskesk.custommarker.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import hu.kecskesk.utils.Utils;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class MarkerGenerator extends AbstractHandler {

	private static final String MY_MARKER_TYPE = "hu.kecskesk.custommarker.mymarker";
	private int markerCounter;
	public static final int MY_JDT_PROBLEM_ID = 1234;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		StringBuilder message = new StringBuilder();
		markerCounter = 0;

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();

		for (IProject project : projects) {
			try {
				if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
					IJavaProject javaProject = JavaCore.create(project);
					findErrorsInProject(javaProject, message);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		message.append("I have added " + markerCounter + " markers for you.");

		MessageDialog.openInformation(window.getShell(), "QuickFix1", message.toString());

		try {
			// PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.eclipse.ui.views.ProblemView");
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.showView("org.eclipse.ui.views.AllMarkersView");
		} catch (PartInitException e) {
			e.printStackTrace();
		}

		return null;
	}

	private void findErrorsInProject(IJavaProject javaProject, StringBuilder message) throws CoreException {
		for (IPackageFragmentRoot mypackage : javaProject.getPackageFragmentRoots()) {
			findErrorsInPackageFragmentRoot(mypackage, message);
		}
	}

	private void findErrorsInPackageFragmentRoot(IPackageFragmentRoot pkg, StringBuilder message) throws CoreException {
		if (pkg.getKind() == IPackageFragmentRoot.K_SOURCE) {
			IJavaElement[] children = pkg.getChildren();
			for (IJavaElement pkgFrag : children) {
				if (pkgFrag instanceof IPackageFragment) {
					findErrorsInPackage((IPackageFragment) pkgFrag, message);
				}
			}
		}
	}

	private void findErrorsInPackage(IPackageFragment mypackage, StringBuilder message) throws CoreException {
		if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
			for (ICompilationUnit compilationUnit : mypackage.getCompilationUnits()) {
				CompilationUnit cu = parse(compilationUnit);
				Map<MethodDeclaration, Map<SingleVariableDeclaration, Boolean>> variables = new HashMap<>();

				cu.accept(new ASTVisitor() {
					@SuppressWarnings("unchecked")
					public boolean visit(MethodDeclaration methodDeclaration) {
						Map<SingleVariableDeclaration, Boolean> parameterAdded =  new HashMap<>();
						methodDeclaration.parameters().forEach(parameter -> parameterAdded.put((SingleVariableDeclaration) parameter, false)); 
						variables.put(methodDeclaration, parameterAdded);
						return false;
					}
				});

				cu.accept(new ASTVisitor() {
					public boolean visit(InfixExpression infixExpression) {
						Optional<SimpleName> variable = Utils.getVariableIfNullCheck(infixExpression);
						if (variable.isPresent()) {
							variables.forEach((method, parameterList) -> {
								parameterList.keySet().forEach((singleVariable) -> {
									IBinding binding = singleVariable.getName().resolveBinding();
									IBinding binding2 =  variable.get().resolveBinding();
									
									if (binding.equals(binding2) && !parameterList.get(singleVariable)) {
										foundNullCheck(singleVariable, compilationUnit, cu, method);
										parameterList.put(singleVariable, true);
									}
								});
							});
						}
						return true;
					}
				});
			}
		}
	}

	private void foundNullCheck(SingleVariableDeclaration variable, ICompilationUnit compilationUnit,
			CompilationUnit cu, MethodDeclaration method) {
		try {
			IMarker nullMarker = compilationUnit.getResource().createMarker(MY_MARKER_TYPE);
			markerCounter++;

			Map<String, Object> attributes = new HashMap<String, Object>();
			attributes.put(IMarker.LOCATION, variable.toString());
			attributes.put(IMarker.MESSAGE, "In this method you could replace your nullable parameter with the use of Optional");
			attributes.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_WARNING));
			attributes.put(IJavaModelMarker.ID, MY_JDT_PROBLEM_ID);

			int startPosition = variable.getStartPosition();
			int length = variable.getLength();
			attributes.put(IMarker.CHAR_START, startPosition);
			attributes.put(IMarker.CHAR_END, startPosition + length);
			attributes.put(IMarker.LINE_NUMBER, cu.getLineNumber(startPosition));

			nullMarker.setAttributes(attributes);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}
}
