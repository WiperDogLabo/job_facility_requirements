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
public class InterruptJobTest {
	
	public InterruptJobTest() {
	}

	public static final String PATH_TO_JOBEXECUTABLECLASS = "src/resource/JobExecutableImpl.groovy"
	public static final String PATH_TO_JOBINTERUPTCLASS = "src/resource/InterruptJob.groovy"
	String path = System.getProperty("user.dir");
	def jf;
	Class jobExecutableCls;
	Class jobInteruptCls;
	
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
		jobExecutableCls = lc.getCls(PATH_TO_JOBEXECUTABLECLASS);
		jobInteruptCls = lc.getCls(PATH_TO_JOBINTERUPTCLASS);
	}
	
	@After
	public void shutdown() throws Exception {
	}
	
	/**
	 * Interrupt running job test.
	 * Expected: Interrupt running job success.
	 */
	@Test
	public void interrupt_job_01() throws Exception {
		String jobName1 = "jobTest1";
		
		def executable1 = jobInteruptCls.newInstance(jobName1, "class", "sender");
		def jd1 = jf.createJob(executable1);
		
		def tr1 = jf.createTrigger(jobName1, 100);
		jf.scheduleJob(jd1, tr1);
		
		Thread.sleep(1000)
		def jobRunningcount1 = jf.getJobRunningCount(jobName1)
		assertEquals(1, jobRunningcount1)
		
		//Interrupt job
		jf.interruptJob(jobName1)
		
		Thread.sleep(500)
		def jobRunningcount2 = jf.getJobRunningCount(jobName1)
		assertEquals(0, jobRunningcount2)
	}
	/**
	 * Interrupt running job if job run time > maxruntime of job class. 
	 * Expected: Interrupt running job success.
	 */
	@Test
	public void interrupt_job_02() throws Exception {
		String jcname = "class";
		int concurrency = 1;
		long maxruntime = 1000;
		String jobName1 = "jobTest1";
		def jc = jf.createJobClass(jcname);
		jc.setConcurrency(concurrency)
		jc.setMaxRunTime(maxruntime)		
		
		def executable1 = jobInteruptCls.newInstance(jobName1, "class", "sender");
		def jd1 = jf.createJob(executable1);
		
		jc.addJob(jf.jobKeyForName(jobName1))
		
		def tr1 = jf.createTrigger(jobName1, 0);
		jf.scheduleJob(jd1, tr1);
		
		Thread.sleep(500)
		
		def jobRunningcount = jf.getJobRunningCount(jobName1)
		assertEquals(1, jobRunningcount)
		
		//After 1.5s, job will be interrupted
		Thread.sleep(1000)
		jobRunningcount = jf.getJobRunningCount(jobName1)
		assertEquals(0, jobRunningcount)
		
		Thread.sleep(10000)
		jobRunningcount = jf.getJobRunningCount(jobName1)
		assertEquals(0, jobRunningcount)
	}

	/**
	 * Interrupt if job wait time > maxwaittime of job class.
	 * Expected: Waitting job will be given up.
	 */
	@Test
	public void interrupt_job_03() throws Exception {
		String jcname = "class";
		int concurrency = 1;
		long maxruntime = 1000;
		long maxwaittime = 1000;
		String jobName1 = "jobTest1";
		String jobName2 = "jobTest2";
		def jc = jf.createJobClass(jcname, concurrency, maxwaittime, Long.MAX_VALUE);
		
		def executable1 = jobInteruptCls.newInstance(jobName1, "class", "sender");
		def executable2 = jobInteruptCls.newInstance(jobName2, "class", "sender");
		def jd1 = jf.createJob(executable1);
		def jd2 = jf.createJob(executable2);
		
		jc.addJob(jf.jobKeyForName(jobName1))
		jc.addJob(jf.jobKeyForName(jobName2))
		
		def tr1 = jf.createTrigger(jobName1, 0);
		def tr2 = jf.createTrigger(jobName2, 100);
		jf.scheduleJob(jd1, tr1);
		jf.scheduleJob(jd2, tr2);
		
		//Job1 successfully execute, but job 2 will be given up.
		Thread.currentThread().sleep(15000)
		println (jobName2 + " will be interrupt !!!")
		
		// wait for interrupt job
		def jobStatus1 = executable1.getStatus()
		def jobStatus2 = executable2.getStatus()
		assertEquals("finish", jobStatus1)
		assertEquals("init", jobStatus2)
	}

	/**
	 * Interrupt running job if job run time > maxruntime of job class (2jobs)
	 * Expected: Waitting job will be interrupted.
	 */
	@Test
	public void interrupt_job_04() throws Exception {
		String jcname = "class";
		int concurrency = 1;
		long maxruntime = 1000;
		long maxwaittime = 1000;
		String jobName1 = "jobTest1";
		String jobName2 = "jobTest2";
		def jc = jf.createJobClass(jcname, concurrency, maxwaittime, maxruntime);
		
		def executable1 = jobInteruptCls.newInstance(jobName1, "class", "sender");
		def executable2 = jobInteruptCls.newInstance(jobName2, "class", "sender");
		def jd1 = jf.createJob(executable1);
		def jd2 = jf.createJob(executable2);
		
		jc.addJob(jf.jobKeyForName(jobName1))
		jc.addJob(jf.jobKeyForName(jobName2))
		
		def tr1 = jf.createTrigger(jobName1, 0);
		def tr2 = jf.createTrigger(jobName2, 100);
		jf.scheduleJob(jd1, tr1);
		jf.scheduleJob(jd2, tr2);
		
		//Interrupt job 1
		Thread.currentThread().sleep(500)
		println (jobName1 + " is running but will be interrupted !!!")
		
		// wait for interrupt job
		def jobStatus1 = executable1.getStatus()
		def isInterrupt1 = executable1.checkInterrupt()
		def jobStatus2 = executable2.getStatus()
		def isInterrupt2 = executable2.checkInterrupt()
		assertEquals("running and will be interrupt", jobStatus1)
		assertEquals("init", jobStatus2)
		assertEquals(true, isInterrupt1)
		assertEquals(false, isInterrupt2)
		
		Thread.currentThread().sleep(15000)
		println (jobName2 + " will be interrupt !!!")
		
		// wait for interrupt job
		jobStatus1 = executable1.getStatus()
		isInterrupt1 = executable1.checkInterrupt()
		jobStatus2 = executable2.getStatus()
		isInterrupt2 = executable2.checkInterrupt()
		assertEquals("running and will be interrupt", jobStatus1)
		assertEquals("running and will be interrupt", jobStatus2)
		assertEquals(true, isInterrupt1)
		assertEquals(true, isInterrupt2)
	}
	
	/**
	 * Interrupt running job if job run time > maxruntime of job class (2jobs: job 1 run nomally, job 2 will be interrupted)
	 * Expected: Waitting job will be interrupted.
	 */
	@Test
	public void interrupt_job_05() throws Exception {
		String jcname = "class";
		int concurrency = 1;
		long maxruntime = 5000;
		long maxwaittime = 20000;
		String jobName1 = "jobTest1";
		String jobName2 = "jobTest2";
		def jc = jf.createJobClass(jcname, concurrency, maxwaittime, maxruntime);
		
		def executable1 = jobExecutableCls.newInstance(jobName1, "class", "sender");
		def executable2 = jobInteruptCls.newInstance(jobName2, "class", "sender");
		def jd1 = jf.createJob(executable1);
		def jd2 = jf.createJob(executable2);
		
		jc.addJob(jf.jobKeyForName(jobName1))
		jc.addJob(jf.jobKeyForName(jobName2))
		
		def tr1 = jf.createTrigger(jobName1, 0);
		def tr2 = jf.createTrigger(jobName2, 100);
		jf.scheduleJob(jd1, tr1);
		jf.scheduleJob(jd2, tr2);
		
		Thread.currentThread().sleep(15000)
		def jobStatus2 = executable2.getStatus()
		def isInterrupt2 = executable2.checkInterrupt()
		
		assertEquals(true, isInterrupt2)
	}
}