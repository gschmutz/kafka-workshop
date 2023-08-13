package com.trivadis.kafkaws.springbootkafkaproducer;


import com.trivadis.kafkaws.avro.v1.Customer;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.apache.avro.data.Json;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

@Component
public class RestProducer {
    private static final String urlJson = "http://dataplatform:18086/topics/rest-proxy-topic";
    private static final String urlAvro= "http://dataplatform:18086/topics/rest-proxy-avro-topic";

    public static String getJsonString(GenericContainer record) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonEncoder encoder = EncoderFactory.get().jsonEncoder(record.getSchema(), os);
        DatumWriter<GenericContainer> writer = new GenericDatumWriter<GenericContainer>();
        if (record instanceof SpecificRecord) {
            writer = new SpecificDatumWriter<GenericContainer>();
        }

        writer.setSchema(record.getSchema());
        writer.write(record, encoder);
        encoder.flush();
        String jsonString = new String(os.toByteArray(), Charset.forName("UTF-8"));
        os.close();
        return jsonString;
    }

    public void produceJson(Customer customer) throws IOException, RestClientException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.kafka.json.v2+json"));

        //System.out.println(customer.toString());
        System.out.println(getJsonString(customer));

        KafkaMessage km = new KafkaMessage();
        Record record = new Record();
        CustomerJson cust = new CustomerJson();
        cust.id = customer.getId();
        cust.customerCode = customer.getCustomerCode();
        cust.email = customer.getEmail();
        cust.language = customer.getLanguage();

        record.key = "1";
        record.value = cust;
        km.records.add(record);

        System.out.println(Customer.getClassSchema().toString());

        //long id = src.getId("rest-proxy-avro-topic-value", new AvroSchema(Customer.getClassSchema()));

        //HttpEntity<KafkaMessage> entity = new HttpEntity<>(km, headers);

        String msg = "{ \"records\": [{\n" +
                "      \"key\": \"1\",\n" +
                "      \"value\": " + customer.toString() + "    }] }";

        HttpEntity<String> entity = new HttpEntity<>(msg, headers);

                /*
        HttpEntity<String> entity = new HttpEntity<>("{ \"records\": [{\n" +
                "      \"key\": \"1\",\n" +
                "      \"value\": {\n" +
                "        \"id\": 1,\n" +
                "        \"customerCode\": 1,\n" +
                "        \"telephone\": \"888582158\",\n" +
                "        \"email\": \"supplier1@test.com\",\n" +
                "        \"language\": \"EN\"        \n" +
                "      }\n" +
                "    }] }", headers);

                 */
        restTemplate.exchange(urlJson, HttpMethod.POST, entity, String.class);
    }

    public void produceAvro(Customer customer) throws IOException, RestClientException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.kafka.avro.v2+json"));
        headers.setAccept(MediaType.parseMediaTypes("application/vnd.kafka.v2+json"));

        //System.out.println(customer.toString());
        System.out.println(getJsonString(customer));

        KafkaMessage km = new KafkaMessage();
        Record record = new Record();

        System.out.println(Customer.getClassSchema().toString());

        SchemaRegistryClient src = new CachedSchemaRegistryClient("http://dataplatform:8081", 10);
        SchemaMetadata sm = src.getLatestSchemaMetadata("rest-proxy-avro-topic-value");
        System.out.println(sm.getId());
        //long id = src.getId("rest-proxy-avro-topic-value", new AvroSchema(Customer.getClassSchema()));

        //HttpEntity<KafkaMessage> entity = new HttpEntity<>(km, headers);

        String msg = "{ \"value_schema_id\": " + sm.getId() + ",\"records\": [{\n" +
//                "      \"key\": \"1\",\n" +
                "      \"value\": " + customer.toString() + "    }] }";

        HttpEntity<String> entity = new HttpEntity<>(msg, headers);

        restTemplate.exchange(urlAvro, HttpMethod.POST, entity, String.class);
    }
}
