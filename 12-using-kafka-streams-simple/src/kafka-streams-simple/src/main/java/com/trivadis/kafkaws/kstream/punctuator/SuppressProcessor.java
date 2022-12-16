package com.trivadis.kafkaws.kstream.punctuator;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.TimestampedKeyValueStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;

import java.time.Duration;

public class SuppressProcessor implements Processor<String, Integer, String, Integer> {

    private final String fStoreName;

    private final Duration fPunctuationInterval;
    private final long fSuppressTimeoutMillis;
    private TimestampedKeyValueStore<String, Integer> stateStore;
    private ProcessorContext<String, Integer> context;

    public SuppressProcessor(String storeName, Duration suppressTimeout, Duration punctuationInterval) {
        fStoreName = storeName;
        fSuppressTimeoutMillis = suppressTimeout.toMillis();
        fPunctuationInterval = punctuationInterval;
    }

    @Override
    public void init(ProcessorContext<String, Integer> context) {
        stateStore = (TimestampedKeyValueStore) context.getStateStore(fStoreName);
        this.context = context;
        context.schedule(fPunctuationInterval, PunctuationType.WALL_CLOCK_TIME, this::punctuate);
    }

    @Override
    public void process(Record<String, Integer> record) {
        ValueAndTimestamp<Integer> r = stateStore.get(record.key());
        if (r != null) {
            stateStore.put(record.key(), ValueAndTimestamp.make(r.value() + record.value(), r.timestamp()));
        } else {
            stateStore.put(record.key(), ValueAndTimestamp.make(record.value(), record.timestamp()));
        }

    }

    private void punctuate(long timestamp) {
        try (var iterator = stateStore.all()) {
            while (iterator.hasNext()) {
                KeyValue<String, ValueAndTimestamp<Integer>> record = iterator.next();
                if (timestamp - record.value.timestamp() > fSuppressTimeoutMillis) {
                    context.forward(new Record<>(record.key, record.value.value(), record.value.timestamp()));
                    stateStore.delete(record.key);
                }
            }
        }
    }
}
