package io.vigilante.site.http;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

public class SiteConfiguration extends Configuration {
    final private int DEFAULT_PORT = 8082;

    private Integer port = DEFAULT_PORT;

    @JsonProperty
    public Integer getPort() {
        return port;
    }

    @JsonProperty
    public void setPort(Integer port) {
        this.port = port;
    }
}