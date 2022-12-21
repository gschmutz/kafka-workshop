package com.trivadis.kafkaws.kstream.countsession;

import java.time.Instant;

public class StateValue {
    private Instant startTime;
    private Instant endTime;
    private Long count;

    public StateValue() {}

    public StateValue(Instant startTime, Instant endTime, Long count) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.count = count;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public Long getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "StateValue{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", count=" + count +
                '}';
    }
}
