package io.mudis.mudisserver.model;

public enum DataStructure {
    NONE,
    LIST,
    HASH;

    public static DataStructure from(String arg) {
        return switch (arg) {
            case "[]" -> LIST;
            case "#{}" -> HASH;
            default -> NONE;
        };
    }
}
