package org.eclipsecon2012.jdt.quickfix1.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;

public class QuickFixNullCheck implements IQuickFixProcessor {

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
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
			// 1. Which proposal(s) do we want. Call a method to handle each proposal.
			case IProblem.PotentialNullLocalVariableReference:
				addNullCheckProposal(context, problem, proposals);
				break;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void addNullCheckProposal(IInvocationContext context, IProblemLocation problem, Collection<IJavaCompletionProposal> proposals){
		ICompilationUnit cu= context.getCompilationUnit();
		ASTNode selectedNode= problem.getCoveredNode(context.getASTRoot());
		AST ast = selectedNode.getAST();
		
		ASTRewrite rewrite = ASTRewrite.create(ast);
		String label= "Add null check to potential null local variable reference";
		
		SimpleName nameAst;
		if (!(selectedNode instanceof SimpleName)) {
			return;
		} else {
			nameAst = (SimpleName)selectedNode;
		}
		
		// Create if statement that has a check and a then
		IfStatement ifStatement = ast.newIfStatement();
		
		// Create check that has selected equals null
		InfixExpression ifExpression = ast.newInfixExpression();
		
		// Create then that has the node we put check on
		Block block = ast.newBlock();
		
		ifStatement.setExpression(ifExpression);
		
		ifStatement.setThenStatement(block);
		
		ifExpression.setOperator(Operator.NOT_EQUALS);
		
		SimpleName simpleNameLeftOperand = ast.newSimpleName(nameAst.getIdentifier());
		ifExpression.setLeftOperand(simpleNameLeftOperand);
		NullLiteral nullLiteral = ast.newNullLiteral();
		ifExpression.setRightOperand(nullLiteral);
		
		Statement blockStatement = (Statement) rewrite.createMoveTarget(nameAst.getParent().getParent());
		block.statements().add(blockStatement);

		rewrite.replace(nameAst.getParent().getParent(), ifStatement, null);
		
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6);
			
		proposals.add(proposal);
	}

}
