package io.mudis.mudis.model;

public enum DS {
    NONE,
    LIST,
    HASH;

    public static DS from(String arg) {
        return switch (arg) {
            case "[]" -> LIST;
            case "#{}" -> HASH;
            default -> NONE;
        };
    }
}
