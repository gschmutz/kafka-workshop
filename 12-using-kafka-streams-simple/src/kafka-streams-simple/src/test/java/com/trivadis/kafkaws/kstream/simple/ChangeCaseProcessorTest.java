package com.trivadis.kafkaws.kstream.simple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;

import com.trivadis.kafkaws.kstream.simple.ChangeCaseProcessor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ChangeCaseProcessorTest {
    MockProcessorContext processorContext;

    @BeforeEach
    public void setup() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        processorContext = new MockProcessorContext(props);
    }

    @Test
    public void testProcessorUpperCase() {
        String key = null;
        String value = "some value";

        ChangeCaseProcessor processor = new ChangeCaseProcessor(true);
        processor.init(processorContext);
        processor.process(null, "this is a test");

        assertEquals(processorContext.forwarded().size(), 1);
        assertEquals("THIS IS A TEST", processorContext.forwarded().get(0).keyValue().value);
    }

}
