package hu.kecskesk.utils;

public class Constants {
	
	public static final Constant FOR_EACH_CONSTANT = new Constant(
			"hu.kecskesk.custommarker.foreachmarker", 
			1234, 
			"forEach method quick fix", 
			"At this line you could replace a for each cycle with Stream API",
			"Use Stream API");
	
	public static final Constant ANONYM_CONSTANT = new Constant(
			"hu.kecskesk.custommarker.diamondmarker",
			1235,
			"Anonym inner class quick fix", 
			"At this line you could remove the type parameter and use diamond operator",
			"Remove redundant type parameter");
	
	public static final Constant TRY_RES_CONSTANT = new Constant(
			"hu.kecskesk.custommarker.tryresourcemarker",
			1236,
			"Try resources quick fix", 
			"In this method you could use the new try resource technique",
			"Use the new try resource technique");
	
	public static final Constant OPTIONAL_CONSTANT = new Constant(
			"hu.kecskesk.custommarker.optionalmarker",
			1237,
			"Optional quick fix", 
			"In this method you could replace your nullable parameter with the use of Optional",
			"Use Optional class instead of nullable parameter");
}
