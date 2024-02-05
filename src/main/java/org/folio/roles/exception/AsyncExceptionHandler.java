package org.folio.roles.exception;

import java.lang.reflect.Method;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

@Log4j2
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

  private final boolean outputParams;

  public AsyncExceptionHandler() {
    this(false);
  }

  public AsyncExceptionHandler(boolean outputParams) {
    this.outputParams = outputParams;
  }

  @Override
  public void handleUncaughtException(Throwable throwable, Method method, Object... params) {
    if (outputParams) {
      log.error("Async method [{}] throw exception. Params: [{}]", method, StringUtils.join(params, ", "), throwable);
    } else {
      log.error("Async method [{}] throw exception", method, throwable);
    }
  }
}
