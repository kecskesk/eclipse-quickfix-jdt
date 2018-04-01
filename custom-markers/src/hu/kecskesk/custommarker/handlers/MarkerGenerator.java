package hu.kecskesk.custommarker.handlers;

import java.util.List;

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
import hu.kecskesk.custommarker.handlers.markervisitors.ImmutableDetectorVisitor;
import hu.kecskesk.custommarker.handlers.markervisitors.OptionalBindingTraverserVisitor;
import hu.kecskesk.custommarker.handlers.markervisitors.OptionalVariableCollectorVisitor;
import hu.kecskesk.utils.Utils;

public class MarkerGenerator extends AbstractHandler {

	private int markerCounter;
	private StringBuilder message;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		message = new StringBuilder();
		markerCounter = 0;

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();

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

		if (message.length() == 0) {
			message.append("I have added " + markerCounter + " markers for you.");
		}
		
		MessageDialog.openInformation(window.getShell(), Activator.ACTIVE_CONSTANT.problemText, message.toString());

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
				CompilationUnit compilationUnit = Utils.parse(iCompilationUnit);
				List<MarkerVisitor> visitors = Activator.getActiveMarkerVisitor();
				if (visitors.size() == 1) {
					MarkerVisitor packageVisitor = visitors.get(0);
					packageVisitor.setICompilationUnit(iCompilationUnit);
					packageVisitor.setCompaliationUnit(compilationUnit);
					
					compilationUnit.accept(packageVisitor);
					markerCounter += packageVisitor.getMarkerCounter();
				} else if(visitors.size() == 2) {
					OptionalVariableCollectorVisitor collectorVisitor = (OptionalVariableCollectorVisitor) visitors.get(0);
					OptionalBindingTraverserVisitor traverserVisitor = (OptionalBindingTraverserVisitor) visitors.get(1);
					
					collectorVisitor.setICompilationUnit(iCompilationUnit);
					collectorVisitor.setCompaliationUnit(compilationUnit);
					traverserVisitor.setICompilationUnit(iCompilationUnit);
					traverserVisitor.setCompaliationUnit(compilationUnit);
					
					compilationUnit.accept(collectorVisitor);
					traverserVisitor.setVariables(collectorVisitor.getVariables());
					compilationUnit.accept(traverserVisitor);
					
					markerCounter += traverserVisitor.getMarkerCounter();
				} else {
					ImmutableDetectorVisitor packageVisitor = new ImmutableDetectorVisitor();
					packageVisitor.setICompilationUnit(iCompilationUnit);
					packageVisitor.setCompaliationUnit(compilationUnit);
					
					compilationUnit.accept(packageVisitor);
					message.append(packageVisitor.getMessage());
				}
			}
		}
	}
}
