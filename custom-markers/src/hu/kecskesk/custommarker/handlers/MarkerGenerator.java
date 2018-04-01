package hu.kecskesk.custommarker.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import hu.kecskesk.custommarker.Activator;
import hu.kecskesk.utils.Utils;

public class MarkerGenerator extends AbstractHandler {

	private int markerCounter;
	
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
		
		MessageDialog.openInformation(window.getShell(), Activator.ACTIVE_CONSTANT.problemText, message.toString());

		try {
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
				CompilationUnit cu = Utils.parse(compilationUnit);
				MarkerVisitor packageVisitor = Activator.getActiveMarkerVisitor();
				packageVisitor.setCompilationUnit(compilationUnit);
				packageVisitor.setCu(cu);
				
				cu.accept(packageVisitor);
				markerCounter += packageVisitor.getMarkerCounter();
			}
		}
	}
}
