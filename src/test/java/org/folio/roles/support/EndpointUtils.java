package org.folio.roles.support;

import static org.folio.roles.domain.dto.HttpMethod.DELETE;
import static org.folio.roles.domain.dto.HttpMethod.GET;
import static org.folio.roles.domain.dto.HttpMethod.PATCH;
import static org.folio.roles.domain.dto.HttpMethod.POST;
import static org.folio.roles.domain.dto.HttpMethod.PUT;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.HttpMethod;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EndpointUtils {

  public static Endpoint endpoint() {
    return endpoint("/test-resources", POST);
  }

  public static Endpoint endpoint(String staticPath, HttpMethod method) {
    return new Endpoint().path(staticPath).method(method);
  }

  public static Endpoint fooItemGetEndpoint() {
    return endpoint("/foo/items/{id}", GET);
  }

  public static Endpoint fooItemGetCollectionEndpoint() {
    return endpoint("/foo/items", GET);
  }

  public static Endpoint fooItemPutEndpoint() {
    return endpoint("/foo/items/{id}", PUT);
  }

  public static Endpoint fooItemPatchEndpoint() {
    return endpoint("/foo/items/{id}", PATCH);
  }

  public static Endpoint fooItemDeleteEndpoint() {
    return endpoint("/foo/items/{id}", DELETE);
  }

  public static Endpoint fooItemPostEndpoint() {
    return endpoint("/foo/items", POST);
  }

  public static Endpoint barItemGetEndpoint() {
    return endpoint("/bar/items/{id}", GET);
  }

  public static Endpoint barItemPutEndpoint() {
    return endpoint("/bar/items/{id}", PUT);
  }

  public static Endpoint barItemPatchEndpoint() {
    return endpoint("/bar/items/{id}", PATCH);
  }
}
