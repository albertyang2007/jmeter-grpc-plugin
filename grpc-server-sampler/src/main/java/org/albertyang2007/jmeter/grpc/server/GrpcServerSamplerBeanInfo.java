package org.albertyang2007.jmeter.grpc.server;

import java.beans.PropertyDescriptor;

import org.apache.jmeter.testbeans.BeanInfoSupport;

public class GrpcServerSamplerBeanInfo extends BeanInfoSupport{
	public GrpcServerSamplerBeanInfo() {
		super(GrpcServerSampler.class);

		createPropertyGroup("Server", new String[]{"hostname", "port"});
		createPropertyGroup("Execute", new String[]{"method", "request"});

		PropertyDescriptor localPropertyDescriptor = property("hostname");
		localPropertyDescriptor.setValue("notUndefined", Boolean.TRUE);
		localPropertyDescriptor.setValue("default", "localhost");

		localPropertyDescriptor = property("port");
		localPropertyDescriptor.setValue("notUndefined", Boolean.TRUE);
		localPropertyDescriptor.setValue("default", new Integer(50051));

		localPropertyDescriptor = property("method");
		localPropertyDescriptor.setValue("notUndefined", Boolean.TRUE);
		localPropertyDescriptor.setValue("default", "SayHello");

		localPropertyDescriptor = property("request");
		localPropertyDescriptor.setValue("notUndefined", Boolean.TRUE);
		localPropertyDescriptor.setValue("default", "{}");
	}

}
