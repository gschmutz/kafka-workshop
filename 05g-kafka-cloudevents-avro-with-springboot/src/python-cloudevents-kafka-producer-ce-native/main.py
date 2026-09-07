from config import BOOTSTRAP_SERVERS, SCHEMA_REGISTRY_URL, ORDERS_TOPIC, CLOUDEVENTS_SOURCE
from cloud_event_producer import CloudEventProducer
from order_service import OrderService


def main() -> None:
    producer = CloudEventProducer(
        bootstrap_servers=BOOTSTRAP_SERVERS,
        schema_registry_url=SCHEMA_REGISTRY_URL,
        source=CLOUDEVENTS_SOURCE,
    )
    service = OrderService(producer, ORDERS_TOPIC)

    service.place_order("CUST-4711", 142.50, "CHF")

    producer.flush()


if __name__ == "__main__":
    main()
