package io.vigilante.site.api;

public interface ResponseCallback<E> {
	void onFinished(boolean success, Throwable t, E o);
}
