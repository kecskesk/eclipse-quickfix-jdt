package hu.kecskesk.custommarker;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import hu.kecskesk.custommarker.handlers.MarkerVisitor;
import hu.kecskesk.custommarker.handlers.markervisitors.ForEachVisitor;
import hu.kecskesk.utils.Constant;
import hu.kecskesk.utils.Constants;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "hu.kecskesk.custommarker"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	// public static final Constant ACTIVE_CONSTANT = Constants.ANONYM_CONSTANT;
	public static final Constant ACTIVE_CONSTANT = Constants.FOR_EACH_CONSTANT;
	
	public static MarkerVisitor getActiveMarkerVisitor() {
		// return new AnonymusClassVisitor();
		return new ForEachVisitor();
	}
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
