package org.folio.roles.configuration;

import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@Configuration
public class AsynchConfiguration {

  @Bean("executorForLoadableRolesAssignmentsRetry")
  public Executor executorForLoadableRolesAssignmentsRetry() {
    var virtualThreadTaskExecutor = new VirtualThreadTaskExecutor();
    return command -> virtualThreadTaskExecutor.execute(getRunnableWithCurrentFolioContext(command));
  }
}
