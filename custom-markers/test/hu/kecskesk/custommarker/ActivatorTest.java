package hu.kecskesk.custommarker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import hu.kecskesk.utils.Constants;

public class ActivatorTest {

	@BeforeEach
	void setup() {
		new Activator();
	}
	
	@Test
	void testActivatorDefaults() {
		assertEquals(Activator.ACTIVE_CONSTANT, Constants.IMMUTABLE_CONSTANT);
		assertEquals(Activator.activeMarkerVisitor, List.of());
	}
}
