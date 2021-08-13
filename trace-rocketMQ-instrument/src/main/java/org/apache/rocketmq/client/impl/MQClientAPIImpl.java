package org.apache.rocketmq.client.impl;

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

import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.protocol.header.SendMessageRequestHeader;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.impl.producer.TopicPublishInfo;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.client.producer.SendResult;

@Instrument(Class = "org.apache.rocketmq.client.impl.MQClientAPIImpl")
public abstract class MQClientAPIImpl{

    public SendResult sendMessage(
        final String addr,
        final String brokerName,
        final Message msg,
        final SendMessageRequestHeader requestHeader,
        final long timeoutMillis,
        final CommunicationMode communicationMode,
        final SendCallback sendCallback,
        final TopicPublishInfo topicPublishInfo,
        final MQClientInstance instance,
        final int retryTimesWhenSendFailed,
        final SendMessageContext context,
        final DefaultMQProducerImpl producer)  throws Throwable  {
        
        if(msg == null){
            return InstrumentApi.invokeOrigin();
        }
        
        // 创建 span
        Tracer tracer = TraceConfiguration.getTracer();
        Span span = tracer.spanBuilder("RocketMQ/" + msg.getTopic() + "/Producer")
                .setSpanKind(SpanKind.PRODUCER)
                .setParent(TraceConfiguration.getContext()) 
                .startSpan();  
        
        span.setAttribute(SemanticAttributes.MESSAGING_SYSTEM, "rocketMQ");
        span.setAttribute(SemanticAttributes.MESSAGING_DESTINATION_KIND, "topic");
        span.setAttribute(SemanticAttributes.MESSAGING_DESTINATION, msg.getTopic());  
        span.setAttribute("messaging.rocketmq.broker_address", addr);  
        
        // Set the context with the current span
        Scope scope = null;
        try {
            scope = TraceConfiguration.getContext().makeCurrent();
            
            SendResult result = InstrumentApi.invokeOrigin();
            span.setAttribute(SemanticAttributes.MESSAGING_MESSAGE_ID, result.getMsgId());
            span.setAttribute("messaging.rocketmq.send_result", result.getSendStatus().name());
            return result;
        } catch(Throwable e){
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        }  finally {
            span.end();
            scope.close();
        }
    }
};