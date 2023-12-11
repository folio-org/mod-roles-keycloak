package org.folio.roles.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * The entity that extends from {@link BasePolicyEntity} and represents a time-based policy.
 */
@Data
@Entity
@DiscriminatorValue("TIME")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TimePolicyEntity extends BasePolicyEntity {

  /**
   * A flag indicating whether this policy should repeat or not.
   */
  @Column(name = "repeat")
  private Boolean repeat = false;

  /**
   * The start time of the policy.
   */
  @Column(name = "start")
  private OffsetDateTime start;

  /**
   * The expiry time of the policy.
   */
  @Column(name = "expires")
  private OffsetDateTime expires;

  /**
   * The start day of the month for the policy.
   */
  @Column(name = "day_of_month_start")
  private Integer dayOfMonthStart;

  /**
   * The end day of the month for the policy.
   */
  @Column(name = "day_of_month_end")
  private Integer dayOfMonthEnd;

  /**
   * The start month of the policy.
   */
  @Column(name = "month_start")
  private Integer monthStart;

  /**
   * The end month of the policy.
   */
  @Column(name = "month_end")
  private Integer monthEnd;

  /**
   * The start hour of the policy.
   */
  @Column(name = "hour_start")
  private Integer hourStart;

  /**
   * The end hour of the policy.
   */
  @Column(name = "hour_end")
  private Integer hourEnd;

  /**
   * The start minute of the policy.
   */
  @Column(name = "minute_start")
  private Integer minuteStart;

  /**
   * The end minute of the policy.
   */
  @Column(name = "minute_end")
  private Integer minuteEnd;
}
