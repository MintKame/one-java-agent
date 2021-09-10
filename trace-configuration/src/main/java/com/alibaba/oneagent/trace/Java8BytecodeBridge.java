package com.alibaba.oneagent.trace;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;

public final class Java8BytecodeBridge {

    public static Context currentContext() {
        return Context.current();
    }

    public static Span currentSpan() {
        return Span.current();
    }

    public static Span spanFromContext(Context context) {
        return Span.fromContext(context);
    }
    
    private Java8BytecodeBridge() {}
}