package org.apache.catalina.core;

import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.Collections;
import org.apache.catalina.connector.Request;

public class TraceTextMapGetter implements TextMapGetter<Request> {

    @Override
    public Iterable<String> keys(Request carrier) {
        return Collections.list(carrier.getHeaderNames());
    }

    @Override
    public String get(Request carrier, String key) {
        return carrier.getHeader(key);
    }
}
