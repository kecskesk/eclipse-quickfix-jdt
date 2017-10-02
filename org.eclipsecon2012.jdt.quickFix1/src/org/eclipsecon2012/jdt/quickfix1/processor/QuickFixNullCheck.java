package org.eclipsecon2012.jdt.quickfix1.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.swt.graphics.Image;

@SuppressWarnings("restriction")
public class QuickFixNullCheck implements IQuickFixProcessor {

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		System.out.println(unit.getElementName());
		System.out.println(problemId);
		
		return IProblem.PotentialNullLocalVariableReference == problemId;
	}

	@Override
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context,
			IProblemLocation[] locations) throws CoreException {
		HashSet<Integer> handledProblems= new HashSet<Integer>(locations.length);
		ArrayList<IJavaCompletionProposal> resultingCollections= new ArrayList<IJavaCompletionProposal>();
		for (int i= 0; i < locations.length; i++) {
			IProblemLocation curr= locations[i];
			Integer id= new Integer(curr.getProblemId());
			if (handledProblems.add(id)) {
				process(context, curr, resultingCollections);
			}
		}
		return resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
	}
	
	private void process(final IInvocationContext context,
			final IProblemLocation problem,
			final ArrayList<IJavaCompletionProposal> proposals)
			throws CoreException {
		final int id = problem.getProblemId();
		if (id == 0) { // no proposals for none-problem locations
			return;
		}
		switch (id) {
			// which proposal(s) do we want. Call a method to handle each proposal.
			case IProblem.PotentialNullLocalVariableReference:
				QuickFixNullCheck.addNullCheckProposal(context, problem, proposals);
				break;
			case IProblem.DereferencingNullableExpression:
				QuickFixNullCheck.addNullCheckProposal(context, problem, proposals);
				break;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void addNullCheckProposal(IInvocationContext context, IProblemLocation problem, Collection<IJavaCompletionProposal> proposals){
		ICompilationUnit cu= context.getCompilationUnit();
		ASTNode selectedNode= problem.getCoveredNode(context.getASTRoot());
		AST ast = selectedNode.getAST();
		if (ast == null) {
			return;
		}
		
		ASTRewrite rewrite = ASTRewrite.create(ast);
		String label= "Add null check to fix potential null reference";
		
		// add sanity checks and AST modifications
		if (!(selectedNode instanceof SimpleName)) {
			return;
		}
		
		// create IfStatement
		IfStatement ifStatement = ast.newIfStatement();
				
		// create condition
		InfixExpression exp = ast.newInfixExpression();
		
		// create a block and add the dereference statement into that block
		Block block = ast.newBlock();
		
		ifStatement.setExpression(exp);
		
		ifStatement.setThenStatement(block);
		
		// set the proper operands and operator of the if's condition expression
		SimpleName name = (SimpleName) selectedNode;
		exp.setOperator(InfixExpression.Operator.NOT_EQUALS);
		SimpleName operandName = ast.newSimpleName(name.getIdentifier());
		exp.setLeftOperand(operandName);
		NullLiteral nullLiteral = ast.newNullLiteral();
		exp.setRightOperand(nullLiteral);
		
		// add the earlier dereferencing statement into the if's then block
		Statement blockSt;
		ASTNode parentNode = name.getParent().getParent();
		
		for (int i = 0; i < 5 && !(parentNode instanceof Statement); i++) {
			parentNode = parentNode.getParent();
		}
		
		if (!(parentNode instanceof Statement)) {
			return;
		}
		
		blockSt = (Statement)rewrite.createMoveTarget(parentNode);
		List<Statement> blockStatements = block.statements();
		blockStatements.add(blockSt);
		
		// replace the dereferencing statement with the if statement using the 'rewrite' object.
		rewrite.replace(parentNode, ifStatement, null);
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
		
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
			
		proposals.add(proposal);
	}

}
