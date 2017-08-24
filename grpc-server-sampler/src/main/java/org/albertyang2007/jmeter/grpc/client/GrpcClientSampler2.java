package org.albertyang2007.jmeter.grpc.client;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.albertyang2007.jmeter.grpc.compiler.DynamicCompiler;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Message;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractStub;

public class GrpcClientSampler2 extends AbstractJavaSamplerClient {
	private static final Logger log = LoggingManager.getLoggerForClass();

	private ManagedChannel channel;
	private AbstractStub<?> blockingStub;
	private AbstractStub<?> futureStub;
	private DynamicCompiler dynaCompiler = new DynamicCompiler();

	private final ClassLoader classLoader = GrpcClientSampler2.class.getClassLoader();

	@Override
	public Arguments getDefaultParameters() {
		Arguments defaultParameters = new Arguments();
		defaultParameters.addArgument("hostname", "localhost");
		defaultParameters.addArgument("port", "50051");
		defaultParameters.addArgument("asyncCall", "false");
		defaultParameters.addArgument("packageN", "io.grpc.examples.helloworld");
		defaultParameters.addArgument("service", "Greeter");
		defaultParameters.addArgument("method", "sendMessage");
		defaultParameters.addArgument("request", "MsgRequest");
		defaultParameters.addArgument("requestBuilderCode", requestBuilderExampleCode());
		return defaultParameters;
	}

	@Override
	public void setupTest(JavaSamplerContext context) {
		log.info("setupTest");
		try {
			this.channel = ManagedChannelBuilder
					.forAddress(context.getParameter("hostname"), context.getIntParameter("port")).usePlaintext(true)
					.build();
			Class<?> serviceGrpcClass = classLoader.loadClass(context.getParameter("packageN") + "."
					+ context.getParameter("service") + "Grpc");
			Method newBlockingStubMethod = serviceGrpcClass.getMethod("newBlockingStub", io.grpc.Channel.class);
			this.blockingStub = (AbstractStub<?>) newBlockingStubMethod.invoke(null, channel);

			Method newFutureStubMethod = serviceGrpcClass.getMethod("newFutureStub", io.grpc.Channel.class);
			this.futureStub = (AbstractStub<?>) newFutureStubMethod.invoke(null, channel);
		} catch (Exception e) {
			log.error("Exception", e);
		}
		super.setupTest(context);
	}

	@Override
	public void teardownTest(JavaSamplerContext context) {
		log.info("teardownTest");
		try {
			this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.teardownTest(context);
	}

	@Override
	public SampleResult runTest(JavaSamplerContext context) {
		log.info("sample");
		boolean isAsyncCall = Boolean.parseBoolean(context.getParameter("asyncCall"));
		if (isAsyncCall) {
			return this.asyncCallMethod(context);
		} else {
			return this.syncCallMethod(context);
		}
	}

	/** Sync Call to server. */
	public SampleResult syncCallMethod(JavaSamplerContext context) {
		log.info("syncCallMethod");

		long start = System.currentTimeMillis();
		SampleResult result = new SampleResult();
		result.setSampleLabel(this.getClass().getSimpleName());
		result.sampleStart();

		try {
			Method apiMethod = blockingStub.getClass().getMethod(context.getParameter("method"),
					Class.forName(context.getParameter("request")));

			// dymamic compile and build the request message
			Object obj = dynaCompiler.buildRequest("RequestFactory", context.getParameter("requestBuilderCode"));
			Method method = obj.getClass().getMethod("buildRequest", null);
			Object req = method.invoke(obj, null);
			// end

			Object resp = apiMethod.invoke(blockingStub, req);
			// TODO: get the response message from resp
			log.info("syncCallMethod response=" + resp.toString());

			result.setSamplerData(context.getParameter("hostname") + ":" + context.getParameter("port") + "\n"
					+ context.getParameter("service") + "#" + context.getParameter("method") + "\nRequestData:\n"
					+ req.toString());
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
	public SampleResult asyncCallMethod(JavaSamplerContext context) {
		log.info("asyncCallMethod");

		final long start = System.currentTimeMillis();
		final SampleResult result = new SampleResult();
		result.setSampleLabel(this.getClass().getSimpleName());
		result.sampleStart();

		try {
			final CountDownLatch countDownLatch = new CountDownLatch(1);

			Method apiMethod = futureStub.getClass().getMethod(context.getParameter("method"),
					Class.forName(context.getParameter("request")));

			// dymamic compile and build the request message
			Object obj = dynaCompiler.buildRequest("RequestFactory", context.getParameter("requestBuilderCode"));
			Method method = obj.getClass().getMethod("buildRequest", null);
			Object req = method.invoke(obj, null);
			// end

			result.setSamplerData(context.getParameter("hostname") + ":" + context.getParameter("port") + "\n"
					+ context.getParameter("service") + "#" + context.getParameter("method") + "\nRequestData:\n"
					+ req.toString());

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

	public static String requestBuilderExampleCode() {
		StringBuilder src = new StringBuilder();
		src.append("import io.grpc.examples.helloworld.HelloRequest;\n");
		src.append("import com.google.protobuf.Message;\n");
		src.append("public class RequestFactory {\n");
		src.append("public Message buildRequest() {\n");
		src.append("HelloRequest request = HelloRequest.newBuilder().setName(\"NameValue\").build();\n");
		src.append("return request;\n");
		src.append("    }\n");
		src.append("}\n");
		return src.toString();
	}
}
