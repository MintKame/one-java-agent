package org.apache.rocketmq.client.consumer.listener;

import com.alibaba.oneagent.trace.configuration.TraceConfiguration;
import com.alibaba.oneagent.trace.Java8BytecodeBridge;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.GlobalOpenTelemetry;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.agent.inst.InstrumentApi;

import java.util.List;
import java.util.Map;
import java.lang.reflect.Method; 
import org.apache.rocketmq.common.message.MessageExt;

@Instrument(Interface = "org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly")
public abstract class MessageListenerOrderly{

    ConsumeOrderlyStatus consumeMessage(final List<MessageExt> msgs,
    final ConsumeOrderlyContext context)throws Throwable{

        if(msgs == null || msgs.isEmpty()){
            return InstrumentApi.invokeOrigin();
        }

        // context propagation
        TextMapGetter<MessageExt> getter =
        new TextMapGetter<MessageExt>() {
            @Override
            public Iterable<String> keys(MessageExt carrier) {
                return carrier.getProperties().keySet();
            }

            @Override
            public String get(MessageExt carrier, String key) {
                return carrier.getUserProperty(key);
            }
        }; 

        TextMapPropagator textMapPropagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
        Span span = null;
        Tracer tracer = TraceConfiguration.getTracer();
        if (msgs.size() == 1) {
            MessageExt msg = msgs.get(0);
            Context extractedContext = textMapPropagator.extract(Java8BytecodeBridge.currentContext(), msg, getter);
                
            // 创建 span 
            span = tracer.spanBuilder("RocketMQ/" + msg.getTopic() + "/Consumer")
                    .setSpanKind(SpanKind.CONSUMER)
                    .setParent(extractedContext)
                    .setAttribute(SemanticAttributes.MESSAGING_SYSTEM, "rocketMQ")
                    .setAttribute(SemanticAttributes.MESSAGING_DESTINATION_KIND, "topic")
                    .setAttribute(SemanticAttributes.MESSAGING_DESTINATION, msg.getTopic())
                    .startSpan();  
        } else {
            // 创建 span 
            span = tracer.spanBuilder("multiple_sources receive")
                    .setSpanKind(SpanKind.CONSUMER)
                    .setParent(Java8BytecodeBridge.currentContext())
                    .setAttribute(SemanticAttributes.MESSAGING_SYSTEM, "rocketMQ")
                    .setAttribute(SemanticAttributes.MESSAGING_OPERATION, "receive")
                    .startSpan(); 

            Context rootContext = Java8BytecodeBridge.currentContext().with(span); 

            for (MessageExt message : msgs) {
                Context extractedContext = textMapPropagator.extract(Java8BytecodeBridge.currentContext(), message, getter);

                Span childSpan = tracer.spanBuilder("RocketMQ/" + message.getTopic() + "/Consumer")
                    .setSpanKind(SpanKind.CONSUMER)
                    .setParent(rootContext)
                    .addLink(Java8BytecodeBridge.spanFromContext(extractedContext).getSpanContext())
                    .setAttribute(SemanticAttributes.MESSAGING_SYSTEM, "rocketMQ")
                    .setAttribute(SemanticAttributes.MESSAGING_DESTINATION_KIND, "topic")
                    .setAttribute(SemanticAttributes.MESSAGING_DESTINATION, message.getTopic())
                    .startSpan();    

                childSpan.end();
            }
        } 

       // Set the context with the current span
        Scope scope = null;
        try {
            scope = span.makeCurrent();
            
            // invoke origin
            ConsumeOrderlyStatus status = InstrumentApi.invokeOrigin();
            if (status == ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT) {
                span.setStatus(StatusCode.ERROR, status.name());
            }
            return status;
        } catch(Throwable e){
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        }  finally { 
            span.end();
            scope.close();
        }
    }

};