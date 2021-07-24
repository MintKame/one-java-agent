package org.apache.http.impl.client;


import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

import com.trace.configuration.TraceConfiguration;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.agent.inst.InstrumentApi;


@Instrument(Class = "org.apache.http.impl.client.InternalHttpClient")
public abstract class InternalHttpClient{

    protected CloseableHttpResponse doExecute(
            final HttpHost target,
            final HttpRequest request,
            final HttpContext context) throws Throwable {
        if (target == null || request == null) {
            // illegal args, can't trace. ignore.
            return InstrumentApi.invokeOrigin();
        }

        int port = target.getPort();
        if(port <= 0){
            if("https".equals(target.getSchemeName().toLowerCase())){
                port = 443;
            } else{
                port = 80;
            }
        }
        String remotePeer = target.getHostName() + ":" + port;

        String uri = request.getRequestLine().getUri();
        boolean isUrl = uri.toLowerCase().startsWith("http");
        String url = null;
        if (isUrl) {
            url = uri;
        } else {
            StringBuffer buff = new StringBuffer();
            buff.append(target.getSchemeName().toLowerCase());
            buff.append("://");
            buff.append(remotePeer);
            buff.append(uri);
            url = buff.toString();
        }

        // 创建 span
        Tracer tracer = TraceConfiguration.getTracer();
        Span span = tracer.spanBuilder(uri)
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();

        span.setAttribute("url", url);
        span.setAttribute("peer", remotePeer);
        span.setAttribute("http.method", request.getRequestLine().getMethod());

        // Set the context with the current span
        Scope scope = null;
        try {
            scope = span.makeCurrent();

            CloseableHttpResponse response = InstrumentApi.invokeOrigin();

            if(response != null){
                StatusLine responseStatusLine = response.getStatusLine();
                if (responseStatusLine != null) {
                    int statusCode = responseStatusLine.getStatusCode();
                    if (statusCode >= 400) {
                        span.setStatus(StatusCode.ERROR, "STATUS_CODE: " + Integer.toString(statusCode));
                    }
                }
            }

            return response;
        } catch(Throwable e){
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        }  finally {
            span.end();
            scope.close();
        }
    }

};