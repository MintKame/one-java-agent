package com.alibaba.oneagent.trace.configuration;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators; 
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.Context;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor; 
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes; 

import java.util.concurrent.TimeUnit;
import java.util.Properties;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class TraceConfiguration {

    private static Tracer tracer = null;

    private static Context context = null;

    private static Span parentSpan = null;

    private static Scope scope = null;

    public static synchronized Tracer getTracer() throws IOException{  
        if (tracer == null) {  
            // 初始化openTelemetry
            Properties prop = new Properties();
            prop.load(TraceConfiguration.class.getResourceAsStream("/trace.properties"));

            String jaegerHost = System.getenv("JAEGER_AGENT_HOST");
            if (jaegerHost == null || jaegerHost.equals(""))
                jaegerHost = "127.0.0.1";
            int jaegerPort = Integer.valueOf(prop.getProperty("jaegerPort").trim());  
            String serviceName = prop.getProperty("appName").trim();

            initOpenTelemetry(jaegerHost, jaegerPort, serviceName);  

            // 设置tracer和parentSpan
            tracer = GlobalOpenTelemetry.getTracer("com.alibaba.oneagent.trace");
            try{
                parentSpan = tracer.spanBuilder("/").startSpan();
                scope = parentSpan.makeCurrent();
                context = Context.current();
            }catch(Throwable t){
                // 创建parentSpan失败
                parentSpan = null;
                scope = null;
            }
        }  
        return tracer;  
    }  

    public static Context getContext(){
        return context;
    }

    /**
     * Initialize an OpenTelemetry SDK with a Jaeger exporter and a SimpleSpanProcessor.
     */
    private static void initOpenTelemetry(String jaegerHost, int jaegerPort, String serviceName) {
        
        // Create a channel towards Jaeger end point
        ManagedChannel jaegerChannel =
            ManagedChannelBuilder.forAddress(jaegerHost, jaegerPort).usePlaintext().build();

        // 导出traces到Jaeger
        JaegerGrpcSpanExporter jaegerExporter =
            JaegerGrpcSpanExporter.builder()
                .setChannel(jaegerChannel)
                .setTimeout(30, TimeUnit.SECONDS)
                .build();

        ResourceBuilder resourceBuilder =
            new ResourceBuilder()
                .putAll(Resource.getDefault())
                .put(ResourceAttributes.SERVICE_NAME, 
                    "otel-" + (serviceName.isEmpty() ? "appName" : serviceName));  
        
        // Set to process the spans by the Jaeger Exporter
        final SdkTracerProvider tracerProvider =
            SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(jaegerExporter))
                .setResource(resourceBuilder.build())
                .build(); 

        OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))  
                .buildAndRegisterGlobal();

        // 关闭 SDK 
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if(parentSpan != null){
                    parentSpan.end();
                    scope.close();
                }
                tracerProvider.close();
            }
        })); 
    } 
} 