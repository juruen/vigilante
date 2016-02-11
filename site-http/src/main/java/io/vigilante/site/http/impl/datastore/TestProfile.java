package io.vigilante.site.http.impl.datastore;


import com.spotify.asyncdatastoreclient.Datastore;
import com.spotify.asyncdatastoreclient.DatastoreConfig;

public class TestProfile  {
    private static final String DATASTORE_HOST = System.getProperty("host", "http://localhost:8082");
    private static final String DATASET_ID = System.getProperty("dataset", "test");
    private static final String NAMESPACE = "test";

    public static Datastore getBackend() {
        return Datastore.create(
            DatastoreConfig.builder()
                .connectTimeout(5000)
                .requestTimeout(1000)
                .maxConnections(5)
                .requestRetry(3)
                .host(DATASTORE_HOST)
                .dataset(DATASET_ID)
                .namespace(NAMESPACE)
                .build());
    }
}