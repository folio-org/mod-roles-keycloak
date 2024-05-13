package org.folio.roles.domain.model;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ExtendedCapabilitySet extends CapabilitySet {

  private List<Capability> capabilityList;
}
