package io.mudis.mudis.utils;

import io.mudis.mudis.model.Operation;

public class RequestValidator {
    public static int MAX_SUBSCRIBE_ARGS_SIZE = 256;
    public static int MAX_UNSUBSCRIBE_ARGS_SIZE = 256;
    public static int MAX_PUBLISH_ARGS_SIZE = 1024 * 1024;

    public static void validateArgsBytes(final byte[] argsBytes,
                                         final Operation op) {
        int maxSize = getOperationArgsMaxSize(op);

        if (argsBytes.length > maxSize) {
            throw new IllegalArgumentException(String.format(
                    "Arguments too long: %d bytes (max: %d)", argsBytes.length, maxSize));
        }
    }

    public static void validateArgsSize(final int size, final Operation op) {
        if (size < 0) {
            throw new IllegalStateException("Negative arguments size: " + size);
        }

        var maxSize = getOperationArgsMaxSize(op);
        if (size > maxSize) {
            throw new IllegalStateException(
                    String.format("Arguments size %d exceeds maximum %d for %s",
                            size, maxSize, op)
            );
        }
    }

    public static void validateOperation(int ordinal) {
        if (ordinal < 0 || ordinal >= Operation.values().length) {
            throw new IllegalStateException(
                    String.format("Invalid operation ordinal: %d (valid: 0-%d)",
                            ordinal, Operation.values().length - 1)
            );
        }
    }

    private static int getOperationArgsMaxSize(Operation op) {
        return switch (op) {
            case SUBSCRIBE -> MAX_SUBSCRIBE_ARGS_SIZE;
            case UNSUBSCRIBE -> MAX_UNSUBSCRIBE_ARGS_SIZE;
            case PUBLISH -> MAX_PUBLISH_ARGS_SIZE;
        };
    }
}
