package info.openrocket.swing.gui.dialogs;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import info.openrocket.core.l10n.DebugTranslator;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.plugin.PluginModule;
import info.openrocket.core.rocketcomponent.*;
import info.openrocket.core.startup.Application;
import info.openrocket.core.util.Coordinate;
import info.openrocket.swing.ServicesForTesting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class ScaleDialogTest {

    private ScaleDialog dialogInstance;
    private Method scaleMethod;       // For dimensions (Length, Radius, Mass)
    private Method scaleOffsetMethod; // For positions (AxialOffset, RadialPosition)

    @BeforeEach
    public void setup() throws Exception {
        com.google.inject.Module applicationModule = new ServicesForTesting();

        com.google.inject.Module pluginModule = new PluginModule();

        Module debugTranslator = new AbstractModule() {
            @Override
            protected void configure() {
                bind(Translator.class).toInstance(new DebugTranslator(null));
            }
        };

        Injector injector = Guice.createInjector(Modules.override(applicationModule).with(debugTranslator),
                pluginModule);

        Application.setInjector(injector);

        // 1. BYPASS CONSTRUCTOR & DEPENDENCIES
        // The ScaleDialog constructor requires OpenRocketDocument, which fails to initialize
        // in a test environment (static block errors).
        // Instead of calling 'new ScaleDialog(...)', we create a mock of ScaleDialog.
        // This uses Objenesis internally to create the instance without calling the constructor.
        dialogInstance = mock(ScaleDialog.class);
        // 2. UNLOCK PRIVATE METHODS VIA REFLECTION
        // Even though 'dialogInstance' is a mock, invoking a PRIVATE method via reflection
        // will execute the real code (because Mockito/ByteBuddy cannot override private methods).
        // This is perfect for us as the scale() logic is stateless and doesn't use instance fields.

        // Method signature: scale(RocketComponent c, double multiplier, boolean scaleMass)
        scaleMethod = ScaleDialog.class.getDeclaredMethod("scale",
                RocketComponent.class, double.class, boolean.class);
        scaleMethod.setAccessible(true);

        // Method signature: scaleOffset(RocketComponent c, double multiplier, boolean scaleMass)
        scaleOffsetMethod = ScaleDialog.class.getDeclaredMethod("scaleOffset",
                RocketComponent.class, double.class, boolean.class);
        scaleOffsetMethod.setAccessible(true);
    }

    // ==========================================
    // 1. RocketComponent (Testing Overrides)
    // ==========================================
    @Test
    public void testRocketComponent_Overrides() throws Exception {
        // We use BodyTube as a concrete implementation of RocketComponent
        BodyTube comp = new BodyTube();

        // Setup Overrides
        comp.setCGOverridden(true);
        comp.setOverrideCGX(10.0);

        comp.setMassOverridden(true);
        comp.setOverrideMass(5.0);

        double multiplier = 2.0;

        // Perform Scale
        // Note: AxialOffset/Overrides are technically handled in the OFFSET scaler list in the source code
        // so we must use scaleOffsetMethod for CG/Offset, but scaleMethod for Mass/Dimensions.
        // The OverrideScaler is registered in SCALERS_OFFSET.
        scaleOffsetMethod.invoke(dialogInstance, comp, multiplier, true);

        // Assert
        assertEquals(20.0, comp.getOverrideCGX(), 0.001, "Override CGX should double");
        assertEquals(40.0, comp.getOverrideMass(), 0.001, "Override Mass should scale by cube (2^3=8, 5*8=40)");
    }

    // ==========================================
    // 2. BodyComponent (Length)
    // ==========================================
    @Test
    public void testBodyComponent() throws Exception {
        BodyTube tube = new BodyTube();
        tube.setLength(100.0);

        double multiplier = 0.5; // Scale down

        scaleMethod.invoke(dialogInstance, tube, multiplier, false);

        assertEquals(50.0, tube.getLength(), 0.001, "Body length should be halved");
    }

    // ==========================================
    // 3. Nose Cone
    // ==========================================
    @Test
    public void testNoseCone() throws Exception {
        NoseCone nose = new NoseCone();
        nose.setLength(50.0);
        nose.setBaseRadius(10.0);
        nose.setBaseRadiusAutomatic(false); // Important: Disable auto or it won't scale
        nose.setShoulderLength(5.0);
        nose.setShoulderRadius(8.0);

        double multiplier = 2.0;

        scaleMethod.invoke(dialogInstance, nose, multiplier, false);

        assertEquals(100.0, nose.getLength(), 0.001);
        assertEquals(20.0, nose.getBaseRadius(), 0.001);
        assertEquals(10.0, nose.getShoulderLength(), 0.001);
        assertEquals(16.0, nose.getShoulderRadius(), 0.001);
    }

    // ==========================================
    // 4. Transition
    // ==========================================
    @Test
    public void testTransition() throws Exception {
        Transition trans = new Transition();
        trans.setLength(20.0);

        trans.setForeRadius(10.0);
        trans.setForeRadiusAutomatic(false);
        trans.setAftRadius(15.0);
        trans.setAftRadiusAutomatic(false);

        trans.setForeShoulderLength(2.0);
        trans.setAftShoulderLength(3.0);

        double multiplier = 2.0;

        scaleMethod.invoke(dialogInstance, trans, multiplier, false);

        assertEquals(40.0, trans.getLength(), 0.001);
        assertEquals(20.0, trans.getForeRadius(), 0.001);
        assertEquals(30.0, trans.getAftRadius(), 0.001);
        assertEquals(4.0, trans.getForeShoulderLength(), 0.001);
        assertEquals(6.0, trans.getAftShoulderLength(), 0.001);
    }

    // ==========================================
    // 5. Launch Lug
    // ==========================================
    @Test
    public void testLaunchLug() throws Exception {
        LaunchLug lug = new LaunchLug();
        lug.setLength(50.0);
        lug.setOuterRadius(3.0);
        lug.setThickness(0.5);

        double multiplier = 3.0;

        scaleMethod.invoke(dialogInstance, lug, multiplier, false);

        assertEquals(150.0, lug.getLength(), 0.001);
        assertEquals(9.0, lug.getOuterRadius(), 0.001);
        assertEquals(1.5, lug.getThickness(), 0.001);
    }

    // ==========================================
    // 6. TrapezoidFinSet
    // ==========================================
    @Test
    public void testTrapezoidFinSet() throws Exception {
        TrapezoidFinSet fins = new TrapezoidFinSet();
        fins.setRootChord(20.0);
        fins.setTipChord(10.0);
        fins.setHeight(10.0);
        fins.setSweep(5.0);
        fins.setThickness(1.0);

        double multiplier = 2.0;

        scaleMethod.invoke(dialogInstance, fins, multiplier, false);

        assertEquals(40.0, fins.getRootChord(), 0.001);
        assertEquals(20.0, fins.getTipChord(), 0.001);
        assertEquals(20.0, fins.getHeight(), 0.001);
        assertEquals(10.0, fins.getSweep(), 0.001);
        assertEquals(2.0, fins.getThickness(), 0.001);
    }

    // ==========================================
    // 7. FreeformFinSet
    // ==========================================
    @Test
    public void testFreeformFinSet() throws Exception {
        FreeformFinSet fins = new FreeformFinSet();

        // FreeformFinSet has a hard limit of 2.5m (SNAP_LARGER_THAN).
        // We must use values that, when scaled, stay under 2.5m.
        // Define a simple triangle shape: (0,0), (0.5,0), (0,0.5)
        Coordinate[] originalPoints = new Coordinate[] {
                new Coordinate(0,0),
                new Coordinate(0.5, 0),
                new Coordinate(0, 0.5)
        };
        fins.setPoints(originalPoints);

        double multiplier = 2.0;

        scaleMethod.invoke(dialogInstance, fins, multiplier, false);

        Coordinate[] scaledPoints = fins.getFinPoints();

        assertEquals(0.0, scaledPoints[0].x, 0.001);
        assertEquals(0.0, scaledPoints[0].y, 0.001);

        // 0.5 * 2.0 = 1.0 (which is < 2.5, so it won't clamp)
        assertEquals(1.0, scaledPoints[1].x, 0.001);
        assertEquals(0.0, scaledPoints[1].y, 0.001);

        assertEquals(0.0, scaledPoints[2].x, 0.001);
        assertEquals(1.0, scaledPoints[2].y, 0.001);
    }

    // ==========================================
    // 8. MassObject
    // ==========================================
    @Test
    public void testMassObject() throws Exception {
        // MassComponent extends MassObject
        MassComponent mass = new MassComponent();
        mass.setLength(10.0);
        mass.setRadius(2.0);
        mass.setRadiusAutomatic(false);
        mass.setRadialPosition(5.0);

        double multiplier = 2.0;

        // 1. Test Dimensions (SCALERS_NO_OFFSET)
        scaleMethod.invoke(dialogInstance, mass, multiplier, false);
        assertEquals(20.0, mass.getLength(), 0.001);
        assertEquals(4.0, mass.getRadius(), 0.001);

        // 2. Test Radial Position (SCALERS_OFFSET)
        // RadialPosition is stored in the "Offset" scalers map in the source code
        scaleOffsetMethod.invoke(dialogInstance, mass, multiplier, false);
        assertEquals(10.0, mass.getRadialPosition(), 0.001, "Radial position should double");
    }

    // ==========================================
    // 9. Parachute
    // ==========================================
    @Test
    public void testParachute() throws Exception {
        Parachute chute = new Parachute();
        chute.setDiameter(50.0);
        chute.setLineLength(40.0);

        double multiplier = 0.1; // Scale way down

        scaleMethod.invoke(dialogInstance, chute, multiplier, false);

        assertEquals(5.0, chute.getDiameter(), 0.001);
        assertEquals(4.0, chute.getLineLength(), 0.001);
    }

    // ==========================================
    // 10. ShockCord
    // ==========================================
    @Test
    public void testShockCord() throws Exception {
        ShockCord cord = new ShockCord();
        cord.setCordLength(100.0);

        double multiplier = 1.5;

        scaleMethod.invoke(dialogInstance, cord, multiplier, false);

        assertEquals(150.0, cord.getCordLength(), 0.001);
    }

    // ==========================================
    // 11. InnerTube
    // ==========================================
    @Test
    public void testInnerTube() throws Exception {
        InnerTube tube = new InnerTube();

        // Test Motor Overhang (SCALERS_NO_OFFSET)
        tube.setMotorOverhang(5.0);

        // Test Axial Offset (SCALERS_OFFSET - inherited from RocketComponent)
        tube.setAxialOffset(10.0);

        double multiplier = 2.0;

        // Scale Dimensions
        scaleMethod.invoke(dialogInstance, tube, multiplier, false);
        assertEquals(10.0, tube.getMotorOverhang(), 0.001);

        // Scale Offsets
        scaleOffsetMethod.invoke(dialogInstance, tube, multiplier, false);
        assertEquals(20.0, tube.getAxialOffset(), 0.001);
    }
}