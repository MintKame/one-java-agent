package org.apache.catalina.core;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NetTransportValues.IP_TCP;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;


import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.agent.inst.InstrumentApi;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.oneagent.Java8BytecodeBridge;
import io.opentelemetry.oneagent.TraceConfiguration;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

@Instrument(Class = "org.apache.catalina.core.StandardHostValve")
public abstract class StandardHostValve{

    public final void invoke(Request req, Response resp) throws Throwable {  
        
        Tracer tracer = TraceConfiguration.getTracer(false); 

        // context propagation
        TextMapGetter<Request> getter = new TraceTextMapGetter();  
        Context extractedContext = TraceConfiguration.getTextMapPropagator()
                .extract(Java8BytecodeBridge.currentContext(), req, getter); 
        
        // 创建span 
        Span span = tracer.spanBuilder(req.getRequestURI().toString()) 
            .setSpanKind(SpanKind.SERVER)
            .setParent(extractedContext) 
            .startSpan();    

        // 设置attributes   
        span.setAttribute(SemanticAttributes.NET_TRANSPORT, IP_TCP);  
        span.setAttribute(SemanticAttributes.HTTP_METHOD, req.getMethod());   
        span.setAttribute(SemanticAttributes.HTTP_URL, req.getRequestURI().toString());   
        
        // Set the context with the current span
        Scope scope = null;
        try {
            scope = span.makeCurrent();

            // invoke origin
            InstrumentApi.invokeOrigin(); 
        } catch (Throwable e) { 
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
            scope.close();
        }
    }

    protected void throwable(Request request, Response response, Throwable throwable) throws Throwable {
        Span span = Java8BytecodeBridge.currentSpan();
        
        if(span != null){
            span.setStatus(StatusCode.ERROR, throwable.getMessage());
        }
    }
};