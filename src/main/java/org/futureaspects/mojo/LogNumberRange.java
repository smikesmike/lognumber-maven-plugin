package org.futureaspects.mojo;

/**
 * Range of allowed lognumbers.
 *
 * @param start first allowed number
 * @param end   first excluded number
 */
record LogNumberRange(int start, int end) {

    /** Name of the Maven property which holds the range. */
    static final String PROPERTY = "log-assign.range";

    /**
     * Parses a range such as {@code 20_000,30_000} or {@code 20.000,30.000}.
     *
     * @param value the configured value
     * @return the parsed range
     * @throws IllegalArgumentException if the value is not a valid range
     */
    static LogNumberRange parse(String value) {
        String[] bounds = value.split(",");
        if (bounds.length != 2) {
            throw new IllegalArgumentException("expected <first>,<firstExcluded> but got '" + value + "'");
        }
        int start = parseBound(bounds[0], value);
        int end = parseBound(bounds[1], value);
        if (start >= end) {
            throw new IllegalArgumentException("start must be lower than end in '" + value + "'");
        }
        return new LogNumberRange(start, end);
    }

    private static int parseBound(String bound, String value) {
        try {
            return Integer.parseUnsignedInt(bound.trim().replace("_", "").replace(".", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid number '" + bound + "' in '" + value + "'", e);
        }
    }

    boolean contains(int number) {
        return number >= start && number < end;
    }

    boolean overlaps(LogNumberRange other) {
        return start < other.end && other.start < end;
    }

    @Override
    public String toString() {
        return start + "," + end;
    }
}
