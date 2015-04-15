package com.nike.tools.bgm.tasks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.client.ssh.SshClient;
import com.nike.tools.bgm.client.ssh.SshTarget;
import com.nike.tools.bgm.env.EnvironmentTx;
import com.nike.tools.bgm.model.domain.ApplicationVm;
import com.nike.tools.bgm.model.domain.TaskStatus;
import com.nike.tools.bgm.utils.ThreadSleeper;
import com.nike.tools.bgm.utils.Waiter;

/**
 * Runs a configurable command over ssh to a third-party system that knows how to
 * create an application vm.
 */
@Lazy
@Component
public class SshVmCreateTask extends ApplicationVmTask
{
  private static final String CMDVAR_ENVNAME = "${envName}";
  private static final long WAIT_DELAY_MILLISECONDS = 30000L; //30sec

  private static int maxNumWaits = 120; //1 hour
  private static int waitReportInterval = 4; //2min

  private static final Logger LOGGER = LoggerFactory.getLogger(SshVmCreateTask.class);

  @Autowired
  private ThreadSleeper threadSleeper;

  @Autowired
  private EnvironmentTx environmentTx;

  @Autowired
  private SshTarget sshTarget;

  @Autowired
  private SshVmCreateConfig sshVmCreateConfig;

  private SshClient sshClient;

  public Task init(int position, String envName)
  {
    super.init(position, envName, true/*createVm*/);
    return this;
  }

  /**
   * Runs a command over ssh on a third-party host that knows how to create an application vm.
   * <p/>
   * Persists the record of this new vm in the current environment.
   */
  @Override
  public TaskStatus process(boolean noop)
  {
    initSshClient();
    execSshVmCreateCommand(noop);
    persistModel(noop);
    return noop ? TaskStatus.NOOP : TaskStatus.DONE;
  }

  private void initSshClient()
  {
    sshClient = new SshClient().init(sshTarget);
  }

  /**
   * Executes the initial command to create a vm.
   */
  private void execSshVmCreateCommand(boolean noop)
  {
    LOGGER.info(context() + "Executing vm-create command over ssh" + noopRemark(noop));
    if (!noop)
    {
      String command = substituteInitialVariables(sshVmCreateConfig.getInitialCommand());
      String output = sshClient.execCommand(command);
      applicationVm = waitTilVmIsAvailable(output);
    }
  }

  /**
   * Substitutes variables of the form '${vblname}' in the original string, returns the replaced version.
   * Currently supports only one variable: envName
   */
  private String substituteInitialVariables(String original)
  {
    return StringUtils.replace(original, CMDVAR_ENVNAME, environment.getEnvName());
  }

  /**
   * Creates a Waiter using an ssh vm progress checker, and returns a transient ApplicationVm entity when done.
   * In case of error - never returns null, throws instead.
   */
  private ApplicationVm waitTilVmIsAvailable(String initialOutput)
  {
    LOGGER.info(context() + "Waiting for applicationVm to become available");
    SshVmCreateProgressChecker progressChecker = new SshVmCreateProgressChecker(initialOutput, context(),
        sshClient, sshTarget, sshVmCreateConfig);
    Waiter<ApplicationVm> waiter = new Waiter(maxNumWaits, waitReportInterval, WAIT_DELAY_MILLISECONDS, threadSleeper,
        progressChecker);
    applicationVm = waiter.waitTilDone();
    if (applicationVm == null)
    {
      throw new RuntimeException(context() + progressChecker.getDescription() + " did not become available");
    }
    return applicationVm;
  }

  /**
   * Attaches the applicationVm to the environment entity, then opens a transaction and persists them.
   */
  private void persistModel(boolean noop)
  {
    if (!noop)
    {
      environment.addApplicationVm(applicationVm);
      applicationVm.setEnvironment(environment);
      environmentTx.updateEnvironment(environment); //Cascades to new applicationVm.
    }
  }
}