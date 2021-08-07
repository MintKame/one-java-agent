package com.trace.configuration;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor; 

import java.util.concurrent.TimeUnit;
import java.util.Properties;
import java.io.IOException;

public class TraceConfiguration {

    private static OpenTelemetry openTelemetry = null;  

    private static Tracer tracer = null;

    private static Span parentSpan = null;

    private static Scope scope = null;

    public static synchronized Tracer getTracer() throws IOException{  
        if (openTelemetry == null) {  
            // 初始化openTelemetry
            Properties prop = new Properties();
            prop.load(TraceConfiguration.class.getResourceAsStream("/trace.properties"));

            String jaegerHost = prop.getProperty("jaegerHost");
            int jaegerPort = Integer.valueOf(prop.getProperty("jaegerPort")); 
            String appName = prop.getProperty("applicationName");

            openTelemetry = initOpenTelemetry(jaegerHost, jaegerPort);  

            // 设置tracer和parentSpan
            tracer = openTelemetry.getTracer("com.alibaba.oneagent");
            try{
                parentSpan = tracer.spanBuilder(appName).startSpan();
                scope = parentSpan.makeCurrent();
            }catch(Throwable t){
                // 创建parentSpan失败
                parentSpan = null;
                scope = null;
            }
        }  
        return tracer;  
    }  
    /**
     * Initialize an OpenTelemetry SDK with a Jaeger exporter and a SimpleSpanProcessor.
     */
    private static OpenTelemetry initOpenTelemetry(String jaegerHost, int jaegerPort) {
        
        // Create a channel towards Jaeger end point
        ManagedChannel jaegerChannel =
                ManagedChannelBuilder.forAddress(jaegerHost, jaegerPort).usePlaintext().build();

        // 导出traces到Jaeger
        JaegerGrpcSpanExporter jaegerExporter =
                JaegerGrpcSpanExporter.builder()
                        .setChannel(jaegerChannel)
                        .setTimeout(30, TimeUnit.SECONDS)
                        .build();

        final SdkTracerProvider tracerProvider =
                SdkTracerProvider.builder()
                        .addSpanProcessor(SimpleSpanProcessor.create(jaegerExporter)) 
                        .setResource(Resource.getDefault())
                        .build();

        OpenTelemetrySdk openTelemetry =
                OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();

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
        return openTelemetry;
    }
}