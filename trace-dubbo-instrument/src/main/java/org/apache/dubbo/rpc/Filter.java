package org.apache.dubbo.rpc;

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
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.agent.inst.InstrumentApi;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;

import java.net.InetAddress;
import java.net.InetSocketAddress;

@Instrument(Interface = "org.apache.dubbo.rpc.Filter")
public abstract class Filter { 
    
    Result invoke(Invoker<?> invoker, Invocation invocation) throws Throwable {

        if(invocation == null || invoker == null || !(invocation instanceof RpcInvocation)){
            return InstrumentApi.invokeOrigin(); 
        }

        RpcContext rpcContext = RpcContext.getContext(); 
        Boolean isConsumer = null;
        try{
            isConsumer = rpcContext.isConsumerSide();
        }catch(Throwable t){
            isConsumer = null;
        }
        if(isConsumer == null){
            return InstrumentApi.invokeOrigin(); 
        }

        URL requestURL = invoker.getUrl(); 
        String methodName = invocation.getMethodName(); 

        // operation name
        String opName = requestURL.getParameter(CommonConstants.GROUP_KEY);
        opName = (opName == null || opName.length() == 0) ? "" : opName + "/";
        opName += requestURL.getPath() + "." + methodName;

        // net peer
        String peerName = null;
        String peerIp = null;
        int port = 0;
        InetSocketAddress remoteConnection = rpcContext.getRemoteAddress();
        if (remoteConnection != null) {
            port = remoteConnection.getPort();
            InetAddress remoteAddress = remoteConnection.getAddress();
            if (remoteAddress != null) {
                peerName = remoteAddress.getHostName();
                peerIp = remoteAddress.getHostAddress(); 
            } else {
                // Failed DNS lookup, the host string is the name.
                peerName = remoteConnection.getHostString(); 
            }
        }
        
        // 创建 span
        Tracer tracer = TraceConfiguration.getTracer();
        Span span = null; 
        if(isConsumer){
            span = tracer.spanBuilder(opName)  
                .setSpanKind(SpanKind.CONSUMER)
                .setParent(Java8BytecodeBridge.currentContext()) 
                .startSpan(); 
        }else {
            // context propagation
            TextMapGetter<RpcInvocation> getter =
                new TextMapGetter<RpcInvocation>() {
                    @Override
                    public String get(RpcInvocation carrier, String key) {
                        return carrier.getAttachment(key);
                    }

                    @Override
                    public Iterable<String> keys(RpcInvocation carrier) {
                        return carrier.getAttachments().keySet();
                    } 
                };
                
            Context extractedContext = GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                    .extract(Java8BytecodeBridge.currentContext(),  (RpcInvocation) invocation, getter);

            span = tracer.spanBuilder(opName)
                .setSpanKind(SpanKind.PRODUCER)
                .setParent(extractedContext) 
                .startSpan();  
        }

        // 设置 attributes
        span.setAttribute(SemanticAttributes.RPC_SYSTEM, "dubbo"); 
        span.setAttribute(SemanticAttributes.RPC_SERVICE, invoker.getInterface().getName());
        span.setAttribute(SemanticAttributes.RPC_METHOD, methodName);
        if (peerName != null && !peerName.equals(peerIp)) {
            span.setAttribute(SemanticAttributes.NET_PEER_NAME, peerName);
        }
        if (peerIp != null) {
            span.setAttribute(SemanticAttributes.NET_PEER_IP, peerIp);
        } 
        if (port > 0) {
            span.setAttribute(SemanticAttributes.NET_PEER_PORT, (long) port);
        }

        // Set the context with the current span
        Scope scope = null;
        try {
            scope = span.makeCurrent();
            
            // context propagation
            if(isConsumer){
                TextMapSetter<RpcContext> setter = 
                new TextMapSetter<RpcContext>() {
                    @Override
                    public void set(RpcContext carrier, String key, String value) {
                        carrier.setAttachment(key, value); 
                    }
                };

                GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                    .inject(Java8BytecodeBridge.currentContext(), rpcContext, setter);
            }

            // invoke origin
            Result result = InstrumentApi.invokeOrigin(); 
            if (result != null && result.getException() != null) {
                span.setStatus(StatusCode.ERROR, result.getException().getMessage()); 
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
