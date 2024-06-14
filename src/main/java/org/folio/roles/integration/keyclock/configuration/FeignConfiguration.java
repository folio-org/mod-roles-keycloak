package org.folio.roles.integration.keyclock.configuration;

import feign.Client;
import feign.codec.Encoder;
import feign.form.FormEncoder;
import feign.okhttp.OkHttpClient;
import lombok.extern.log4j.Log4j2;
import org.folio.common.utils.FeignClientTlsUtils;
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
  public Client feignClient(okhttp3.OkHttpClient okHttpClient, KeycloakConfigurationProperties properties) {
    return FeignClientTlsUtils.getOkHttpClient(okHttpClient, properties.getTls());
  }
}
