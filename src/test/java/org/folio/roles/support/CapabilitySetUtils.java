package org.folio.roles.support;

import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static org.folio.roles.domain.dto.CapabilityAction.CREATE;
import static org.folio.roles.domain.dto.CapabilityType.DATA;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.CapabilityUtils.FOO_CREATE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_DELETE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_EDIT_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_MANAGE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_VIEW_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.RESOURCE_NAME;
import static org.folio.roles.support.CapabilityUtils.UI_FOO_CREATE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.UI_FOO_EDIT_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.UI_FOO_VIEW_CAPABILITY;
import static org.folio.roles.utils.CapabilityUtils.getCapabilityName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilityAction;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.dto.CapabilitySets;
import org.folio.roles.domain.dto.CapabilitySetsUpdateRequest;
import org.folio.roles.domain.entity.CapabilitySetEntity;
import org.folio.roles.domain.entity.type.EntityCapabilityAction;
import org.folio.roles.domain.entity.type.EntityCapabilityType;
import org.folio.roles.domain.model.ExtendedCapabilitySet;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CapabilitySetUtils {

  public static final UUID CAPABILITY_SET_ID = UUID.randomUUID();

  public static final String CAPABILITY_SET_NAME = "test-resource_item.create";
  public static final String INVALID_CAPABILITY_SET_NAME = "boo_item.create";

  public static final UUID FOO_MANAGE_CAPABILITY_SET = fromString("a1002e06-a2bc-4ce4-9d71-e25db1250e09");
  public static final List<UUID> FOO_MANAGE_CAPABILITIES = List.of(
    FOO_VIEW_CAPABILITY, FOO_CREATE_CAPABILITY, FOO_EDIT_CAPABILITY, FOO_DELETE_CAPABILITY);

  public static final UUID FOO_VIEW_CAPABILITY_SET = fromString("8812fa56-1e07-4bea-87a8-16b548f7e4fc");
  public static final List<UUID> FOO_VIEW_CAPABILITIES = List.of(FOO_VIEW_CAPABILITY);

  public static final UUID FOO_MANAGE_V2_CAPABILITY_SET = fromString("a8ec43cc-f38d-4d34-9e5c-39815ffd099c");
  public static final List<UUID> FOO_MANAGE_V2_CAPABILITIES = List.of(FOO_MANAGE_CAPABILITY);

  public static final String FOO_EDIT_CAPABILITY_SET_NAME = "foo_item.edit";
  public static final UUID FOO_EDIT_CAPABILITY_SET = fromString("6532d4f8-3e97-4d8b-886f-4ec2a2adc4a3");
  public static final List<UUID> FOO_EDIT_CAPABILITIES = List.of(FOO_VIEW_CAPABILITY, FOO_EDIT_CAPABILITY);

  public static final String FOO_CREATE_CAPABILITY_SET_NAME = "foo_item.create";
  public static final UUID FOO_CREATE_CAPABILITY_SET = fromString("55a910de-cecf-4e0e-9d35-2e8e2ecf699e");
  public static final List<UUID> FOO_CREATE_CAPABILITIES = List.of(FOO_VIEW_CAPABILITY, FOO_CREATE_CAPABILITY);

  public static final UUID UI_FOO_EDIT_CAPABILITY_SET = fromString("ec87daa4-47e5-48da-beae-603dfaaa128a");
  public static final List<UUID> UI_FOO_EDIT_CAPABILITIES = List.of(UI_FOO_VIEW_CAPABILITY, UI_FOO_EDIT_CAPABILITY);

  public static final UUID UI_FOO_CREATE_CAPABILITY_SET = fromString("4fdc76d8-efdf-4ffa-b231-8c7bbbb62939");
  public static final List<UUID> UI_FOO_CREATE_CAPABILITIES = List.of(UI_FOO_VIEW_CAPABILITY, UI_FOO_CREATE_CAPABILITY);

  public static CapabilitySet capabilitySet() {
    return capabilitySet(CAPABILITY_SET_ID, RESOURCE_NAME, CREATE, List.of(CAPABILITY_ID));
  }

  public static CapabilitySet capabilitySet(UUID id) {
    return capabilitySet(id, RESOURCE_NAME, CREATE, List.of(CAPABILITY_ID));
  }

  public static CapabilitySet capabilitySet(List<UUID> capabilityIds) {
    return capabilitySet(CAPABILITY_SET_ID, RESOURCE_NAME, CREATE, capabilityIds);
  }

  public static CapabilitySet capabilitySet(UUID id, List<UUID> capabilityIds) {
    return capabilitySet(id, RESOURCE_NAME, CREATE, capabilityIds);
  }

  public static CapabilitySet capabilitySet(String resource, CapabilityAction action) {
    return capabilitySet(CAPABILITY_SET_ID, resource, action, List.of(CAPABILITY_ID));
  }

  public static CapabilitySet capabilitySet(UUID id, String res, CapabilityAction action, List<UUID> capabilityIds) {
    return new CapabilitySet()
      .id(id)
      .name(getCapabilityName(res, action))
      .resource(res)
      .action(action)
      .description(String.format("Capability set to %s a %s", action.getValue(), res.toLowerCase()))
      .applicationId(APPLICATION_ID)
      .capabilities(capabilityIds != null ? new ArrayList<>(capabilityIds) : null)
      .type(DATA);
  }

  public static ExtendedCapabilitySet extendedCapabilitySet(CapabilitySet set, Capability... capabilities) {
    return extendedCapabilitySet(set, asList(capabilities));
  }

  public static ExtendedCapabilitySet extendedCapabilitySet(CapabilitySet set, List<Capability> capabilities) {
    var extendedCapabilitySet = new ExtendedCapabilitySet();

    extendedCapabilitySet.setId(set.getId());
    extendedCapabilitySet.setName(set.getName());
    extendedCapabilitySet.setResource(set.getResource());
    extendedCapabilitySet.setAction(set.getAction());
    extendedCapabilitySet.setDescription(set.getDescription());
    extendedCapabilitySet.setApplicationId(set.getApplicationId());
    extendedCapabilitySet.setType(set.getType());
    extendedCapabilitySet.setCapabilityList(capabilities);
    extendedCapabilitySet.setCapabilities(set.getCapabilities());

    return extendedCapabilitySet;
  }

  public static CapabilitySets capabilitySets(CapabilitySet... capabilitySets) {
    return capabilitySets(capabilitySets.length, capabilitySets);
  }

  public static CapabilitySets capabilitySets(long totalRecords, CapabilitySet... capabilitySets) {
    return new CapabilitySets().capabilitySets(List.of(capabilitySets)).totalRecords(totalRecords);
  }

  public static CapabilitySetEntity capabilitySetEntity() {
    return capabilitySetEntity(CAPABILITY_SET_ID, RESOURCE_NAME, CREATE, List.of(CAPABILITY_ID));
  }

  public static CapabilitySetEntity capabilitySetEntity(UUID id) {
    return capabilitySetEntity(id, RESOURCE_NAME, CREATE, List.of(CAPABILITY_ID));
  }

  public static CapabilitySetEntity capabilitySetEntity(UUID id, List<UUID> capabilityIds) {
    return capabilitySetEntity(id, RESOURCE_NAME, CREATE, capabilityIds);
  }

  public static CapabilitySetEntity capabilitySetEntity(List<UUID> capabilityIds) {
    return capabilitySetEntity(CAPABILITY_SET_ID, RESOURCE_NAME, CREATE, capabilityIds);
  }

  public static CapabilitySetEntity capabilitySetEntity(UUID id, String resource, CapabilityAction action) {
    return capabilitySetEntity(id, resource, action, List.of(CAPABILITY_ID));
  }

  public static CapabilitySetEntity capabilitySetEntity(String resource, CapabilityAction action) {
    return capabilitySetEntity(CAPABILITY_SET_ID, resource, action, List.of(CAPABILITY_ID));
  }

  public static CapabilitySetEntity capabilitySetEntity(UUID id, String resource,
    CapabilityAction action, List<UUID> capabilityIds) {
    var entity = new CapabilitySetEntity();
    entity.setId(id);
    entity.setName(getCapabilityName(resource, action));
    entity.setDescription(String.format("Capability to %s a %s", action.getValue(), resource.toLowerCase()));
    entity.setResource(resource);
    entity.setAction(EntityCapabilityAction.from(action));
    entity.setApplicationId(APPLICATION_ID);
    entity.setCapabilities(capabilityIds);
    entity.setType(EntityCapabilityType.DATA);
    return entity;
  }

  public static CapabilitySetsUpdateRequest capabilitySetsUpdateRequest(UUID... capabilityIds) {
    return new CapabilitySetsUpdateRequest().capabilitySetIds(List.of(capabilityIds));
  }

  public static CapabilitySetsUpdateRequest capabilitySetsUpdateRequest(List<UUID> ids) {
    return new CapabilitySetsUpdateRequest().capabilitySetIds(ids);
  }
}
