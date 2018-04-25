package hu.kecskesk.utils;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.swt.graphics.Image;

@SuppressWarnings("restriction")
public class ImageCollector {

	public Image collect() {
		return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
	}
}
