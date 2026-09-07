package com.trivadis.kafkaws.events;

public final class CloudEventHeaders {
    public static final String SPEC_VERSION  = "ce_specversion";
    public static final String ID            = "ce_id";
    public static final String TYPE          = "ce_type";
    public static final String SOURCE        = "ce_source";
    public static final String TIME          = "ce_time";
    public static final String DATA_SCHEMA   = "ce_dataschema";
    public static final String SUBJECT       = "ce_subject";
    public static final String CONTENT_TYPE  = "content-type";

    public static final String PARTITION_KEY = "ce_partitionkey";

    public static final String AVRO_CONTENT_TYPE = "avro/binary";
    public static final String SPEC_VERSION_1_0  = "1.0";

    private CloudEventHeaders() {}
}