package io.vigilante.site.http.model.adapters;

public interface Adapter<A, B> {
    B adapt(A a);
}
