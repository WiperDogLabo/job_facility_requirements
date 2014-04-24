package org.wiperdog.jobmanager.test;

import javax.inject.Inject;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.junit.runner.JUnitCore;
import org.osgi.service.cm.ManagedService;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ScheduleAndRescheduleJobTest {
	
	public ScheduleAndRescheduleJobTest() {
	}

	public static final String PATH_TO_CLASS = "src/resource/JobExecutableImpl.groovy";
	String path = System.getProperty("user.dir");
	def jf;
	String jobName;
	def executable;
	def jd;
	def tr;
	Class jobExecutableCls;
	
	@Inject
	private org.osgi.framework.BundleContext context;
	
	@Configuration
	public Option[] config() {
		return options(
		cleanCaches(true),
		frameworkStartLevel(6),
		// felix log level
		systemProperty("felix.log.level").value("4"), // 4 = DEBUG
		// setup properties for fileinstall bundle.
		systemProperty("felix.home").value(path),
		// Pax-exam make this test code into OSGi bundle at runtime, so
		// we need "groovy-all" bundle to use this groovy test code.
		mavenBundle("org.codehaus.groovy", "groovy-all", "2.2.1").startLevel(2),
		mavenBundle("commons-collections", "commons-collections", "3.2.1").startLevel(2),
		mavenBundle("commons-beanutils", "commons-beanutils", "1.8.0").startLevel(2),
		mavenBundle("commons-digester", "commons-digester", "2.0").startLevel(2),
		wrappedBundle(mavenBundle("c3p0", "c3p0", "0.9.1.2").startLevel(3)),
		mavenBundle("org.wiperdog", "org.wiperdog.rshell.api", "0.1.0").startLevel(3),
		mavenBundle("org.quartz-scheduler", "quartz", "2.2.1").startLevel(3),
		//mavenBundle("org.wiperdog", "org.wiperdog.jobmanager", "0.2.1").startLevel(3),		
		mavenBundle("org.wiperdog", "org.wiperdog.jobmanager", "0.2.3-SNAPSHOT").startLevel(3),
		junitBundles()
		);
	}
	
	@Before
	public void setup() throws Exception {
		jf = context.getService(context.getServiceReference("org.wiperdog.jobmanager.JobFacade"));
		
		// load class
		ClassLoaderUtil lc = new ClassLoaderUtil();
		jobExecutableCls = lc.getCls(PATH_TO_CLASS);
	}
	
	@After
	public void shutdown() throws Exception {
	}
	
	/**
	 * Create schedule for job with jobDetail and trigger.
	 * Create trigger with the parameters: jobName, delay, interval.
	 * The schedule does not exist. 
	 * Expected: create schedule success.
	 */
	@Test
	public void schedule_and_reschedule_job_01() throws Exception {
		executable = jobExecutableCls.newInstance(path + "/" + jobName, "class", "sender");
		jd = jf.createJob(executable);
		tr = jf.createTrigger(jobName, 0, 200);
		// process schedule for job
		jf.scheduleJob(jd, tr);
	}
	
	/**
	 * Create schedule for job with jobDetail and trigger.
	 * Create trigger with only one parameters: jobName
	 * The schedule does not exist.
	 * Expected: create schedule success.
	 */
	@Test
	public void schedule_and_reschedule_job_02() throws Exception {
		executable = jobExecutableCls.newInstance(path + "/" + jobName, "class", "sender");
		jd = jf.createJob(executable);
		tr = jf.createTrigger(jobName);
		// process schedule for job
		jf.scheduleJob(jd, tr);
	}

	/**
	 * Create schedule for job with jobDetail and trigger.
	 * Create trigger with the parameters: jobName, long.
	 * The schedule does not exist.
	 * Expected: create schedule success.
	 */
	@Test
	public void schedule_and_reschedule_job_03() throws Exception {
		executable = jobExecutableCls.newInstance(path + "/" + jobName, "class", "sender");
		jd = jf.createJob(executable);
		tr = jf.createTrigger(jobName, 10);
		// process schedule for job
		jf.scheduleJob(jd, tr);
	}
	
	/**
	 * Create schedule for job with jobDetail and trigger.
	 * Create trigger with the parameters: jobName, Date.
	 * The schedule does not exist.
	 * Expected: create schedule success.
	 */
	@Test
	public void schedule_and_reschedule_job_04() throws Exception {
		Date date = new Date();
		executable = jobExecutableCls.newInstance(path + "/" + jobName, "class", "sender");
		jd = jf.createJob(executable);
		tr = jf.createTrigger(jobName, date);
		// process schedule for job
		jf.scheduleJob(jd, tr);
	}
	
	/**
	 * Create schedule for job with jobDetail and trigger.
	 * Create trigger with the parameters: jobName, crontab.
	 * The schedule does not exist.
	 * Expected: create schedule success.
	 */
	@Test
	public void schedule_and_reschedule_job_05() throws Exception {
		executable = jobExecutableCls.newInstance(path + "/" + jobName, "class", "sender");
		jd = jf.createJob(executable);
		tr = jf.createTrigger(jobName, "0/60 * * * * ?");
		// process schedule for job
		jf.scheduleJob(jd, tr);
	}
	
	/**
	 * Create schedule for job with jobDetail and trigger.
	 * The schedule already exist. 
	 * Expected: create schedule success. 
	 */
	@Test
	public void schedule_and_reschedule_job_06() throws Exception {
		executable = jobExecutableCls.newInstance(path + "/" + jobName, "class", "sender");
		jd = jf.createJob(executable);
		tr = jf.createTrigger(jobName, 0, 200);
		// process schedule for job
		jf.scheduleJob(jd, tr);
		Thread.sleep(5000);
		// re-schedule for job
		tr = jf.createTrigger(jobName, 10, 100);
		jf.scheduleJob(jd, tr);
	}
	
	/**
	 * Create schedule for job with jobDetail is null. 
	 * Expected: create schedule failure.
	 */
	@Test
	public void schedule_and_reschedule_job_07() throws Exception {
		try {
			jd = null;
			tr = jf.createTrigger(jobName, 0, 200);
			// process schedule for job with jobdetail is null
			jf.scheduleJob(jd, tr);
		} catch (Exception e) {
			assertTrue(e.toString().contains("java.lang.NullPointerException")); 
			System.out.println(e);
		}
	}
	
	/**
	 * Create schedule for job with Trigger is null. 
	 * Expected: create schedule failure.
	 */
	@Test
	public void schedule_and_reschedule_job_08() {
		try {
			executable = jobExecutableCls.newInstance(path + "/" + jobName, "class", "sender");
			jd = jf.createJob(executable);
			tr = null;
			// process schedule for job with Trigger is null
			jf.scheduleJob(jd, tr);
		} catch (Exception e) {
			assertTrue(e.toString().contains("java.lang.NullPointerException")); 
			System.out.println(e);
		}
	}
}
	
