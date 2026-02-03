package io.mudis.mudis.model;

public enum Ds {
    NONE,
    LIST,
    HASH;

    public static Ds from(String arg) {
        return switch (arg) {
            case "[]" -> LIST;
            case "#{}" -> HASH;
            default -> NONE;
        };
    }
}
