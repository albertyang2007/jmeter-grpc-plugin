package org.albertyang2007.grpc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.MsgReply;
import io.grpc.examples.helloworld.MsgRequest;
import io.grpc.stub.StreamObserver;

public class GreeterGrpcServer {
	private static final Logger logger = LoggerFactory.getLogger(GreeterGrpcServer.class.getName());

	private Server server;

	private void start() throws IOException {
		/* The port on which the server should run */
		int port = 50051;
		server = ServerBuilder.forPort(port).addService(new GreeterImpl()).build().start();
		logger.info("Server started, listening on " + port);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				// Use stderr here since the logger may have been reset by its
				// JVM shutdown hook.
				System.err.println("*** shutting down gRPC server since JVM is shutting down");
				GreeterGrpcServer.this.stop();
				System.err.println("*** server shut down");
			}
		});
	}

	private void stop() {
		if (server != null) {
			server.shutdown();
		}
	}

	/**
	 * Await termination on the main thread since the grpc library uses daemon
	 * threads.
	 */
	private void blockUntilShutdown() throws InterruptedException {
		if (server != null) {
			server.awaitTermination();
		}
	}

	/**
	 * Main launches the server from the command line.
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		final GreeterGrpcServer server = new GreeterGrpcServer();
		server.start();
		server.blockUntilShutdown();
	}

	static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
		@Override
		public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
			HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}

		@Override
		public void sendMessage(MsgRequest req, StreamObserver<MsgReply> responseObserver) {
			MsgReply reply = MsgReply.newBuilder().setId(123).setResult(req.toString()).build();
			//try{
			//	logger.info("sleep 1 seconds");
			//	Thread.sleep(1000);
			//}catch(Exception e){
			//	e.printStackTrace();
			//}
			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}
	}
}
