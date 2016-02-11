package io.vigilante.site.api;

import java.security.GeneralSecurityException;

import com.google.common.util.concurrent.ListenableFuture;

public interface AuthenticationManager {

	AuthenticationResponse googleVerify(String namespace, String token) throws GeneralSecurityException;

	ListenableFuture<Void> verifySessionToken(String namespace, String session);
}
