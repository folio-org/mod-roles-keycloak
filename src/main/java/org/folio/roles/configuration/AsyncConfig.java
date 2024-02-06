package org.folio.roles.configuration;

import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import org.folio.roles.exception.AsyncExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

  // default application executor
  // see TaskExecutorConfigurations.applicationTaskExecutor()
  private final ThreadPoolTaskExecutor executor;

  @Override
  public Executor getAsyncExecutor() {
    return executor;
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new AsyncExceptionHandler(true);
  }
}
