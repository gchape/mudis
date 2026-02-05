package io.mudis.mudis.utils;

import io.mudis.mudis.model.Operation;

/**
 * Validates request parameters for operations.
 */
public class RequestValidator {
    private static final int MAX_SUBSCRIBE_ARGS_SIZE = 256;
    private static final int MAX_UNSUBSCRIBE_ARGS_SIZE = 256;
    private static final int MAX_PUBLISH_ARGS_SIZE = 1024 * 1024; // 1MB

    private RequestValidator() {
    }

    public static void validateArgsBytes(byte[] argsBytes, Operation op) {
        if (argsBytes == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }

        int maxSize = getMaxArgsSize(op);
        if (argsBytes.length > maxSize) {
            throw new IllegalArgumentException(
                    String.format("Arguments too long: %d bytes (max: %d for %s)",
                            argsBytes.length, maxSize, op)
            );
        }
    }

    public static void validateArgsSize(int size, Operation op) {
        if (size < 0) {
            throw new IllegalStateException("Negative arguments size: " + size);
        }

        int maxSize = getMaxArgsSize(op);
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
                    String.format("Invalid operation ordinal: %d (valid range: 0-%d)",
                            ordinal, Operation.values().length - 1)
            );
        }
    }

    private static int getMaxArgsSize(Operation op) {
        return switch (op) {
            case SUBSCRIBE -> MAX_SUBSCRIBE_ARGS_SIZE;
            case UNSUBSCRIBE -> MAX_UNSUBSCRIBE_ARGS_SIZE;
            case PUBLISH -> MAX_PUBLISH_ARGS_SIZE;
        };
    }
}
