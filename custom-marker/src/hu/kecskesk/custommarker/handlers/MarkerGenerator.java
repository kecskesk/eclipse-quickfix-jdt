package hu.kecskesk.custommarker.handlers;

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
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
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

	private static final String MY_MARKER_TYPE = "hu.kecskesk.custommarker.mymarker";
	private static final int MY_JDT_PROBLEM_ID = 1234;
	
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
						IMarker marker = method.getResource().createMarker(MY_MARKER_TYPE);
						CompilationUnit cu = parse(method.getCompilationUnit());
						Map<String, Object> attributes = new HashMap<String,Object>();					
						attributes.put(IMarker.LOCATION, method.getElementName());
						attributes.put(IMarker.MESSAGE, "Test marker -> class: " + type.getElementName() + " method: " + method.getElementName() + " .");
						attributes.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_ERROR));
						attributes.put(IJavaModelMarker.ID, MY_JDT_PROBLEM_ID);
						
						setPositionFinder(method.getElementName(), type, attributes, cu);
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
        return (CompilationUnit) parser.createAST(null);
    }
	
	private void setPositionFinder(String name, IType type, Map<String, Object> attributes, CompilationUnit cu) throws JavaModelException
	{
	    ICompilationUnit unit = type.getCompilationUnit();
	    ASTParser parser = ASTParser.newParser(AST.JLS8);
	    parser.setSource(unit);
	    parser.setResolveBindings(true);
	    CompilationUnit cunit = (CompilationUnit) parser.createAST(null);

	    cunit.accept(new ASTVisitor() {

			public boolean visit(MethodDeclaration methodDeclaration)
	        {
	            String methodName = methodDeclaration.getName().toString();
	            //System.out.println(methodName);
	            if (methodName.equals(name))
	            {
	                int startPosition = methodDeclaration.getStartPosition();
	                int length = methodDeclaration.getLength();
	   
            		// Marks the line of the method 
	                // Underlines the whole method character-precise
	                attributes.put(IMarker.CHAR_START, startPosition);
	                attributes.put(IMarker.CHAR_END, startPosition + length);
	                attributes.put(IMarker.LINE_NUMBER, cu.getLineNumber(startPosition));
	            }
	            return false;
	        }
	    });
	}
}
