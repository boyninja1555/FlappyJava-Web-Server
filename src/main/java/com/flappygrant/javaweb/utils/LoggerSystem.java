package com.flappygrant.javaweb.utils;

public class LoggerSystem {
    private String iAm;
    private String logPrefixPrefix;
    private String logPrefixSuffix;

    public LoggerSystem(String whoAmI) {
        // Initializes dynamic variables
        iAm = whoAmI;
        logPrefixPrefix = "[" + iAm.toUpperCase() + "/";
        logPrefixSuffix = "] ";
    }

    // Logs information (formatted)
    public void info(String message) {
        log("info", message);
    }

    // Logs warnings (formatted)
    public void warn(String message) {
        log("warn", message);
    }

    // Logs errors (formatted)
    public void error(String message) {
        log("error", message);
    }

    // Logs debug information (formatted)
    public void debug(String message) {
        log("debug", message);
    }

    // Logs a message with a specific log level (formatted & expandable)
    private void log(String level, String message) {
        System.out.println(logPrefixPrefix + level.toUpperCase() + logPrefixSuffix + message);
    }
}
