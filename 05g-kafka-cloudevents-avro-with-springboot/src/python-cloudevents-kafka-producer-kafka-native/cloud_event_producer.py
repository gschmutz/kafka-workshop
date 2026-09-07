import uuid
from datetime import datetime, timezone

from confluent_kafka import Producer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
from confluent_kafka.serialization import SerializationContext, MessageField

_CE_SPEC_VERSION = "ce_specversion"
_CE_ID           = "ce_id"
_CE_TYPE         = "ce_type"
_CE_SOURCE       = "ce_source"
_CE_TIME         = "ce_time"
_CE_DATA_SCHEMA  = "ce_dataschema"
_CONTENT_TYPE    = "content-type"


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
        serializer = self._get_serializer(schema_str)
        value_bytes = serializer(payload, SerializationContext(topic, MessageField.VALUE))

        headers = self._build_ce_headers(event_type, subject_name)

        self._producer.produce(
            topic=topic,
            key=key,
            value=value_bytes,
            headers=headers,
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

    def _build_ce_headers(
        self, event_type: str, subject_name: str | None
    ) -> list[tuple[str, str]]:
        headers = [
            (_CE_SPEC_VERSION, "1.0"),
            (_CE_ID,           str(uuid.uuid4())),
            (_CE_TYPE,         event_type),
            (_CE_SOURCE,       self._source),
            (_CE_TIME,         datetime.now(timezone.utc).isoformat()),
            (_CONTENT_TYPE,    "avro/binary"),
        ]
        if subject_name:
            data_schema = (
                f"{self._schema_registry_url}/subjects/{subject_name}/versions/latest"
            )
            headers.append((_CE_DATA_SCHEMA, data_schema))
        return headers


def _delivery_callback(err, msg) -> None:
    if err:
        print(f"ERROR: delivery failed [key={msg.key()}]: {err}")
    else:
        print(
            f"Published CE [key={msg.key()}, topic={msg.topic()}, "
            f"partition={msg.partition()}, offset={msg.offset()}]"
        )
