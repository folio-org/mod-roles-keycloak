package org.folio.roles.support;

import static java.util.UUID.fromString;
import static org.folio.roles.domain.dto.CapabilityAction.CREATE;
import static org.folio.roles.domain.dto.CapabilityType.DATA;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.utils.CapabilityUtils.getCapabilityName;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.folio.roles.domain.dto.Capabilities;
import org.folio.roles.domain.dto.CapabilitiesUpdateRequest;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilityAction;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.entity.CapabilityEntity;

@UtilityClass
public class CapabilityUtils {

  public static final String FOO_RESOURCE = "Foo Item";
  public static final String UI_FOO_RESOURCE = "UI Foo Item";
  public static final String RESOURCE_NAME = "Test Resource";
  public static final String APPLICATION_ID = "test-application-0.0.1";
  public static final String PERMISSION_NAME = "test-resource.item.create";

  public static final UUID CAPABILITY_ID = UUID.randomUUID();

  public static final UUID FOO_CREATE_CAPABILITY = fromString("8d2da27c-1d56-48b6-958d-2bfae6d79dc8");
  public static final UUID FOO_VIEW_CAPABILITY = fromString("e2628d7d-059a-46a1-a5ea-10a5a37b1af2");
  public static final UUID FOO_EDIT_CAPABILITY = fromString("78d6a59f-90ab-46a1-a349-4d25d0798763");
  public static final UUID FOO_DELETE_CAPABILITY = fromString("ff2c8ad0-2b82-4b87-bafe-43a55ae7f4db");

  public static final UUID UI_FOO_VIEW_CAPABILITY = fromString("48e57f3b-3622-43db-b437-5d30ebe8f867");
  public static final UUID UI_FOO_CREATE_CAPABILITY = fromString("f491047c-32eb-4736-815c-ebb8e94dffac");
  public static final UUID UI_FOO_EDIT_CAPABILITY = fromString("af9a59c5-ba1d-47df-82f0-6dd3cef2b25e");
  public static final UUID UI_FOO_DELETE_CAPABILITY = fromString("5d764bb8-b5e5-4f33-8640-23eb9732b438");

  public static Capability capability() {
    return capability(CAPABILITY_ID, RESOURCE_NAME, CREATE, PERMISSION_NAME, EndpointUtils.endpoint());
  }

  public static Capability capability(UUID id) {
    return capability(id, RESOURCE_NAME, CREATE, PERMISSION_NAME, EndpointUtils.endpoint());
  }

  public static Capability capability(UUID id, String permission) {
    return capability(id, RESOURCE_NAME, CREATE, permission, EndpointUtils.endpoint());
  }

  public static Capability capability(UUID id, Endpoint... endpoints) {
    return capability(id, RESOURCE_NAME, CREATE, PERMISSION_NAME, endpoints);
  }

  public static Capability capability(UUID id, String resource, CapabilityAction action,
    String permission, Endpoint... endpoints) {
    return new Capability()
      .id(id)
      .type(DATA)
      .applicationId(APPLICATION_ID)
      .name(getCapabilityName(resource, action))
      .description(String.format("Capability to %s a %s", action.getValue(), resource.toLowerCase()))
      .resource(resource)
      .permission(permission)
      .endpoints(Arrays.asList(endpoints))
      .action(action);
  }

  public static Capabilities capabilities(Capability... capabilities) {
    return capabilities(capabilities.length, capabilities);
  }

  public static Capabilities capabilities(long totalRecords, Capability... capabilities) {
    return new Capabilities()
      .capabilities(Arrays.asList(capabilities))
      .totalRecords(totalRecords);
  }

  public static CapabilityEntity capabilityEntity() {
    return capabilityEntity(CAPABILITY_ID, RESOURCE_NAME, CREATE, PERMISSION_NAME);
  }

  public static CapabilityEntity capabilityEntity(UUID id) {
    return capabilityEntity(id, RESOURCE_NAME, CREATE, PERMISSION_NAME);
  }

  public static CapabilityEntity capabilityEntity(UUID id, String resource, CapabilityAction action) {
    return capabilityEntity(id, resource, action, PERMISSION_NAME);
  }

  public static CapabilityEntity capabilityEntity(String resource, CapabilityAction action) {
    return capabilityEntity(CAPABILITY_SET_ID, resource, action, PERMISSION_NAME);
  }

  public static CapabilityEntity capabilityEntity(UUID id, String resource,
    CapabilityAction action, String permission) {
    var entity = new CapabilityEntity();
    entity.setId(id);
    entity.setName(getCapabilityName(resource, action));
    entity.setResource(resource);
    entity.setAction(action);
    entity.setApplicationId(CapabilityUtils.APPLICATION_ID);
    entity.setType(DATA);
    entity.setPermission(permission);
    return entity;
  }

  public static Capability fooItemCapability(UUID id, CapabilityAction action) {
    var capabilityName = "foo_item." + action.getValue();
    return new Capability()
      .id(id)
      .name(capabilityName)
      .resource("Foo Item")
      .description(String.format("Capability to %s a foo item", action.getValue()))
      .applicationId(APPLICATION_ID)
      .type(DATA)
      .action(action);
  }

  public static Capability fooItemCapability(UUID id, CapabilityAction action, String perm, Endpoint... endpoints) {
    var capabilityName = "foo_item." + action.getValue();
    return new Capability()
      .id(id)
      .name(capabilityName)
      .resource("Foo Item")
      .permission(perm)
      .endpoints(Arrays.asList(endpoints))
      .description(String.format("Capability to %s a foo item", action.getValue()))
      .applicationId(APPLICATION_ID)
      .type(DATA)
      .action(action);
  }

  public static CapabilitiesUpdateRequest capabilitiesUpdateRequest(UUID... capabilityIds) {
    return new CapabilitiesUpdateRequest().capabilityIds(List.of(capabilityIds));
  }

  public static CapabilitiesUpdateRequest capabilitiesUpdateRequest(List<UUID> ids) {
    return new CapabilitiesUpdateRequest().capabilityIds(ids);
  }
}
