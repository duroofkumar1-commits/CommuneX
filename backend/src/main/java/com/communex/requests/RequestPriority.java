package com.communex.requests;

/** Server-classified coordination priority; request clients cannot set this field directly. */
public enum RequestPriority {
    LOW(0), NORMAL(1), HIGH(2), CRITICAL(3);

    private final int rank;
    RequestPriority(int rank) { this.rank = rank; }
    public int rank() { return rank; }
}
