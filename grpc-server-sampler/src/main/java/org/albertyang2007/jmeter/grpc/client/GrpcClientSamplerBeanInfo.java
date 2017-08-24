package org.albertyang2007.jmeter.grpc.client;

import java.beans.PropertyDescriptor;

import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.gui.TypeEditor;

public class GrpcClientSamplerBeanInfo extends BeanInfoSupport{
	public GrpcClientSamplerBeanInfo() {
		super(GrpcClientSampler.class);

		createPropertyGroup("Server", new String[]{"hostname", "port"});
		createPropertyGroup("Service", new String[]{"packageN", "service", "asyncCall"});
		createPropertyGroup("Execute", new String[]{"method", "request","requestBuilderCode"});

		PropertyDescriptor localPropertyDescriptor = property("hostname");
		localPropertyDescriptor.setValue("notUndefined", Boolean.TRUE);
		localPropertyDescriptor.setValue("default", "localhost");

		localPropertyDescriptor = property("port");
		localPropertyDescriptor.setValue("notUndefined", Boolean.TRUE);
		localPropertyDescriptor.setValue("default", new Integer(50051));

		localPropertyDescriptor = property("asyncCall");
		localPropertyDescriptor.setValue("notUndefined", Boolean.TRUE);
		localPropertyDescriptor.setValue("default", Boolean.TRUE);
		
		localPropertyDescriptor = property("packageN");
		localPropertyDescriptor.setValue("notUndefined", Boolean.TRUE);
		localPropertyDescriptor.setValue("default", "io.grpc.examples.helloworld");

		localPropertyDescriptor = property("service");
		localPropertyDescriptor.setValue("notUndefined", Boolean.TRUE);
		localPropertyDescriptor.setValue("default", "Greeter");
		
		localPropertyDescriptor = property("method");
		localPropertyDescriptor.setValue("notUndefined", Boolean.TRUE);
		localPropertyDescriptor.setValue("default", "SayHello");
		
		localPropertyDescriptor = property("request");
		localPropertyDescriptor.setValue("notUndefined", Boolean.TRUE);
		localPropertyDescriptor.setValue("default", "HelloRequest");

		localPropertyDescriptor = property("requestBuilderCode", TypeEditor.TextAreaEditor);
		localPropertyDescriptor.setValue("notUndefined", Boolean.TRUE);
		localPropertyDescriptor.setValue("textLanguage", "java");
		localPropertyDescriptor.setValue("default", "");
	}

}
