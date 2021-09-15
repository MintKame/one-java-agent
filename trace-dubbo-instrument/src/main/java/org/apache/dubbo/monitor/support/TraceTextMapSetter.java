package org.apache.dubbo.monitor.support;

import io.opentelemetry.context.propagation.TextMapSetter;

import org.apache.dubbo.rpc.RpcContext;

public class TraceTextMapSetter implements TextMapSetter<RpcContext> {
    @Override
    public void set(RpcContext carrier, String key, String value) { 
        carrier.setAttachment(key, value); 
    }
}
