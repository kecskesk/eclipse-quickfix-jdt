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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import hu.kecskesk.custommarker.Activator;
import hu.kecskesk.utils.CUParser;

public class RefactoringsHandler extends AbstractHandler {
	private MarkerGenerator markerGenerator;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();

		markerGenerator = new MarkerGenerator();
		for (IProject project : projects) {
			try {
				if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
					IJavaProject javaProject = JavaCore.create(project);
					findErrorsInProject(javaProject);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		
		MessageDialog.openInformation(window.getShell(), Activator.ACTIVE_CONSTANT.problemText, markerGenerator.getMessage());

		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.showView("org.eclipse.ui.views.AllMarkersView");
		} catch (PartInitException e) {
			e.printStackTrace();
		}

		return null;
	}

	private void findErrorsInProject(IJavaProject javaProject) throws CoreException {
		for (IPackageFragmentRoot mypackage : javaProject.getPackageFragmentRoots()) {
			findErrorsInPackageFragmentRoot(mypackage);
		}
	}

	private void findErrorsInPackageFragmentRoot(IPackageFragmentRoot pkg) throws CoreException {
		if (pkg.getKind() == IPackageFragmentRoot.K_SOURCE) {
			IJavaElement[] children = pkg.getChildren();
			for (IJavaElement pkgFrag : children) {
				if (pkgFrag instanceof IPackageFragment) {
					findErrorsInPackage((IPackageFragment) pkgFrag);
				}
			}
		}
	}

	private void findErrorsInPackage(IPackageFragment mypackage) throws CoreException {
		if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
			for (ICompilationUnit iCompilationUnit : mypackage.getCompilationUnits()) {
				markerGenerator.setCuParser(new CUParser(iCompilationUnit));
				markerGenerator.addMarkersInUnit(iCompilationUnit);
			}
		}
	}
}
