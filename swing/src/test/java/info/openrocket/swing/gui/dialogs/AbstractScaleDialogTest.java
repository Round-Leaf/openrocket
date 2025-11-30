package info.openrocket.swing.gui.dialogs;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import info.openrocket.core.l10n.DebugTranslator;
import info.openrocket.core.l10n.Translator;
import info.openrocket.core.plugin.PluginModule;
import info.openrocket.core.rocketcomponent.RocketComponent;
import info.openrocket.core.startup.Application;
import info.openrocket.swing.ServicesForTesting;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Method;

import static org.mockito.Mockito.mock;

public abstract class AbstractScaleDialogTest {

    protected ScaleDialog dialogInstance;
    protected Method scaleMethod;       // For dimensions (Length, Radius, Mass)
    protected Method scaleOffsetMethod; // For positions (AxialOffset, RadialPosition)

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
        dialogInstance = mock(ScaleDialog.class);

        // 2. UNLOCK PRIVATE METHODS VIA REFLECTION
        scaleMethod = ScaleDialog.class.getDeclaredMethod("scale",
                RocketComponent.class, double.class, boolean.class);
        scaleMethod.setAccessible(true);

        scaleOffsetMethod = ScaleDialog.class.getDeclaredMethod("scaleOffset",
                RocketComponent.class, double.class, boolean.class);
        scaleOffsetMethod.setAccessible(true);
    }
}