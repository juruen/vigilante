package io.vigilante.site.api.impl.datastoreng;


import com.spotify.asyncdatastoreclient.Entity;

@FunctionalInterface
public interface EntityTranslator<T> {
    T translate(Entity entity);
}
