package org.folio.roles.integration.kafka;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.domain.dto.CapabilityAction.EDIT;
import static org.folio.roles.domain.dto.CapabilityAction.VIEW;
import static org.folio.roles.domain.dto.CapabilityType.DATA;
import static org.folio.roles.integration.kafka.model.ModuleType.MODULE;
import static org.folio.roles.integration.kafka.model.ModuleType.UI_MODULE;
import static org.folio.roles.support.AuthResourceUtils.permission;
import static org.folio.roles.support.CapabilityUtils.APPLICATION_ID;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.support.EndpointUtils.endpoint;
import static org.folio.roles.support.EndpointUtils.fooItemGetEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPatchEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPutEndpoint;
import static org.folio.roles.utils.CapabilityUtils.getCapabilityName;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilityAction;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.HttpMethod;
import org.folio.roles.integration.kafka.model.CapabilityEvent;
import org.folio.roles.integration.kafka.model.CapabilityResultHolder;
import org.folio.roles.integration.kafka.model.CapabilitySetDescriptor;
import org.folio.roles.integration.kafka.model.FolioResource;
import org.folio.roles.integration.kafka.model.ModuleType;
import org.folio.roles.integration.kafka.model.Permission;
import org.folio.roles.service.permission.FolioPermissionService;
import org.folio.roles.support.AuthResourceUtils;
import org.folio.roles.support.TestUtils;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CapabilityEventProcessorTest {

  @InjectMocks private CapabilityEventProcessor capabilityEventProcessor;
  @Mock private FolioPermissionService folioPermissionService;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @MethodSource("capabilityEventDataProvider")
  @DisplayName("process_parameterized")
  @ParameterizedTest(name = "[{index}] {0}")
  void process_parameterized(@SuppressWarnings("unused") String name,
    CapabilityEvent event, CapabilityResultHolder expectedResult) {
    if (CollectionUtils.isNotEmpty(expectedResult.capabilitySets())) {
      when(folioPermissionService.expandPermissionNames(any())).then(inv ->
        mapItems(inv.<List<String>>getArgument(0), AuthResourceUtils::permission));
    }

    var result = capabilityEventProcessor.process(event);

    assertThat(result).isEqualTo(expectedResult);
  }

  private static Stream<Arguments> capabilityEventDataProvider() {
    var itemGetPermName = "test-resource.item.get";
    var itemViewPermName = "test-resource.view";
    var resource = "Test-Resource Item";
    var csResource = "Test-Resource";
    var description = resource + " - permission description";
    var csDescription = csResource + " - permission set description";
    var capability = capability(null, resource, VIEW, itemGetPermName).description(description);
    var uiCapability = capability(null, csResource, VIEW, itemViewPermName).description(csDescription);
    var capabilitySetDesc = capabilitySetDescriptor(csResource, Map.of(resource, List.of(VIEW)));
    var permission = permission(itemGetPermName).description(description);
    var permissionSet = permission(itemViewPermName, itemGetPermName).description(csDescription);

    return Stream.of(
      arguments("module event (permission)",
        event(MODULE, resource(permission)),
        result(List.of(capability), emptyList())),

      arguments("module event (permission and permissionSet)",
        event(MODULE, resource(permission), resource(permissionSet)),
        result(List.of(capability), List.of(capabilitySetDesc))),

      arguments("module event (found element endpoints empty)",
        event(MODULE,
          resource(permission("foo.item.put"), fooItemPutEndpoint()),
          resource(permission("foo.item.patch"))),
        result(List.of(fooCapability(EDIT, "put", fooItemPutEndpoint())), emptyList())),

      arguments("module event (curr element endpoints empty)",
        event(MODULE,
          resource(permission("foo.item.put")),
          resource(permission("foo.item.patch"), fooItemPatchEndpoint())),
        result(List.of(fooCapability(EDIT, "put")), emptyList())),

      arguments("module event (duplicate capability name)",
        event(MODULE, resource(permission), resource(permission)),
        result(List.of(capability), emptyList())),

      arguments("module event (put and patch endpoints merged)",
        event(MODULE,
          resource(permission("foo.item.put"), fooItemPutEndpoint()),
          resource(permission("foo.item.patch"), fooItemPatchEndpoint())),
        result(List.of(fooCapability(EDIT, "put", fooItemPutEndpoint(), fooItemPatchEndpoint())),
          emptyList())),

      arguments("module event (put and patch endpoints not merged)",
        event(MODULE,
          resource(permission("foo.item.put"), fooItemPutEndpoint()),
          resource(permission("foo.item.patch"), endpoint("/module/foo/items/{id}", HttpMethod.PATCH))),
        result(List.of(fooCapability(EDIT, "put", fooItemPutEndpoint())), emptyList())),

      arguments("module event (patch and put endpoints merged)",
        event(MODULE,
          resource(permission("foo.item.patch"), fooItemPatchEndpoint()),
          resource(permission("foo.item.put"), fooItemPutEndpoint())),
        result(List.of(fooCapability(EDIT, "put", fooItemPatchEndpoint(), fooItemPutEndpoint())), emptyList())),

      arguments("module event (put and get endpoints not merged)",
        event(MODULE,
          resource(permission("foo.item.put"), fooItemPutEndpoint()),
          resource(permission("foo.item.get"), fooItemGetEndpoint())),
        result(
          List.of(fooCapability(EDIT, "put", fooItemPutEndpoint()), fooCapability(VIEW, "get", fooItemGetEndpoint())),
          emptyList())),

      arguments("module event (put and get endpoints not merged (same permission name))",
        event(MODULE,
          resource(permission("foo.item.put"), fooItemPutEndpoint()),
          resource(permission("foo.item.put"), fooItemGetEndpoint())),
        result(List.of(fooCapability(EDIT, "put", fooItemPutEndpoint())), emptyList())),

      arguments("module event (get and put endpoints not merged (same permission name))",
        event(MODULE,
          resource(permission("foo.item.put"), fooItemGetEndpoint()),
          resource(permission("foo.item.put"), fooItemPutEndpoint())),
        result(List.of(fooCapability(EDIT, "put", fooItemGetEndpoint())), emptyList())),

      arguments("module event (permission set)",
        event(MODULE, resource(permissionSet)),
        result(emptyList(), List.of(capabilitySetDesc))),

      arguments("module event (duplicate permission set)",
        event(MODULE, resource(permissionSet), resource(permissionSet)),
        result(emptyList(), List.of(capabilitySetDesc))),

      arguments("ui-module event (permission)",
        event(UI_MODULE, resource(permission)),
        result(List.of(capability), emptyList())),

      arguments("ui-module event (permission)",
        event(UI_MODULE, resource(permission), resource(permission)),
        result(List.of(capability), emptyList())),

      arguments("ui-module event (permission set)",
        event(UI_MODULE, resource(permissionSet), resource(permission)),
        result(List.of(uiCapability, capability), List.of(capabilitySetDesc))),

      arguments("module event(empty resources)", event(UI_MODULE), result(emptyList(), emptyList())),
      arguments("ui-module event(empty resources)", event(UI_MODULE), result(emptyList(), emptyList()))
    );
  }

  private static CapabilityEvent event(ModuleType type, FolioResource... resources) {
    return new CapabilityEvent()
      .moduleId("test-module-0.0.1")
      .moduleType(type)
      .applicationId(APPLICATION_ID)
      .resources(asList(resources));
  }

  private static FolioResource resource(Permission permission, Endpoint... endpoints) {
    return new FolioResource().permission(permission).endpoints(asList(endpoints));
  }

  private static CapabilityResultHolder result(List<Capability> capabilities, List<CapabilitySetDescriptor> sets) {
    return new CapabilityResultHolder(capabilities, sets);
  }

  private static CapabilitySetDescriptor capabilitySetDescriptor(String resource,
    Map<String, List<CapabilityAction>> capabilities) {
    return new CapabilitySetDescriptor()
      .name(getCapabilityName(resource, CapabilityAction.VIEW))
      .resource(resource)
      .action(CapabilityAction.VIEW)
      .type(DATA)
      .applicationId(APPLICATION_ID)
      .description(resource + " - permission set description")
      .capabilities(capabilities);
  }

  private static Capability fooCapability(CapabilityAction action, String permissionSuffix, Endpoint... endpoints) {
    return capability(null, "Foo Item", action, "foo.item." + permissionSuffix, endpoints).description(null);
  }
}
