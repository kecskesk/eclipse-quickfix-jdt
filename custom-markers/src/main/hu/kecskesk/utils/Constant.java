package hu.kecskesk.utils;

public class Constant {
	public String markerType;
	public int problemId;
	public String problemText;
	public String solutionText;
	public String quickFixLabel;
	
	public Constant(String markerType, int problemId, String problemText, String solutionText, String quickFixLabel) {
		super();
		this.markerType = markerType;
		this.problemId = problemId;
		this.problemText = problemText;
		this.solutionText = solutionText;
		this.quickFixLabel = quickFixLabel;
	}	
}
