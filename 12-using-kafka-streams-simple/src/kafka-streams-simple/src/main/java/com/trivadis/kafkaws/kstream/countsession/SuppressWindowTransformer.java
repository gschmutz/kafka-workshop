package com.trivadis.kafkaws.kstream.countsession;

import org.apache.avro.generic.GenericData;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.Window;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.internals.TimeWindow;
import org.apache.kafka.streams.processor.Cancellable;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

import java.io.StringBufferInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SuppressWindowTransformer implements Transformer<Windowed<String>, Long, KeyValue<String, StateValue>> {
    private ProcessorContext context;
    private Cancellable cancellable;
    private KeyValueStore<String, StateValue> kvStore;
    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        kvStore = (KeyValueStore) context.getStateStore("suppressed_windowed_transformer");
        cancellable = context.schedule(Duration.ofMillis(2000), PunctuationType.STREAM_TIME, timestamp -> flushOldWindow());
    }

    @Override
    public KeyValue<String, StateValue> transform(Windowed<String> key, Long value) {
        if (value == null) {
            kvStore.delete(key.key());
        } else {
            System.out.println("Handle " + value + ": " + key.window().startTime() + "->" + key.window().endTime());
            kvStore.put(key.key(), new StateValue(key.window().startTime(), key.window().endTime(), value));    //buffer (or suppress) the new in coming window
            flushOldWindow();
        }

        return null;
    }

    private void flushOldWindow() {
        List<KeyValue<String, StateValue>> toBeClosedItems = new ArrayList<>();

        KeyValueIterator<String, StateValue> iterator = kvStore.all();
        while (iterator.hasNext() ) {
            KeyValue<String, StateValue> item = iterator.next();

            System.out.println("FlushOrldWindow(): " + item.value.getStartTime() + ":" + item.value.getEndTime());
            if (item.value.getEndTime().isBefore(Instant.ofEpochMilli(context.currentStreamTimeMs()).minusSeconds(15))) {
                System.out.println (item.value.getEndTime() + " is older than 15secs");
                toBeClosedItems.add(item);
            }
        }
        iterator.close();
        //your logic to check for old windows in kvStore then

        //forward (or unsuppressed) your suppressed records downstream using ProcessorContext.forward(key, value)
        for (KeyValue<String, StateValue> item : toBeClosedItems) {
            kvStore.delete(item.key);
            System.out.println("Close item:" + item.key + ":" + item.value);

            context.forward(item.key, item.value);
        }
    }

    @Override
    public void close() {
        cancellable.cancel();//cancel punctuate
    }
}


