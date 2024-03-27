package org.folio.roles.integration.keyclock.configuration;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.ResourceUtils.getFile;

import feign.Client;
import feign.codec.Encoder;
import feign.form.FormEncoder;
import feign.okhttp.OkHttpClient;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import lombok.extern.log4j.Log4j2;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.folio.roles.integration.keyclock.configuration.properties.KeycloakTlsProperties;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;

/**
 * Feign configuration for keycloak.
 */
@Log4j2
public class FeignConfiguration {

  /**
   * Additional map to form-data encoder for feign client.
   *
   * @param converters - existing {@link HttpMessageConverters} factory
   * @return configured {@link Encoder} object
   */
  @Bean
  public Encoder feignFormEncoder(ObjectFactory<HttpMessageConverters> converters) {
    return new FormEncoder(new SpringEncoder(converters));
  }

  /**
   * Feign {@link OkHttpClient} based client.
   *
   * @param okHttpClient - {@link OkHttpClient} from spring context
   * @param properties - keycloak configuration properties
   * @return created feign {@link Client} object
   */
  @Bean
  public Client feignClient(okhttp3.OkHttpClient okHttpClient, KeycloakConfigurationProperties properties)
    throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
    var client = properties.getTls().isEnabled()
      ? sslClient(okHttpClient.newBuilder(), properties.getTls())
      : okHttpClient;

    return new OkHttpClient(client);
  }

  private static okhttp3.OkHttpClient sslClient(okhttp3.OkHttpClient.Builder clientBuilder,
    KeycloakTlsProperties properties)
    throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
    log.debug("Creating OkHttpClient with SSL enabled...");
    var keyStore = initKeyStore(properties);
    var trustManager = trustManager(keyStore);

    var sslSocketFactory = sslContext(trustManager).getSocketFactory();

    return clientBuilder
      .sslSocketFactory(sslSocketFactory, trustManager)
      .hostnameVerifier(NoopHostnameVerifier.INSTANCE)
      .build();
  }

  private static KeyStore initKeyStore(KeycloakTlsProperties properties)
    throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
    var trustStorePath = requireNonNull(properties.getTrustStorePath(), "Trust store path is not defined");
    var trustStorePassword = requireNonNull(properties.getTrustStorePassword(), "Trust store password is not defined");

    KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (var is = new FileInputStream(getFile(trustStorePath))) {
      trustStore.load(is, trustStorePassword.toCharArray());
    }
    log.debug("Keystore initialized from file: keyStoreType = {}, file = {}", trustStore.getType(), trustStorePath);

    return trustStore;
  }

  private static X509TrustManager trustManager(KeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException {
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(keyStore);

    TrustManager[] trustManagers = tmf.getTrustManagers();
    if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
      throw new IllegalStateException("Unexpected default trust managers:"
        + Arrays.toString(trustManagers));
    }

    return (X509TrustManager) trustManagers[0];
  }

  private static SSLContext sslContext(X509TrustManager trustManager)
    throws NoSuchAlgorithmException, KeyManagementException {

    var sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[] {trustManager}, null);
    log.debug("SSL context initialized: protocol = {}", sslContext.getProtocol());

    return sslContext;
  }
}
