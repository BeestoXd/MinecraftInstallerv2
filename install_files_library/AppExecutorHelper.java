package jdk.test.lib.jfr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;


/**
 * Helper class for running applications with enabled JFR recording
 */
public class AppExecutorHelper {

    /**
     * Executes an application with enabled JFR and writes collected events
     * to the given output file.
     * Which events to track and other parameters are taken from the setting .jfc file.
     *
     * @param setting JFR settings file(optional)
     * @param jfrFilename JFR resulting recording filename(optional)
     * @param additionalVMFlags additional VM flags passed to the java(optional)
     * @param className name of the class to execute
     * @param classArguments arguments passed to the class(optional)
     * @return output analyzer for executed application
     */
    public static OutputAnalyzer executeAndRecord(String settings, String jfrFilename, String[] additionalVmFlags,
                                                  String className, String... classArguments) throws Exception {
        List<String> arguments = new ArrayList<>();
        String baseStartFlightRecording = "-XX:StartFlightRecording";
        String additionalStartFlightRecording = "";

        if (additionalVmFlags != null) {
            Collections.addAll(arguments, additionalVmFlags);
        }

        if (settings != null & jfrFilename != null) {
            additionalStartFlightRecording = String.format("=settings=%s,filename=%s", settings, jfrFilename);
        } else if (settings != null) {
            additionalStartFlightRecording = String.format("=settings=%s", settings);
        } else if (jfrFilename != null) {
            additionalStartFlightRecording = String.format("=filename=%s", jfrFilename);
        }
        arguments.add(baseStartFlightRecording + additionalStartFlightRecording);

        arguments.add(className);
        if (classArguments.length > 0) {
            Collections.addAll(arguments, classArguments);
        }

        ProcessBuilder pb = ProcessTools.createTestJvm(arguments);
        return ProcessTools.executeProcess(pb);
    }
}
