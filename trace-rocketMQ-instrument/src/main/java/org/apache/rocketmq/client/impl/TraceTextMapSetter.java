package org.apache.rocketmq.client.impl;

import io.opentelemetry.context.propagation.TextMapSetter;

import static org.apache.rocketmq.common.message.MessageDecoder.NAME_VALUE_SEPARATOR;
import static org.apache.rocketmq.common.message.MessageDecoder.PROPERTY_SEPARATOR;

public class TraceTextMapSetter implements TextMapSetter<StringBuilder> {
    @Override
    public void set(StringBuilder carrier, String key, String value) {
        if (value != null && !value.equals("")) { 
            carrier.append(key);
            carrier.append(NAME_VALUE_SEPARATOR);
            carrier.append(value);
            carrier.append(PROPERTY_SEPARATOR);
        }
    }
}
