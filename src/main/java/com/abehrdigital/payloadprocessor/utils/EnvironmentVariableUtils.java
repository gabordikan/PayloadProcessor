package com.abehrdigital.payloadprocessor.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

public class EnvironmentVariableUtils {
    public static String getEnvironmentVariableReturnNullIfDoesntExist(String name) {
        String environmentVariable;

        try {
            environmentVariable = System.getenv(name);
        } catch (Exception exception) {
            LogManager.getLogger(EnvironmentVariableUtils.class).log(Level.WARN , exception);
            environmentVariable = null;
        }

        return environmentVariable;
    }
}
