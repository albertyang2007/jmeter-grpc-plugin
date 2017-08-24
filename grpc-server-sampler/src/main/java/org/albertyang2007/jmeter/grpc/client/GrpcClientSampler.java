package org.albertyang2007.jmeter.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractStub;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.albertyang2007.jmeter.grpc.compiler.DynamicCompiler;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Message;

public class GrpcClientSampler extends AbstractSampler implements TestBean {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggingManager.getLoggerForClass();
	private String hostname = "localhost";
	private int port = 50051;
	private boolean asyncCall = true;
	private String packageN = "io.grpc.examples.helloworld";
	private String service = "Greeter";
	private String method = "sendMessage";
	private String request = "MsgRequest";
	private String requestBuilderCode = requestBuilderExampleCode();

	private ManagedChannel channel = null;
	private AbstractStub<?> blockingStub = null;
	private AbstractStub<?> futureStub = null;
	private DynamicCompiler dynaCompiler = new DynamicCompiler();

	private final ClassLoader classLoader = GrpcClientSampler.class.getClassLoader();

	public GrpcClientSampler() {
		log.info("Created " + this);
		setName("Grpc Client Sampler");
	}

	private void initGrpcClient() {
		log.info("initGrpcClient");
		try {
			this.channel = ManagedChannelBuilder.forAddress(this.getHostname(), this.getPort()).usePlaintext(true)
					.build();
			Class<?> serviceGrpcClass = classLoader.loadClass(this.getPackageN() + "." + this.getService() + "Grpc");
			Method newBlockingStubMethod = serviceGrpcClass.getMethod("newBlockingStub", io.grpc.Channel.class);
			this.blockingStub = (AbstractStub<?>) newBlockingStubMethod.invoke(null, channel);

			Method newFutureStubMethod = serviceGrpcClass.getMethod("newFutureStub", io.grpc.Channel.class);
			this.futureStub = (AbstractStub<?>) newFutureStubMethod.invoke(null, channel);
		} catch (Exception e) {
			log.error("Exception", e);
		}
	}

	private void shutdown() throws InterruptedException {
		this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}

	@Override
	public SampleResult sample(Entry entry) {
		log.info("sample");

		if (this.channel == null) {
			this.initGrpcClient();
		}

		if (this.isAsyncCall()) {
			return this.asyncCallMethod();
		} else {
			return this.syncCallMethod();
		}
	}

	/** Sync Call to server. */
	public SampleResult syncCallMethod() {
		log.info("syncCallMethod");

		long start = System.currentTimeMillis();
		SampleResult result = new SampleResult();
		result.setSampleLabel(getName());
		result.sampleStart();

		try {
			Method apiMethod = blockingStub.getClass().getMethod(this.getMethod(),
					Class.forName(this.getRequest()));

			// dymamic compile and build the request message
			Object obj = dynaCompiler.buildRequest("RequestFactory", this.getRequestBuilderCode());
			Method method = obj.getClass().getMethod("buildRequest", null);
			Object req = method.invoke(obj, null);
			// end

			Object resp = apiMethod.invoke(blockingStub, req);
			// TODO: get the response message from resp
			log.info("syncCallMethod response=" + resp.toString());

			result.setSamplerData(getHostname() + ":" + getPort() + "\n" + this.getService() + "#" + this.getMethod()
					+ "\nRequestData:\n" + req.toString());
			result.setResponseData(resp.toString());
			result.setResponseCodeOK();
			result.setResponseMessage("OK");
			result.setResponseCode("200");

		} catch (Exception e) {
			log.error("syncCallMethod Exception", e);
			result.setResponseData(e.getMessage());
			result.setResponseMessage("Failure");
			result.setResponseCode("500");
		} finally {
			result.setLatency(System.currentTimeMillis() - start);
			result.sampleEnd();
		}

		return result;
	}

	/** aSync Call to server. */
	public SampleResult asyncCallMethod() {
		log.info("asyncCallMethod");

		final long start = System.currentTimeMillis();
		final SampleResult result = new SampleResult();
		result.setSampleLabel(getName());
		result.sampleStart();

		try {
			final CountDownLatch countDownLatch = new CountDownLatch(1);

			Method apiMethod = futureStub.getClass().getMethod(this.getMethod(),
					Class.forName(this.getRequest()));

			// dymamic compile and build the request message
			Object obj = dynaCompiler.buildRequest("RequestFactory", this.getRequestBuilderCode());
			Method method = obj.getClass().getMethod("buildRequest", null);
			Object req = method.invoke(obj, null);
			// end

			result.setSamplerData(getHostname() + ":" + getPort() + "\n" + this.getService() + "#" + this.getMethod()
					+ "\nRequestData:\n" + req.toString());

			final ListenableFuture<?> reply = (ListenableFuture<?>) apiMethod.invoke(futureStub, req);

			reply.addListener(new Runnable() {
				@Override
				public void run() {
					try {
						final Message resp = (Message) reply.get();
						log.info("asyncCallMethod response=" + resp.toString());
						result.setResponseData(resp.toString());
						result.setResponseMessage(resp.toString());
						result.setSuccessful(true);
						result.setResponseMessage("OK");
						result.setResponseCode("200");
					} catch (Exception e) {
						log.error("asyncCallMethod InterruptedException", e);
						result.setResponseData(e.getMessage());
						result.setResponseMessage(e.getMessage());
						result.setSuccessful(false);
						result.setResponseCode("500");
					} finally {
						countDownLatch.countDown();
						log.info("end2");
					}
				}
			}, MoreExecutors.newDirectExecutorService());

			try {
				countDownLatch.await();
			} catch (Exception e) {
				log.error("CountDownLatch Exception", e);
				throw Throwables.propagate(e);
			}

		} catch (Exception e) {
			log.error("asyncCallMethod Exception", e);
			result.setResponseData(e.getMessage());
			result.setResponseMessage(e.getMessage());
			result.setResponseCode("500");
			result.setSuccessful(false);
		} finally {
			result.setLatency(System.currentTimeMillis() - start);
			result.sampleEnd();
			log.info("end1");
		}

		return result;
	}

	public String getHostname() {
		return this.hostname;
	}

	public int getPort() {
		return this.port;
	}

	public String getMethod() {
		// TODO: must convert the first character to LowerCase

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

	public boolean isAsyncCall() {
		return asyncCall;
	}

	public void setAsyncCall(boolean asyncCall) {
		this.asyncCall = asyncCall;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getPackageN() {
		return packageN;
	}

	public void setPackageN(String packageN) {
		this.packageN = packageN;
	}

	public String getRequestBuilderCode() {
		return requestBuilderCode;
	}

	public void setRequestBuilderCode(String requestBuilderCode) {
		this.requestBuilderCode = requestBuilderCode;
	}

	public static String requestBuilderExampleCode() {
		StringBuilder src = new StringBuilder();
		src.append("import io.grpc.examples.helloworld.MsgRequest;\n");
		src.append("import com.google.protobuf.Message;\n");
		src.append("public class RequestFactory {\n");
		src.append("public Message buildRequest() {\n");
		src.append("MsgRequest request = MsgRequest.newBuilder().setName(\"Name\").setTimeout(10000).build();\n");
		src.append("return request;\n");
		src.append("    }\n");
		src.append("}\n");
		return src.toString();
	}
}
