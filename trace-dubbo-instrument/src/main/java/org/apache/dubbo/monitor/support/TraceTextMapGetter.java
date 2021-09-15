package org.apache.dubbo.monitor.support;

import io.opentelemetry.context.propagation.TextMapGetter;

import org.apache.dubbo.rpc.RpcContext;

public class TraceTextMapGetter implements TextMapGetter<RpcContext> {
    @Override
    public String get(RpcContext carrier, String key) {
        String value = carrier.getAttachment(key); 
        return value;
    }

    @Override
    public Iterable<String> keys(RpcContext carrier) {
        return carrier.getAttachments().keySet();
    } 
}