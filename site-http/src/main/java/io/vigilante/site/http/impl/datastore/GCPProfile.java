package io.vigilante.site.http.impl.datastore;


import com.google.api.services.datastore.client.DatastoreHelper;
import com.google.api.services.datastore.client.DatastoreOptions;
import com.spotify.asyncdatastoreclient.Datastore;
import com.spotify.asyncdatastoreclient.DatastoreConfig;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class GCPProfile {

    private final static String DEFAULT_NAMESPACE = "test";

    public static Datastore getBackend() throws GeneralSecurityException, IOException {
        DatastoreOptions options = DatastoreHelper.getOptionsfromEnv().build();

        return Datastore.create(
            DatastoreConfig.builder()
                .requestTimeout(10000)
                /*
                .maxConnections(20)
                .requestRetry(3)
                */
                .dataset(options.getDataset())
                .credential(options.getCredential())
                .namespace(DEFAULT_NAMESPACE)
                .build());

    }
}
