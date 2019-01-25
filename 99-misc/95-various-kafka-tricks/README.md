# Various Kafka Tricks
## Consuming from __consumer_offsets topic

```
kafka-console-consumer --consumer.config /tmp/consumer.config \
--formatter "kafka.coordinator.group.GroupMetadataManager\$OffsetsMessageFormatter" \
--bootstrap-server broker-1:9092 --topic __consumer_offsets
```

