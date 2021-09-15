package org.apache.rocketmq.client.consumer.listener;

import io.opentelemetry.context.propagation.TextMapGetter;

import org.apache.rocketmq.common.message.MessageExt;

public class TraceTextMapGetter implements TextMapGetter<MessageExt> {
    @Override
    public Iterable<String> keys(MessageExt carrier) {
        return carrier.getProperties().keySet();
    }

    @Override
    public String get(MessageExt carrier, String key) {
        String value = carrier.getUserProperty(key); 
        return value;
    }
}