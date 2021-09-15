package org.apache.rocketmq.client.producer;

import io.opentelemetry.oneagent.TraceConfiguration;
import io.opentelemetry.oneagent.Java8BytecodeBridge;

import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.agent.inst.InstrumentApi;

@Instrument(Interface = "org.apache.rocketmq.client.producer.SendCallback")
public abstract class SendCallback{
    
    void onSuccess(final SendResult sendResult) throws Throwable {
        // 创建 span
        Tracer tracer = TraceConfiguration.getTracer(true);
        Span span = tracer.spanBuilder("RocketMQ/Producer/Callback")
                .setSpanKind(SpanKind.PRODUCER)
                .setParent(Java8BytecodeBridge.currentContext()) 
                .startSpan();  
        span.setAttribute(SemanticAttributes.MESSAGING_SYSTEM, "rocketMQ");
        
        // Set the context with the current span
        Scope scope = null;
        try {
            scope = span.makeCurrent();

            InstrumentApi.invokeOrigin(); 
            
            SendStatus sendStatus = sendResult.getSendStatus();
            if (sendStatus != SendStatus.SEND_OK) {
                span.setStatus(StatusCode.ERROR,  sendStatus.name());
            }
        } catch(Throwable e){
            span.setStatus(StatusCode.ERROR, e.getMessage()); 
            throw e;
        }  finally { 
            span.end();
            scope.close();
        }
    }

    void onException(final Throwable exception)throws Throwable{
        // 创建 span
        Tracer tracer = TraceConfiguration.getTracer(true);
        Span span = tracer.spanBuilder("RocketMQ/Producer/Callback")
                .setSpanKind(SpanKind.PRODUCER)
                .setParent(Java8BytecodeBridge.currentContext()) 
                .startSpan(); 
        span.setAttribute(SemanticAttributes.MESSAGING_SYSTEM, "rocketMQ"); 
        
        // Set the context with the current span
        Scope scope = null;
        try {
            scope = span.makeCurrent();

            InstrumentApi.invokeOrigin();
            
            span.setStatus(StatusCode.ERROR, exception.getMessage());
            
        } catch(Throwable e){
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        }  finally { 
            span.end();
            scope.close();
        }
    }
};