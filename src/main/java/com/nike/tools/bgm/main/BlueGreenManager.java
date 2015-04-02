package com.nike.tools.bgm.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import com.nike.tools.bgm.jobs.Job;
import com.nike.tools.bgm.jobs.JobFactory;

import static com.nike.tools.bgm.main.ReturnCode.CMDLINE_ERROR;
import static com.nike.tools.bgm.main.ReturnCode.PROCESSING_ERROR;
import static com.nike.tools.bgm.main.ReturnCode.SUCCESS;

@Component
public class BlueGreenManager
{
  private static Logger LOGGER = LoggerFactory.getLogger(BlueGreenManager.class);

  @Autowired
  private ArgumentParser argumentParser;

  @Autowired
  private JobFactory jobFactory;

  /**
   * Parses input args, decides what job to run.  Returns true if args are valid.
   */
  public Job parseArgsToJob(String[] args)
  {
    argumentParser.parseArgs(args);
    return jobFactory.makeJob(argumentParser.getJobName(), argumentParser.getParameters(), argumentParser.getCommandLine());
  }

  private void explainValidJobs()
  {
    jobFactory.explainValidJobs();
  }

  public static void main(String[] args)
  {
    ReturnCode returnCode = SUCCESS;
    BlueGreenManager blueGreenManager = null;
    try
    {
      ApplicationContext context =
          new ClassPathXmlApplicationContext(new String[] { "applicationContext/main.xml" });

      blueGreenManager = context.getBean(BlueGreenManager.class);
      Job job = blueGreenManager.parseArgsToJob(args);
      if (job != null)
      {
        job.process();
      }
    }
    catch (CmdlineException e)
    {
      returnCode = CMDLINE_ERROR;
      if (blueGreenManager != null)
      {
        blueGreenManager.explainValidJobs();
        LOGGER.error(e.getMessage());
      }
      else
      {
        LOGGER.error("Inexplicable cmdline error", e);
      }
    }
    catch (Throwable e)
    {
      returnCode = PROCESSING_ERROR;
      LOGGER.error("Processing error", e);
    }
    finally
    {
      System.exit(returnCode.getCode()); //NOSONAR
    }
  }

}
