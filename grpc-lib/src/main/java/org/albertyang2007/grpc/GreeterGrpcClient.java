package org.albertyang2007.grpc;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.MsgReply;
import io.grpc.examples.helloworld.MsgRequest;

public class GreeterGrpcClient {
	private static final Logger logger = LoggerFactory.getLogger(GreeterGrpcClient.class.getName());

	private final ManagedChannel channel;
	private final GreeterGrpc.GreeterBlockingStub blockingStub;
	private final GreeterGrpc.GreeterFutureStub futureStub;

	/**
	 * Construct client connecting to HelloWorld server at {@code host:port}.
	 */
	public GreeterGrpcClient(String host, int port) {
		this(ManagedChannelBuilder.forAddress(host, port)
				// Channels are secure by default (via SSL/TLS). For the example
				// we disable TLS to avoid
				// needing certificates.
				.usePlaintext(true).build());
	}

	/**
	 * Construct client for accessing RouteGuide server using the existing
	 * channel.
	 */
	GreeterGrpcClient(ManagedChannel channel) {
		this.channel = channel;
		blockingStub = GreeterGrpc.newBlockingStub(channel);
		futureStub = GreeterGrpc.newFutureStub(channel);
	}

	public void shutdown() throws InterruptedException {
		channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}

	/** Sync Say hello to server. */
	public void syncSayHello(String name) {
		logger.info("syncSayHello name=" + name);
		HelloRequest request = HelloRequest.newBuilder().setName(name).build();
		HelloReply response;
		try {
			response = blockingStub.sayHello(request);
		} catch (StatusRuntimeException e) {
			logger.error("syncSayHello StatusRuntimeException", e);
			return;
		}
		logger.info("syncSayHello response=" + response.getMessage());
	}

	/** aSync Say hello to server. */
	public void asyncSayHello(String name) {
		logger.info("asyncSayHello name=" + name);
		HelloRequest request = HelloRequest.newBuilder().setName(name).build();
		final ListenableFuture<HelloReply> futureAnswer = futureStub.sayHello(request);
		futureAnswer.addListener(new Runnable() {
			@Override
			public void run() {
				try {
					final HelloReply response = futureAnswer.get();
					logger.info("asyncSayHello response=" + response.getMessage());
				} catch (InterruptedException e) {
					logger.error("asyncSayHello InterruptedException", e);
				} catch (ExecutionException e) {
					logger.error("asyncSayHello ExecutionException", e);
				}
			}
		}, MoreExecutors.newDirectExecutorService());
	}

	/** Sync SendMessage to server. */
	public void syncSendMessage(String name) {
		logger.info("syncSendMessage name=" + name);
		MsgRequest request = MsgRequest.newBuilder().setName("Name").setId(10000).build();
		MsgReply response;
		try {
			response = blockingStub.sendMessage(request);
		} catch (StatusRuntimeException e) {
			logger.error("syncSayHello StatusRuntimeException", e);
			return;
		}
		logger.info("syncSendMessage response=" + response.toString());
	}

	/**
	 * Greet server. If provided, the first element of {@code args} is the name
	 * to use in the greeting.
	 */
	public static void main(String[] args) throws Exception {
		GreeterGrpcClient client = new GreeterGrpcClient("localhost", 50051);
		try {
			client.syncSayHello("Albert");
			client.asyncSayHello("Ben");
		} finally {
			client.shutdown();
		}
	}
}
