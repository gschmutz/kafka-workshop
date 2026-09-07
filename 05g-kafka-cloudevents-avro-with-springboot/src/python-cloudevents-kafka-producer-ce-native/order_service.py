import uuid
from datetime import datetime, timezone
from pathlib import Path

from cloud_event_producer import CloudEventProducer

_SCHEMA_STR = (Path(__file__).parent / "avro" / "order-created.avsc").read_text()
_EVENT_TYPE = "com.example.orders.OrderCreated"
_SUBJECT    = "outbox.Order-value"


class OrderService:

    def __init__(self, producer: CloudEventProducer, topic: str):
        self._producer = producer
        self._topic = topic

    def place_order(self, customer_id: str, amount: float, currency: str) -> str:
        order_id = str(uuid.uuid4())

        payload = {
            "orderId":    order_id,
            "customerId": customer_id,
            "amount":     amount,
            "currency":   currency,
            "createdAt":  datetime.now(timezone.utc).isoformat(),
        }

        self._producer.publish(
            topic=self._topic,
            key=order_id,
            payload=payload,
            schema_str=_SCHEMA_STR,
            event_type=_EVENT_TYPE,
            subject_name=_SUBJECT,
        )

        print(f"Order placed: {order_id}")
        return order_id
