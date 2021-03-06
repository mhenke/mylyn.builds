/*******************************************************************************
 * Copyright (c) 2010 Markus Knittig and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Markus Knittig - initial API and implementation
 *     Tasktop Technologies - improvements
 *     Eike Stepper - improvements for bug 323759
 *     Benjamin Muskalla - 323920: [build] config retrival fails for jobs with whitespaces
 *******************************************************************************/

package org.eclipse.mylyn.internal.hudson.core.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.eclipse.core.runtime.Assert;
import org.eclipse.mylyn.builds.core.spi.AbstractConfigurationCache;
import org.eclipse.mylyn.commons.core.IOperationMonitor;
import org.eclipse.mylyn.commons.http.CommonHttpClient;
import org.eclipse.mylyn.commons.http.CommonHttpMethod;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.internal.commons.http.CommonPostMethod;
import org.eclipse.mylyn.internal.hudson.model.HudsonMavenReportersSurefireAggregatedReport;
import org.eclipse.mylyn.internal.hudson.model.HudsonModelBuild;
import org.eclipse.mylyn.internal.hudson.model.HudsonModelHudson;
import org.eclipse.mylyn.internal.hudson.model.HudsonModelJob;
import org.eclipse.mylyn.internal.hudson.model.HudsonModelProject;
import org.eclipse.mylyn.internal.hudson.model.HudsonModelRun;
import org.eclipse.mylyn.internal.hudson.model.HudsonModelRunArtifact;
import org.eclipse.mylyn.internal.hudson.model.HudsonTasksJunitTestResult;
import org.eclipse.mylyn.internal.hudson.model.HudsonTasksTestAggregatedTestResultActionChildReport;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.google.gson.Gson;

/**
 * Represents the Hudson repository that is accessed through REST.
 * 
 * @author Markus Knittig
 * @author Steffen Pingel
 * @author Eike Stepper
 */
public class RestfulHudsonClient {

	public enum BuildId {
		LAST(-1, "lastBuild"), LAST_FAILED(-5, "lastFailedBuild"), LAST_STABLE(-2, "lastStableBuild"), LAST_SUCCESSFUL(
				-3, "lastSuccessfulBuild"), LAST_UNSTABLE(-4, "lastUnstableBuild");

		private HudsonModelBuild build;

		private final int id;

		private final String url;

		BuildId(int id, String url) {
			this.id = id;
			this.url = url;
			this.build = new HudsonModelBuild();
			this.build.setNumber(id);
		}

		public HudsonModelBuild getBuild() {
			return build;
		}

	};

	private static final String URL_API = "/api/xml"; //$NON-NLS-1$

	private AbstractConfigurationCache<HudsonConfiguration> cache;

	private final CommonHttpClient client;

	public RestfulHudsonClient(AbstractWebLocation location, HudsonConfigurationCache cache) {
		client = new CommonHttpClient(location);
		client.getHttpClient().getParams().setAuthenticationPreemptive(true);
		setCache(cache);
	}

	protected void checkResponse(CommonHttpMethod method) throws HudsonException {
		checkResponse(method, HttpStatus.SC_OK);
	}

	protected void checkResponse(CommonHttpMethod method, int expected) throws HudsonException {
		int statusCode = method.getStatusCode();
		if (statusCode != expected) {
			if (statusCode == HttpStatus.SC_NOT_FOUND) {
				throw new HudsonResourceNotFoundException(NLS.bind("Requested resource ''{0}'' does not exist",
						method.getPath()));
			}
			throw new HudsonException(NLS.bind("Unexpected response from Hudson server for ''{0}'': {1}",
					method.getPath(), HttpStatus.getStatusText(statusCode)));
		}
	}

