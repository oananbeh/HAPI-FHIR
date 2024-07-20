package ca.uhn.fhir.rest.server;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.test.utilities.HttpClientExtension;
import ca.uhn.fhir.test.utilities.server.RestfulServerExtension;
import ca.uhn.fhir.util.TestUtil;
import ca.uhn.fhir.util.UrlUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ServerSearchDstu2Test {

	private static final FhirContext ourCtx = FhirContext.forDstu2Cached();
	private static String ourLastMethod;
	private static StringParam ourLastRef;
	private static ReferenceParam ourLastRef2;
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ServerSearchDstu2Test.class);

	@RegisterExtension
	public static final RestfulServerExtension ourServer  = new RestfulServerExtension(ourCtx)
		.setDefaultResponseEncoding(EncodingEnum.XML)
		.registerProvider(new DummyPatientResourceProvider())
		.withPagingProvider(new FifoMemoryPagingProvider(100))
		.setDefaultPrettyPrint(false);

	@RegisterExtension
	public static final HttpClientExtension ourClient = new HttpClientExtension();

	@BeforeEach
	public void before() {
		ourLastMethod = null;
		ourLastRef = null;
		ourLastRef2 = null;
	}

	@Test
	public void testReferenceParamMissingFalse() throws Exception {
		HttpGet httpGet = new HttpGet(ourServer.getBaseUrl() + "/?param3:missing=false");
		HttpResponse status = ourClient.execute(httpGet);
		String responseContent = IOUtils.toString(status.getEntity().getContent());
		IOUtils.closeQuietly(status.getEntity().getContent());
		ourLog.info(responseContent);
		assertEquals("searchParam3", ourLastMethod);
		assertEquals(Boolean.FALSE, ourLastRef2.getMissing());
	}

	@Test
	public void testReferenceParamMissingTrue() throws Exception {
		HttpGet httpGet = new HttpGet(ourServer.getBaseUrl() + "/?param3:missing=true");
		HttpResponse status = ourClient.execute(httpGet);
		String responseContent = IOUtils.toString(status.getEntity().getContent());
		IOUtils.closeQuietly(status.getEntity().getContent());
		ourLog.info(responseContent);
		assertEquals("searchParam3", ourLastMethod);
		assertEquals(Boolean.TRUE, ourLastRef2.getMissing());
	}

	@Test
	public void testSearchParam1() throws Exception {
		HttpGet httpGet = new HttpGet(ourServer.getBaseUrl() + "/?param1=param1value");
		HttpResponse status = ourClient.execute(httpGet);
		String responseContent = IOUtils.toString(status.getEntity().getContent());
		IOUtils.closeQuietly(status.getEntity().getContent());
		ourLog.info(responseContent);
		assertEquals("searchParam1", ourLastMethod);
		assertEquals("param1value", ourLastRef.getValue());
	}

	@Test
	public void testSearchParam2() throws Exception {
		HttpGet httpGet = new HttpGet(ourServer.getBaseUrl() + "/?param2=param2value&foo=bar");
		HttpResponse status = ourClient.execute(httpGet);
		String responseContent = IOUtils.toString(status.getEntity().getContent());
		IOUtils.closeQuietly(status.getEntity().getContent());
		ourLog.info(responseContent);
		assertEquals("searchParam2", ourLastMethod);
		assertEquals("param2value", ourLastRef.getValue());
	}

	@Test
	public void testSearchParamWithSpace() throws Exception {
		HttpGet httpGet = new HttpGet(ourServer.getBaseUrl() + "/?param2=param+value&foo=bar");
		HttpResponse status = ourClient.execute(httpGet);
		String responseContent = IOUtils.toString(status.getEntity().getContent());
		IOUtils.closeQuietly(status.getEntity().getContent());
		ourLog.info(responseContent);
		assertEquals("searchParam2", ourLastMethod);
		assertEquals("param value", ourLastRef.getValue());
	}

	@Test
	public void testSearchWithEncodedValue() throws Exception {
		HttpGet httpGet = new HttpGet(ourServer.getBaseUrl() + "/?param1=" + UrlUtil.escapeUrlParam("Jernelöv"));
		HttpResponse status = ourClient.execute(httpGet);
		String responseContent = IOUtils.toString(status.getEntity().getContent());
		IOUtils.closeQuietly(status.getEntity().getContent());
		ourLog.info(responseContent);
		assertEquals("searchParam1", ourLastMethod);
		assertEquals("Jernelöv", ourLastRef.getValue());
	}

	@Test
	public void testUnknownSearchParam() throws Exception {
		HttpGet httpGet = new HttpGet(ourServer.getBaseUrl() + "/?foo=bar");
		HttpResponse status = ourClient.execute(httpGet);
		String responseContent = IOUtils.toString(status.getEntity().getContent());
		IOUtils.closeQuietly(status.getEntity().getContent());
		ourLog.info(responseContent);
		assertNull(ourLastMethod);
	}

	@AfterAll
	public static void afterClassClearContext() throws Exception {
		TestUtil.randomizeLocaleAndTimezone();
	}

	public static class DummyPatientResourceProvider {

		@Search(allowUnknownParams=true)
		public List<IBaseResource> searchParam1(
				@RequiredParam(name = "param1") StringParam theParam) {
			ourLastMethod = "searchParam1";
			ourLastRef = theParam;
			
			List<IBaseResource> retVal = new ArrayList<>();
			Patient patient = new Patient();
			patient.setId("123");
			patient.addName().addGiven("GIVEN");
			retVal.add(patient);
			return retVal;
		}

		@Search(allowUnknownParams=true)
		public List<IBaseResource> searchParam2(
				@RequiredParam(name = "param2") StringParam theParam) {
			ourLastMethod = "searchParam2";
			ourLastRef = theParam;
			
			List<IBaseResource> retVal = new ArrayList<>();
			Patient patient = new Patient();
			patient.setId("123");
			patient.addName().addGiven("GIVEN");
			retVal.add(patient);
			return retVal;
		}

		@Search(allowUnknownParams=true)
		public List<IBaseResource> searchParam3(
				@RequiredParam(name = "param3") ReferenceParam theParam) {
			ourLastMethod = "searchParam3";
			ourLastRef2 = theParam;
			
			List<IBaseResource> retVal = new ArrayList<>();
			Patient patient = new Patient();
			patient.setId("123");
			patient.addName().addGiven("GIVEN");
			retVal.add(patient);
			return retVal;
		}

	}

}