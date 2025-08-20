package org.icepdf.examples.component;
/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

import static org.icepdf.core.pobjects.fonts.FontManager.FILE_WHITE_LISTE_PATTERN_PROPERTY;
import static org.icepdf.core.pobjects.fonts.FontManager.MOST_COMMON_FONTS_PATTERN;
import static org.icepdf.examples.component.DocumentLoadedHelper.sleep;
import static org.icepdf.ri.util.FontPropertiesManager.PREFERENCES_KEY_CLASS;

import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.icepdf.core.pobjects.fonts.FontFactory;
import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.SwingViewBuilder;
import org.icepdf.ri.common.views.annotations.acroform.TextWidgetComponent;
import org.icepdf.ri.util.FontPropertiesManager;
import org.icepdf.ri.util.ViewerPropertiesManager;

/**
 * This class started as a copy of <code>ViewerComponentExample</code>.
 */
public class MemoryIssueViewerExample {

    private static final int JUST_A_BIT = 50;
    private static final int LONG_TO_MAKE_MEMORY_SNAPSHOT = 1232100;
    private static JFrame APPLICATION_FRAME = new JFrame();
    private static SwingController CONTROLLER = new SwingController();
    private static List<PropertyChangeListener> REMAINING_LISTENERS = new ArrayList<>();

    public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {
        System.getProperties().put("org.icepdf.ri.viewer.readonly",true);
        FontPropertiesManager.getInstance().loadOrReadSystemFonts();
        final String filePath = copyResourceToTmpFile("/samples/memory-leak-creating-at-icepdf-7.3.0.pdf");
        boolean checkIndivdual = false;
        if (checkIndivdual) {
            checkDocument(filePath, true, true, false, 0);
        } else {
            for (int i = 0; i < 10; i++) {
                if (!checkDocument(filePath, true, false, true, 0)) {
                    System.out.println("There are still some Listeners in memory");
                    break;
                }
                sleep(LONG_TO_MAKE_MEMORY_SNAPSHOT);
            }
        }

        CONTROLLER.dispose();
        CONTROLLER = null;
        APPLICATION_FRAME.setVisible(false);
        APPLICATION_FRAME.dispose();
        APPLICATION_FRAME = null;
        sleep(LONG_TO_MAKE_MEMORY_SNAPSHOT);
    }

    /**
     * When running #checkDocument with waitThatDocumentIsLoaded false, then it happens sometimes that there are still listeners in PropertyChangeListeners.
     *
     * @return
     */
    private static boolean findOrgIcePdfRemainings() {
        REMAINING_LISTENERS = Arrays.stream(KeyboardFocusManager.getCurrentKeyboardFocusManager().getPropertyChangeListeners()).filter(p -> p instanceof TextWidgetComponent).toList();
        System.out.printf("Found %d keyboard focus listeners%n", REMAINING_LISTENERS.size());
        return REMAINING_LISTENERS.size() > 0;
    }

    private static String copyResourceToTmpFile(String resourcePath) throws IOException {
        try (InputStream is = MemoryIssueViewerExample.class.getResource(resourcePath).openStream()) {
            Path tempFile = Files.createTempFile("icepdf-resource", "pdf");
            Files.write(tempFile, is.readAllBytes());
            return tempFile.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return true, if the KeyListeners were removed
     */
    private static boolean checkDocument(String filePath, boolean waitThatDocumentIsLoaded, boolean useNewFontWhiteList, boolean waitForMemorySnapshot, int fontFactoryCacheSize) throws InterruptedException, InvocationTargetException {
        System.out.println("check file: " + filePath);
        Runnable runnable = () -> {
            CONTROLLER.setIsEmbeddedComponent(true);
            System.getProperties().put(FontFactory.FONT_FACTORY_CACHE_SIZE_KEY, fontFactoryCacheSize);
            if (useNewFontWhiteList) {
                // new property to reduce memoryfootprint
                System.getProperties().put(FILE_WHITE_LISTE_PATTERN_PROPERTY, MOST_COMMON_FONTS_PATTERN);
                // new property to cache the font-properties in a seperated property-file
                System.getProperties().put(PREFERENCES_KEY_CLASS, MemoryIssueViewerExample.class);
            }else{
                // new property to reduce memoryfootprint
                System.getProperties().put(FILE_WHITE_LISTE_PATTERN_PROPERTY, "");
                // new property to cache the font-properties in a seperated property-file
                System.getProperties().remove(PREFERENCES_KEY_CLASS);
            }
            FontPropertiesManager.getInstance().loadOrReadSystemFonts();

            ViewerPropertiesManager properties = ViewerPropertiesManager.getInstance();
            properties.getPreferences().putFloat(ViewerPropertiesManager.PROPERTY_DEFAULT_ZOOM_LEVEL, 1.25f);

            SwingViewBuilder factory = new SwingViewBuilder(CONTROLLER, properties);

            JPanel viewerComponentPanel = factory.buildViewerPanel();
            APPLICATION_FRAME.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            APPLICATION_FRAME.getContentPane().add(viewerComponentPanel);
            CONTROLLER.openDocument(filePath);

            APPLICATION_FRAME.addWindowListener(CONTROLLER);

            APPLICATION_FRAME.pack();
            APPLICATION_FRAME.setVisible(true);
        };
        SwingUtilities.invokeAndWait(runnable);
        if (waitThatDocumentIsLoaded) {
            DocumentLoadedHelper.waitUntilDocumentLoaded(CONTROLLER);
        } else {
            sleep(JUST_A_BIT);
        }
        if (waitForMemorySnapshot) {
            sleep(LONG_TO_MAKE_MEMORY_SNAPSHOT);
        }
        SwingUtilities.invokeAndWait(() -> {
            CONTROLLER.closeDocument();
        });
        return !findOrgIcePdfRemainings();
    }
}
