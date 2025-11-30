package info.openrocket.swing.gui.dialogs;

import info.openrocket.core.rocketcomponent.Transition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScaleDialogTransitionTest extends AbstractScaleDialogTest {

    @Test
    public void testTransition() throws Exception {
        Transition trans = new Transition();
        trans.setLength(20.0);

        trans.setForeRadius(10.0);
        trans.setForeRadiusAutomatic(false);
        trans.setAftRadius(16.0);
        trans.setAftRadiusAutomatic(false);

        trans.setForeShoulderLength(2.0);
        trans.setAftShoulderLength(4.0);

        // --- STEP 1: Scale by 0.5 ---
        scaleMethod.invoke(dialogInstance, trans, 0.5, false);

        assertEquals(10.0, trans.getLength(), 0.001);
        assertEquals(5.0, trans.getForeRadius(), 0.001);
        assertEquals(8.0, trans.getAftRadius(), 0.001);
        assertEquals(1.0, trans.getForeShoulderLength(), 0.001);
        assertEquals(2.0, trans.getAftShoulderLength(), 0.001);

        // --- STEP 2: Scale by 1.5 ---
        scaleMethod.invoke(dialogInstance, trans, 1.5, false);

        assertEquals(15.0, trans.getLength(), 0.001);
        assertEquals(7.5, trans.getForeRadius(), 0.001);
        assertEquals(12.0, trans.getAftRadius(), 0.001);
        assertEquals(1.5, trans.getForeShoulderLength(), 0.001);
        assertEquals(3.0, trans.getAftShoulderLength(), 0.001);
    }
}