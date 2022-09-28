package com.trivadis.kafkaws;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Notification {
    @JsonProperty
    public long id;
    @JsonProperty
    public String message;

    public Notification() {}

    public Notification(long id, String message) {
        this.id = id;
        this.message = message;
    }

}
