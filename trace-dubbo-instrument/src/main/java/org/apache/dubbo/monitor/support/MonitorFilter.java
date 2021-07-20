package org.apache.dubbo.monitor.support;

import com.trace.configuration.TraceConfiguration;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.agent.inst.InstrumentApi;

@Instrument(Class = "org.apache.dubbo.monitor.support.MonitorFilter")
public abstract class MonitorFilter { 
    
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws Throwable {

        RpcContext rpcContext = RpcContext.getContext();
        boolean isConsumer = rpcContext.isConsumerSide();
        URL requestURL = invoker.getUrl();
        String host = requestURL.getHost();
        int port = requestURL.getPort();

        System.err.println("isConsumer: " + isConsumer);
        System.err.println("requestURL: " + requestURL);
        System.err.println("host: " + host);
        System.err.println("port: " + port);

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
        span.setAttribute("host", host);
        span.setAttribute("port", String.valueOf(port));

        // Set the context with the current span
        Scope scope = null;
        try {
            scope = span.makeCurrent();

            Result result = InstrumentApi.invokeOrigin();
            System.err.println("result: " + result);
            if (result != null && result.getException() != null) {
                System.err.println("exception: " + result.getException());
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
