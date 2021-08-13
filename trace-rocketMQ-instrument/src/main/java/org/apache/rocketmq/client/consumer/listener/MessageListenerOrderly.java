package org.apache.rocketmq.client.consumer.listener;

import com.alibaba.oneagent.trace.configuration.TraceConfiguration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.agent.inst.InstrumentApi;

import java.util.List;
import java.lang.reflect.Method;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.common.message.MessageExt;

@Instrument(Interface = "org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly")
public abstract class MessageListenerOrderly{

    ConsumeOrderlyStatus consumeMessage(final List<MessageExt> msgs,
    final ConsumeOrderlyContext context)throws Throwable{

        if(msgs == null || msgs.isEmpty()){
            return InstrumentApi.invokeOrigin();
        }
        MessageExt msg = msgs.get(0);
        
        // 创建 span
        Tracer tracer = TraceConfiguration.getTracer(); 
        Span span = tracer.spanBuilder("RocketMQ/" + msg.getTopic() + "/Consumer")
                .setSpanKind(SpanKind.CONSUMER)
                .setParent(TraceConfiguration.getContext()) 
                .startSpan();  
        span.setAttribute(SemanticAttributes.MESSAGING_SYSTEM, "rocketMQ");
        span.setAttribute(SemanticAttributes.MESSAGING_DESTINATION_KIND, "topic");
        span.setAttribute(SemanticAttributes.MESSAGING_DESTINATION, msg.getTopic());
    
        // Set the context with the current span
        Scope scope = null;
        try {
            scope = TraceConfiguration.getContext().makeCurrent();

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