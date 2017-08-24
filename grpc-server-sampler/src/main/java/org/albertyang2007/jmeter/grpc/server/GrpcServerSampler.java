package org.albertyang2007.jmeter.grpc.server;

import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

public class GrpcServerSampler extends AbstractSampler implements TestBean {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggingManager.getLoggerForClass();
	private String hostname = "localhost";
	private int port = 50051;
	private String method ="SayHello";
	private String request = "{}";

	public GrpcServerSampler() {
		log.info("Created " + this);
		setName("Grpc Server Sampler");
	}

	@Override
	public SampleResult sample(Entry entry) {
		log.info("sample entry=" + entry.toString());
		long start = System.currentTimeMillis();
		SampleResult result = new SampleResult();
		result.setSampleLabel(getName());
		result.setSamplerData("Host: " + getHostname() + " Port: " + getPort());
		result.sampleStart();

		result.setSamplerData("SamplerDataContent");
		result.setResponseCodeOK();
		result.setResponseMessage("OK");
		result.setLatency(System.currentTimeMillis() - start);
		result.sampleEnd();

		log.info("sample result=" + result.toString());
		return result;
	}

	public String getHostname() {
		return this.hostname;
	}

	public int getPort() {
		return this.port;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
