package org.albertyang2007.jmeter.grpc.compiler;

import io.grpc.examples.helloworld.MsgRequest;
import com.google.protobuf.Message;

public class RequestFactory {
	public Message buildRequest() {
		MsgRequest request = MsgRequest.newBuilder().setName("NameValue").setId(10000).build();
		return request;
	}
}
