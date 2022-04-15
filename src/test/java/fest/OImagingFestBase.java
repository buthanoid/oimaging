/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fest;

import fest.common.JmcsFestSwingJUnitTestCase;
import fr.jmmc.jmcs.Bootstrapper;
import fr.jmmc.jmcs.data.preference.CommonPreferences;
import fr.jmmc.jmcs.data.preference.PreferencesException;
import fr.jmmc.jmcs.data.preference.SessionSettingsPreferences;
import fr.jmmc.oimaging.Preferences;
import fr.jmmc.oimaging.model.IRModel;
import fr.jmmc.oimaging.model.IRModelManager;
import fr.jmmc.oitools.image.FitsImage;
import java.io.File;
import org.fest.swing.annotation.GUITest;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OImagingFestBase extends JmcsFestSwingJUnitTestCase {

    public static boolean USE_DEVMODE = false;
    public static boolean USE_DOCKER = true;
    public static boolean USE_BETA = false;

    public static int MY_DELAY = NULL_DELAY; // choose NULL_DELAY for speed and MEDIUM_DELAY for debug

    @BeforeClass
    /** FIRST method, starts the app. */
    public static void m0_init() {
        // Hack to reset LAF & ui scale:
        CommonPreferences.getInstance().resetToDefaultPreferences();

        // invoke Bootstrapper method to initialize logback now:
        Bootstrapper.getState();

        Preferences.getInstance().resetToDefaultPreferences();
        SessionSettingsPreferences.getInstance().resetToDefaultPreferences();

        try {
            CommonPreferences.getInstance().setPreference(CommonPreferences.SHOW_STARTUP_SPLASHSCREEN, false);
        } catch (PreferencesException pe) {
            logger.error("setPreference failed", pe);
        }

        defineRobotDelayBetweenEvents(MY_DELAY);

        enableTooltips(false);

        System.setProperty("oimaging.devMode", USE_DEVMODE ? "true" : "false");
        System.setProperty("RemoteExecutionMode.local", USE_DOCKER ? "true" : "false");
        System.setProperty("RemoteExecution.beta", USE_BETA ? "true" : "false");

        startApplication(fr.jmmc.oimaging.OImaging.class);
    }

    @Test
    @GUITest
    /** LAST method, exits the app. */
    public void m999_exit() {
        window.close();
    }

    public void loadOIFitsFile(File directory, File file) {
        window.button("jButtonLoadData").click();
        window.fileChooser().setCurrentDirectory(directory);
        window.fileChooser().selectFile(file);
        window.fileChooser().approve();
    }

    public void createImageDefault() {
        window.button("jButtonCreateImage").click();
        window.optionPane().okButton().click();
    }

    public IRModel getIRModel() {
        return IRModelManager.getInstance().getIRModel();
    }

    public FitsImage getInitImage() {
        return getIRModel().getSelectedInputImageHDU().getFitsImages().get(0);
    }
}
