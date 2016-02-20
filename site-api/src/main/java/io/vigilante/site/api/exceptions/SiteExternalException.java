package io.vigilante.site.api.exceptions;

public class SiteExternalException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SiteExternalException(String message) {
		super(message);
	}
}
