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

import java.lang.reflect.InvocationTargetException;
import org.icepdf.core.util.Library;
import org.icepdf.ri.common.SwingController;

public class DocumentLoadedHelper {

    /**
     * This is just a hack to wait for the document to be unloaded. Probably there is a "normal" way which I unfortunately did not find.
     */
    public static void waitUntilDocumentLoaded(SwingController controller) throws InterruptedException, InvocationTargetException {
        do {
            sleep(100);
        } while (controller.getDocumentViewController().getDocument().getNumberOfPages() <= 0);
        while (Library.isAnActionRunning()) {
            Thread.sleep(100);
        }
        while (isIcePdfRunning()) {
            sleep(100);
        }
    }

    private static boolean isIcePdfRunning() {
        for (StackTraceElement[] value : Thread.getAllStackTraces().values()) {
            for (StackTraceElement stackTraceElement : value) {
                if (stackTraceElement.getClassName().startsWith("org.icepdf")
                    && !stackTraceElement.getClassName().equals(DocumentLoadedHelper.class.getName())
                    && !stackTraceElement.getClassName().equals(MemoryIssueViewerExample.class.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
