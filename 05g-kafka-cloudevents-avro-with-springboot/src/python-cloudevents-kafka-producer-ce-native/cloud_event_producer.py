import uuid
from datetime import datetime, timezone

from cloudevents.v1.http import CloudEvent
from cloudevents.v1.kafka import to_binary
from confluent_kafka import Producer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
from confluent_kafka.serialization import SerializationContext, MessageField


class CloudEventProducer:

    def __init__(self, bootstrap_servers: str, schema_registry_url: str, source: str):
        self._source = source
        self._schema_registry_url = schema_registry_url
        self._schema_registry_client = SchemaRegistryClient({"url": schema_registry_url})
        self._producer = Producer({"bootstrap.servers": bootstrap_servers})
        self._serializer_cache: dict[str, AvroSerializer] = {}

    def publish(
        self,
        topic: str,
        key: str,
        payload: dict,
        schema_str: str,
        event_type: str,
        subject_name: str | None = None,
    ) -> None:
        value_bytes = self._get_serializer(schema_str)(
            payload, SerializationContext(topic, MessageField.VALUE)
        )

        attributes = {
            "type": event_type,
            "source": self._source,
            "id": str(uuid.uuid4()),
            "time": datetime.now(timezone.utc).isoformat(),
            "datacontenttype": "avro/binary",
        }
        if subject_name:
            attributes["dataschema"] = (
                f"{self._schema_registry_url}/subjects/{subject_name}/versions/latest"
            )

        event = CloudEvent(attributes=attributes, data=value_bytes)
        message = to_binary(event, data_marshaller=lambda x: x)

        self._producer.produce(
            topic=topic,
            key=key,
            value=message.value,
            headers=message.headers,
            on_delivery=_delivery_callback,
        )
        self._producer.poll(0)

    def flush(self) -> None:
        self._producer.flush()

    def _get_serializer(self, schema_str: str) -> AvroSerializer:
        if schema_str not in self._serializer_cache:
            self._serializer_cache[schema_str] = AvroSerializer(
                self._schema_registry_client, schema_str
            )
        return self._serializer_cache[schema_str]


def _delivery_callback(err, msg) -> None:
    if err:
        print(f"ERROR: delivery failed [key={msg.key()}]: {err}")
    else:
        print(
            f"Published CE [key={msg.key()}, topic={msg.topic()}, "
            f"partition={msg.partition()}, offset={msg.offset()}]"
        )