	public List<HudsonModelRun> getBuilds(final HudsonModelJob job, final IOperationMonitor monitor)
			throws HudsonException {
		return new HudsonOperation<List<HudsonModelRun>>(client) {
			@Override
			public List<HudsonModelRun> execute() throws IOException, HudsonException, JAXBException {
				String url = HudsonUrl.create(getJobUrl(job))
						.depth(1)
						.tree("builds[number,url,building,result,duration,timestamp,actions[causes[shortDescription],failCount,totalCount,skipCount]]")
						.toUrl();
				CommonHttpMethod method = createGetMethod(url);
				try {
					execute(method, monitor);
					checkResponse(method);
					InputStream in = method.getResponseBodyAsStream(monitor);

					HudsonModelProject project = unmarshal(parse(in, url), HudsonModelProject.class);
					return project.getBuild();
				} finally {
					method.releaseConnection(monitor);
				}
			}
		}.run();
	}

	public HudsonModelBuild getBuild(final HudsonModelJob job, final HudsonModelRun build,
			final IOperationMonitor monitor) throws HudsonException {
		return new HudsonOperation<HudsonModelBuild>(client) {
			@Override
			public HudsonModelBuild execute() throws IOException, HudsonException, JAXBException {
				String url = getBuildUrl(job, build) + URL_API;
				CommonHttpMethod method = createGetMethod(url);
				try {
					execute(method, monitor);
					checkResponse(method);
					InputStream in = method.getResponseBodyAsStream(monitor);
					HudsonModelBuild hudsonBuild = unmarshal(parse(in, url), HudsonModelBuild.class);
					return hudsonBuild;
				} finally {
					method.releaseConnection(monitor);
				}
			}
		}.run();
	}

	public HudsonTestReport getTestReport(final HudsonModelJob job, final HudsonModelRun build,
			final IOperationMonitor monitor) throws HudsonException {
		return new HudsonOperation<HudsonTestReport>(client) {
			@Override
			public HudsonTestReport execute() throws IOException, HudsonException, JAXBException {
				//				String url = HudsonUrl.create(getBuildUrl(job, build) + "/testReport" + URL_API).exclude(
				//						"/testResult/suite/case/stdout").exclude("/testResult/suite/case/stderr").toUrl();
				// need to scope retrieved data due to http://issues.hudson-ci.org/browse/HUDSON-7399
				String resultTree = "duration,failCount,passCount,skipCount,suites[cases[className,duration,errorDetails,errorStackTrace,failedSince,name,skipped,status],duration,name,stderr,stdout]";
				String aggregatedTree = "failCount,skipCount,totalCount,childReports[child[number,url],result["
						+ resultTree + "]]";
				String url = HudsonUrl.create(getBuildUrl(job, build) + "/testReport")
						.tree(resultTree + "," + aggregatedTree)
						.toUrl();
				CommonHttpMethod method = createGetMethod(url);
				try {
					execute(method, monitor);
					checkResponse(method);
					InputStream in = method.getResponseBodyAsStream(monitor);
					Element element = parse(in, url);
					if ("surefireAggregatedReport".equals(element.getNodeName())) {
						HudsonMavenReportersSurefireAggregatedReport report = unmarshal(element,
								HudsonMavenReportersSurefireAggregatedReport.class);
						// unmarshal nested test results
						for (HudsonTasksTestAggregatedTestResultActionChildReport child : report.getChildReport()) {
							child.setResult(RestfulHudsonClient.unmarshal((Node) child.getResult(),
									HudsonTasksJunitTestResult.class));
						}
						return new HudsonTestReport(report);
					}
					return new HudsonTestReport(unmarshal(element, HudsonTasksJunitTestResult.class));
				} finally {
					method.releaseConnection(monitor);
				}
			}
		}.run();
	}

	protected String getBuildUrl(final HudsonModelJob job, final HudsonModelRun build) throws HudsonException {
		if (build.getNumber() == -1) {
			return getJobUrl(job) + "/lastBuild";
		} else {
			return getJobUrl(job) + "/" + build.getNumber();
		}
	}

	public String getArtifactUrl(final HudsonModelJob job, final HudsonModelRun build, HudsonModelRunArtifact artifact)
			throws HudsonException {
		return getBuildUrl(job, build) + "/artifact/" + artifact.getRelativePath();
	}

	public AbstractConfigurationCache<HudsonConfiguration> getCache() {
		return cache;
	}

