package com.trivadis.kafkaws;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Notification {
    @JsonProperty
    private long id;
    @JsonProperty
    private String message;
    @JsonProperty
    private String createdAt;

    public Notification() {};

    public Notification(long id, String message, String createdAt) {
        this.id = id;
        this.message = message;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", message='" + message + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
