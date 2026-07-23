package org.folio.roles.utils;

import static java.util.Arrays.stream;

import java.io.IOException;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Log4j2
@RequiredArgsConstructor
@Component
public class ResourceHelper {
  private static final PathMatchingResourcePatternResolver PATH_RESOLVER =
    new PathMatchingResourcePatternResolver(ResourceHelper.class.getClassLoader());

  private final JsonHelper jsonHelper;

  /**
   * Loads json data from the specified directory and deserializes it to the specified type.
   *
   * @param resourceDir  directory to load json data from
   * @param resourceType type of data
   * @param <T>          type of data
   * @return list of deserialized data
   */
  public <T> Stream<T> readObjectsFromDirectory(String resourceDir, Class<T> resourceType) {
    return readSourced(resourceDir, resourceType)
      .map(SourcedResource::value);
  }

  /**
   * Loads json data from the specified directory and deserializes it to the specified type, keeping the path of the
   * file each object was read from.
   *
   * @param resourceDir  directory to load json data from
   * @param resourceType type of data
   * @param <T>          type of data
   * @return list of deserialized data paired with the source file path
   */
  public <T> Stream<SourcedResource<T>> readSourcedObjectsFromDirectory(String resourceDir, Class<T> resourceType) {
    return readSourced(resourceDir, resourceType);
  }

  private <T> Stream<SourcedResource<T>> readSourced(String resourceDir, Class<T> resourceType) {
    return stream(getResources(resourceDir))
      .map(res -> new SourcedResource<>(sourceOf(resourceDir, res), deserializeResource(res, resourceType)));
  }

  private static String sourceOf(String resourceDir, Resource res) {
    return resourceDir + "/" + res.getFilename();
  }

  private <T> T deserializeResource(Resource res, Class<T> resourceType) {
    try {
      return jsonHelper.parse(res.getInputStream(), resourceType);
    } catch (IOException e) {
      var msg =
        String.format("Failed to deserialize data of type %s from file: %s", resourceType, res.getFilename());
      throw new IllegalStateException(msg, e);
    }
  }

  private static Resource[] getResources(String resourceDir) {
    try {
      return PATH_RESOLVER.getResources(String.format("/%s/%s", resourceDir, "*.json"));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load data for path: " + resourceDir, e);
    }
  }

  /**
   * Deserialized object together with the path of the file it was read from.
   *
   * @param source path of the file the value was read from
   * @param value  deserialized value
   * @param <T>    type of the deserialized value
   */
  public record SourcedResource<T>(String source, T value) {}
}
