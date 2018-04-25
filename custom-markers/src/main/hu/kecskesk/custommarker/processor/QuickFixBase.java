package hu.kecskesk.custommarker.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.swt.graphics.Image;

import hu.kecskesk.utils.ImageCollector;

public abstract class QuickFixBase implements IQuickFixProcessor {
	static ImageCollector imageCollector = new ImageCollector();
	
	@Override
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations)
			throws CoreException {
		HashSet<Integer> handledProblems = new HashSet<Integer>(locations.length);
		ArrayList<IJavaCompletionProposal> resultingCollections = new ArrayList<IJavaCompletionProposal>();
		for (int i = 0; i < locations.length; i++) {
			IProblemLocation curr = locations[i];
			Integer id = curr.getProblemId();
			if (handledProblems.add(id)) {
				addProposal(context, curr, resultingCollections);
			}
		}
		return resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
	}

	abstract public void addProposal(IInvocationContext context, IProblemLocation problem,
			Collection<IJavaCompletionProposal> proposals) throws JavaModelException;

	protected static ASTRewriteCorrectionProposal createProposalFromRewrite(ICompilationUnit cu, ASTRewrite rewrite,
			String label) {
		Image image = imageCollector.collect();
		ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
		return proposal;
	}
}
