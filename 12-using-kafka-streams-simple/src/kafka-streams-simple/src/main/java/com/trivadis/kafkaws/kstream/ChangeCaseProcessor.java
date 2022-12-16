package com.trivadis.kafkaws.kstream;

import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;

public class ChangeCaseProcessor implements Processor<Void, String> {
    private ProcessorContext context = null;
    private boolean toUpperCase = true;

    public ChangeCaseProcessor(boolean toUpperCase) {
        this.toUpperCase = toUpperCase;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
    }

    @Override
    public void process(Void key, String value) {
        String newValue = value;

        if (toUpperCase) {
            newValue = value.toUpperCase();
        } else {
            newValue = value.toLowerCase();
        }
        context.forward(key, newValue);
    }

    @Override
    public void close() {
        // no special clean up needed in this example
    }

}