package io.mudis.mudisclient.model;

public enum Operation {
    SHOW,
    PUBLISH,
    SUBSCRIBE,
    UNSUBSCRIBE;

    public static String asString() {
        var ops = values();
        var sb = new StringBuilder();
        for (int i = 0; i < ops.length; i++) {
            sb.append(ops[i].name());
            if (i < ops.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
