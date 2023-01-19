package com.trivadis.kafkaws.kstream.countsession;

import org.apache.kafka.streams.KeyValue;

import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.*;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SuppressWindowProcessor implements ProcessorSupplier<Windowed<String>, Long, String, StateValue> {
    StoreBuilder<?> storeBuilder;

    public SuppressWindowProcessor(StoreBuilder<?> storeBuilder) {
        this.storeBuilder = storeBuilder;
    }

    @Override
    public Processor<Windowed<String>, Long, String, StateValue> get() {
        return new SupressWindowProcessor();
    }

    @Override
    public Set<StoreBuilder<?>> stores() {
        return ProcessorSupplier.super.stores();
    }

    class SupressWindowProcessor extends ContextualProcessor <Windowed<String>, Long, String, StateValue> {

        private KeyValueStore<String, StateValue> kvStore;

        @Override
        public void init(ProcessorContext<String, StateValue> context) {
            super.init(context);
            kvStore = (KeyValueStore) context.getStateStore("suppressed_windowed_transformer");

            context().schedule(Duration.ofMillis(2000), PunctuationType.WALL_CLOCK_TIME, timestamp -> flushOldWindow(timestamp));
        }

        @Override
        public void process(Record<Windowed<String>, Long> record) {
            if (record.value() == null) {
                kvStore.delete(record.key().key());
            } else {
                System.out.println("Handle " + record.value() + ": " + record.key().window().startTime() + "->" + record.key().window().endTime());
                kvStore.put(record.key().key(), new StateValue(record.key().window().startTime(), record.key().window().endTime(), record.value()));    //buffer (or suppress) the new in coming window
            }
        }

        private void flushOldWindow(long timestamp) {
            List<KeyValue<String, StateValue>> toBeClosedItems = new ArrayList<>();

            KeyValueIterator<String, StateValue> iterator = kvStore.all();
            while (iterator.hasNext() ) {
                KeyValue<String, StateValue> item = iterator.next();

                System.out.println("FlushOldWindow(): " + item.value.getStartTime() + ":" + item.value.getEndTime());
                if (item.value.getEndTime().isBefore(Instant.now().minusSeconds(15))) {
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

                context().forward(new Record<String,StateValue>(item.key, item.value, item.value.getEndTime().toEpochMilli()));
            }
        }

    }
}