	public HudsonConfiguration getConfiguration() {
		return getCache().getConfiguration(client.getLocation().getUrl());
	}

	public Reader getConsole(final HudsonModelJob job, final HudsonModelBuild hudsonBuild,
			final IOperationMonitor monitor) throws HudsonException {
		return new HudsonOperation<Reader>(client) {
			@Override
			public Reader execute() throws IOException, HudsonException {
				CommonHttpMethod method = createGetMethod(getBuildUrl(job, hudsonBuild) + "/consoleText");
				execute(method, monitor);
				checkResponse(method);
				String charSet = method.getResponseCharSet();
				if (charSet == null) {
					charSet = "UTF-8";
				}
				return new InputStreamReader(method.getResponseBodyAsStream(monitor), charSet);
			}
		}.run();
	}

	private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder();
	}

	public List<HudsonModelJob> getJobs(final List<String> ids, final IOperationMonitor monitor) throws HudsonException {
		if (ids != null && ids.isEmpty()) {
			return Collections.emptyList();
		}

		return new HudsonOperation<List<HudsonModelJob>>(client) {
			@Override
			public List<HudsonModelJob> execute() throws IOException, HudsonException, JAXBException {
				String url = HudsonUrl.create(client.getLocation().getUrl())
						.depth(1)
						.include("/hudson/job")
						.match("name", ids)
						.exclude("/hudson/job/build")
						.toUrl();
				CommonHttpMethod method = createGetMethod(url);
				try {
					execute(method, monitor);
					checkResponse(method);
					InputStream in = method.getResponseBodyAsStream(monitor);

					Map<String, String> jobNameById = new HashMap<String, String>();

					HudsonModelHudson hudson = unmarshal(parse(in, url), HudsonModelHudson.class);

					List<HudsonModelJob> buildPlans = new ArrayList<HudsonModelJob>();
					List<Object> jobsNodes = hudson.getJob();
					for (Object jobNode : jobsNodes) {
						Node node = (Node) jobNode;
						HudsonModelJob job = unmarshal(node, HudsonModelJob.class);
						if (job.getDisplayName() != null && job.getDisplayName().length() > 0) {
							jobNameById.put(job.getName(), job.getDisplayName());
						} else {
							jobNameById.put(job.getName(), job.getName());
						}
						buildPlans.add(job);
					}

					if (ids == null) {
						// update list of known jobs if all jobs were retrieved
						HudsonConfiguration configuration = new HudsonConfiguration();
						configuration.jobNameById = jobNameById;
						setConfiguration(configuration);
					}

					return buildPlans;
				} finally {
					method.releaseConnection(monitor);
				}
			}
		}.run();
	}

	String getJobUrl(HudsonModelJob job) throws HudsonException {
		String encodedJobname = "";
		try {
			encodedJobname = new URI(null, job.getName(), null).toASCIIString();
		} catch (URISyntaxException e) {
			throw new HudsonException(e);
		}
		return client.getLocation().getUrl() + "/job/" + encodedJobname;
	}

	Element parse(InputStream in, String url) throws HudsonException {
		try {
			return getDocumentBuilder().parse(in).getDocumentElement();
		} catch (SAXException e) {
			throw new HudsonException(NLS.bind("Failed to parse response from {0}", url), e);
		} catch (Exception e) {
			throw new HudsonException(NLS.bind("Failed to parse response from {0}", url), e);
		}
	}

	public Document getJobConfig(final HudsonModelJob job, final IOperationMonitor monitor) throws HudsonException {
		return new HudsonOperation<Document>(client) {
			@Override
			public Document execute() throws IOException, HudsonException, JAXBException {
				CommonHttpMethod method = createGetMethod(getJobUrl(job) + "/config.xml");
				try {
					execute(method, monitor);
					checkResponse(method);

					InputStream in = method.getResponseBodyAsStream(monitor);
					DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					return builder.parse(in); // TODO Enhance progress monitoring
				} catch (ParserConfigurationException e) {
					throw new HudsonException(e);
				} catch (SAXException e) {
					throw new HudsonException(e);
				} finally {
					method.releaseConnection(monitor);
				}
			}
		}.run();
	}

	private class Parameters {

		private NameValue[] parameter;
	}

	private static class NameValue {

		@SuppressWarnings("unused")
		private Object name;

		@SuppressWarnings("unused")
		private Object value;

	}

	public void runBuild(final HudsonModelJob job, final Map<String, String> parameters, final IOperationMonitor monitor)
			throws HudsonException {
		new HudsonOperation<Object>(client) {
			@Override
			public Object execute() throws IOException, HudsonException {
				CommonPostMethod method = (CommonPostMethod) createPostMethod(getJobUrl(job) + "/build");
				method.setFollowRedirects(false);
				method.setDoAuthentication(true);
				if (parameters != null) {
					Parameters params = new Parameters();
					params.parameter = new NameValue[parameters.size()];

					int i = 0;
					for (Entry<String, String> entry : parameters.entrySet()) {
						method.addParameter(new NameValuePair("name", entry.getKey()));
						String value = entry.getValue();
						if (value != null) {
							method.addParameter(new NameValuePair("value", value));
						}

						NameValue param = new NameValue();
						param.name = entry.getKey();
						param.value = entry.getValue();
						params.parameter[i++] = param;
					}
					method.addParameter(new NameValuePair("json", new Gson().toJson(params)));
					method.addParameter(new NameValuePair("Submit", "Build"));
				}

				try {
					execute(method, monitor);
					checkResponse(method, 302);
					return null;
				} finally {
					method.releaseConnection(monitor);
				}
			}
		}.run();
	}

	public void setCache(AbstractConfigurationCache<HudsonConfiguration> cache) {
		Assert.isNotNull(cache);
		this.cache = cache;
	}

	protected void setConfiguration(HudsonConfiguration configuration) {
		getCache().setConfiguration(client.getLocation().getUrl(), configuration);
	}

	public static <T> T unmarshal(Node node, Class<T> clazz) throws JAXBException {
		JAXBContext ctx;
		try {
			ctx = JAXBContext.newInstance(clazz);
		} catch (JAXBException e) {
			// fails on Java 5, see bug 325176
			// instantiate com.sun.xml.bind implementation which is an optional dependency
			// use reflection to avoid compile time dependency, see bug 344198 
			//JAXBContext ctx = new com.sun.xml.bind.v2.runtime.JAXBContextImpl.JAXBContextBuilder().setClasses(new Class[] { clazz }).build();
			try {
				Class<?> contextBuilderClass = Class.forName("com.sun.xml.bind.v2.runtime.JAXBContextImpl$JAXBContextBuilder");
				Object obj = contextBuilderClass.newInstance();
				Method method = contextBuilderClass.getDeclaredMethod("setClasses", Class[].class);
				obj = method.invoke(obj, new Object[] { new Class[] { clazz } });
				method = contextBuilderClass.getDeclaredMethod("build");
				ctx = (JAXBContext) method.invoke(obj);
			} catch (Exception e2) {
				throw new JAXBException(e2);
			}
		}
		Unmarshaller unmarshaller = ctx.createUnmarshaller();

		JAXBElement<T> hudsonElement = unmarshaller.unmarshal(node, clazz);
		return hudsonElement.getValue();
	}

	public HudsonServerInfo validate(final IOperationMonitor monitor) throws HudsonException {
		return new HudsonOperation<HudsonServerInfo>(client) {
			@Override
			public HudsonServerInfo execute() throws IOException, HudsonException {
				CommonHttpMethod method = createHeadMethod(client.getLocation().getUrl());
				try {
					execute(method, monitor);
					checkResponse(method);
					Header header = method.getResponseHeader("X-Hudson"); //$NON-NLS-1$
					if (header == null) {
						throw new HudsonException(NLS.bind("{0} does not appear to be a Hudson instance",
								client.getLocation().getUrl()));
					}
					HudsonServerInfo info = new HudsonServerInfo(header.getValue());
					return info;
				} finally {
					method.releaseConnection(monitor);
				}
			}
		}.run();
	}

}