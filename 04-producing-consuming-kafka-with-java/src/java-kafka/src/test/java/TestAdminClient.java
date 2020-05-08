import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

class TestAdminClient {

	@Test
	void test() throws InterruptedException, ExecutionException {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "analyticsplatform:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());

        AdminClient client = AdminClient.create(props);
        ListTopicsResult result = client.listTopics();
        for (String topic : result.names().get()) {
        	System.out.println(topic);
        }
        KafkaFuture<Collection<TopicListing>> l = result.listings();
        for (TopicListing tl : l.get()) {
        	System.out.println(tl.toString());
        }
        DescribeTopicsResult describeTopicsResult = client.describeTopics(Collections.singleton("ATLAS_HOOK"));
        DescribeConfigsResult configs = client.describeConfigs(Collections.singleton(new ConfigResource(ConfigResource.Type.TOPIC, "ATLAS_HOOK")));
        System.out.println(describeTopicsResult.values());
        System.out.println(configs.values().get(new ConfigResource(ConfigResource.Type.TOPIC, "ATLAS_HOOK")).get());
	}

}
