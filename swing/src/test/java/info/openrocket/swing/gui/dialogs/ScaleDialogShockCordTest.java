package info.openrocket.swing.gui.dialogs;

import info.openrocket.core.rocketcomponent.ShockCord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScaleDialogShockCordTest extends AbstractScaleDialogTest {

    @Test
    public void testShockCord() throws Exception {
        ShockCord cord = new ShockCord();
        cord.setCordLength(100.0);

        // --- STEP 1: Scale by 0.5 ---
        scaleMethod.invoke(dialogInstance, cord, 0.5, false);
        assertEquals(50.0, cord.getCordLength(), 0.001);

        // --- STEP 2: Scale by 1.5 ---
        scaleMethod.invoke(dialogInstance, cord, 1.5, false);
        assertEquals(75.0, cord.getCordLength(), 0.001);
    }
}