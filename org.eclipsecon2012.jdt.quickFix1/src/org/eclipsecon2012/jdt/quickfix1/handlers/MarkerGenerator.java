package org.eclipsecon2012.jdt.quickfix1.handlers;

import java.util.HashMap;
import java.util.Map;

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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class MarkerGenerator extends AbstractHandler {

	private static final String MY_MARKER_TYPE = "org.eclipsecon2012.jdt.quickFix1.mymarker";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		StringBuilder message = new StringBuilder();
		message.append("I have added x markers for you.");
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();
		
		for (IProject project : projects) {
			try {
				if (project.hasNature(JavaCore.NATURE_ID)) {
					IJavaProject javaProject = JavaCore.create(project);
					findErrorsInProject(javaProject, message);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		MessageDialog.openInformation(window.getShell(), "QuickFix1", message.toString());
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
			for (IJavaElement pkgFrag: children) {
				if(pkgFrag instanceof IPackageFragment) {
					findErrorsInPackage((IPackageFragment)pkgFrag, message);
				}
			}
		}
	}
	
	private void findErrorsInPackage(IPackageFragment mypackage, StringBuilder message) throws CoreException {
		if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
			for (ICompilationUnit compilationUnit : mypackage.getCompilationUnits()) {
				for (IType type : compilationUnit.getAllTypes()) {
					for (IMethod method : type.getMethods()) {
						IMarker marker = method.getUnderlyingResource().createMarker(MY_MARKER_TYPE);
						CompilationUnit cu = parse(method.getCompilationUnit());
						Map<String, Object> attributes = new HashMap<String,Object>();					
						attributes.put(IMarker.LOCATION, method.getElementName());
						attributes.put(IMarker.MESSAGE, "Test marker");
						attributes.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_ERROR));
						attributes.put(IMarker.LINE_NUMBER, cu.getLineNumber(cu.getStartPosition()) - 1);
						
						marker.setAttributes(attributes);
					}
				}
			}
		}
	}
	
	private static CompilationUnit parse(ICompilationUnit unit) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null); // parse
    }
}
