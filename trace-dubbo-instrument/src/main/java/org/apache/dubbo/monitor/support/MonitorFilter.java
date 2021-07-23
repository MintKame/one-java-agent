package org.apache.dubbo.monitor.support;

import com.trace.configuration.TraceConfiguration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.agent.inst.InstrumentApi;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;

@Instrument(Class = "org.apache.dubbo.monitor.support.MonitorFilter")
public abstract class MonitorFilter { 
    
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws Throwable {

        RpcContext rpcContext = RpcContext.getContext();
        boolean isConsumer = rpcContext.isConsumerSide();
        URL requestURL = invoker.getUrl();
        String host = requestURL.getHost();
        int port = requestURL.getPort();

        // operation name
        String opName = requestURL.getParameter(CommonConstants.GROUP_KEY);
        opName = (opName == null || opName.length() == 0) ? "" : opName + "/";
        opName += requestURL.getPath() + "." + invocation.getMethodName();

        // 创建 span
        Tracer tracer = TraceConfiguration.getTracer();
        Span span = null; 
        if(isConsumer){
            span = tracer.spanBuilder(opName)  
                .setSpanKind(SpanKind.CONSUMER)
                .startSpan(); 
        }else {
            span = tracer.spanBuilder(opName)
                .setSpanKind(SpanKind.PRODUCER)
                .startSpan();  
        }
        span.setAttribute("requestURL", requestURL.toString());
        span.setAttribute("peer", host + String.valueOf(port));

        // Set the context with the current span
        Scope scope = null;
        try {
            scope = span.makeCurrent();

            Result result = InstrumentApi.invokeOrigin(); 
            if (result != null && result.getException() != null) {
                span.setStatus(StatusCode.ERROR, result.getException().getMessage()); // todo
            }
            return result;
        } catch(Throwable e){
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        }  finally {
            span.end();
            scope.close();
        }
    }
}
