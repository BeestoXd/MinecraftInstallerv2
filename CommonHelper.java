package jdk.test.lib.jfr;

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;


/**
 * Common helper class.
 */
public final class CommonHelper {

    private static RecordedEvent timeStampCounterEvent;

    public static void verifyException(VoidFunction f, String msg, Class<? extends Throwable> expectedException) throws Throwable {
        try {
            f.run();
        } catch (Throwable t) {
            if (expectedException.isAssignableFrom(t.getClass())) {
                return;
            }
            t.printStackTrace();
            assertEquals(t.getClass(), expectedException, "Wrong exception class");
        }
        fail("Missing Exception for: " + msg);
    }

    public static Recording verifyExists(long recId, List<Recording> recordings) {
        for (Recording r : recordings) {
            if (recId == r.getId()) {
                return r;
            }
        }
        Asserts.fail("Recording not found, id=" + recId);
        return null;
    }


    public static void waitForRecordingState(Recording r, RecordingState expectedState) throws Exception {
        while (r.getState() != expectedState) {
            Thread.sleep(20);
        }
    }

    public static void verifyRecordingState(Recording r, RecordingState expectedState) throws Exception {
        assertEquals(expectedState, r.getState(), "Wrong state");
    }

    public static boolean hasFastTimeEnabled() throws Exception {
        return getTimeStampCounterEvent().getValue("fastTimeEnabled");
    }

    private synchronized static RecordedEvent getTimeStampCounterEvent() throws IOException, Exception {
        if (timeStampCounterEvent == null) {
            try (Recording r = new Recording()) {
                r.enable(EventNames.CPUTimeStampCounter);
                r.start();
                r.stop();
                Path p = Utils.createTempFile("timestamo", ".jfr");
                r.dump(p);
                List<RecordedEvent> events = RecordingFile.readAllEvents(p);
                Files.deleteIfExists(p);
                if (events.isEmpty()) {
                    throw new Exception("Could not locate CPUTimeStampCounter event");
                }
                timeStampCounterEvent = events.get(0);
            }
        }
        return timeStampCounterEvent;
    }

    public static void waitForSystemCurrentMillisToChange() {
        long t = System.currentTimeMillis();
        while (t == System.currentTimeMillis()) {
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
               throw new Error("Sleep interupted", e);
            }
        }
    }
}
