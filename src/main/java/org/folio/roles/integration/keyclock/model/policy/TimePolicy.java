package org.folio.roles.integration.keyclock.model.policy;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TimePolicy extends BasePolicy {

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime notBefore;
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime notOnOrAfter;
  private Integer month;
  private Integer monthEnd;
  private Integer dayMonth;
  private Integer dayMonthEnd;
  private Integer hour;
  private Integer hourEnd;
  private Integer minute;
  private Integer minuteEnd;

  private Config config;

  public boolean hasConfigValue() {
    return nonNull(config);
  }

  @JsonIgnore
  public boolean isRepeated() {
    if (hasConfigValue()) {
      return nonNull(config.getHour());
    } else {
      return nonNull(getHour());
    }
  }

  @Data
  public static class Config {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nbf;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime noa;
    private Integer month;
    private Integer monthEnd;
    private Integer dayMonth;
    private Integer dayMonthEnd;
    private Integer hour;
    private Integer hourEnd;
    private Integer minute;
    private Integer minuteEnd;
  }
}
