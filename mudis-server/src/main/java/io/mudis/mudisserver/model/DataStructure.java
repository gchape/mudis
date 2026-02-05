package io.mudis.mudisserver.model;

public enum DataStructure {
    SET,
    QUEUE;

    public static DataStructure from(String arg) {
        return switch (arg) {
            case "#{}" -> SET;
            case "[]" -> QUEUE;
            default -> throw new IllegalStateException("Unexpected value: " + arg);
        };
    }
}
