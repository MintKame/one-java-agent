# 准备

+ 导出trace数据

```bash
docker run --rm -i --name jaeger -p 16686:16686 -p 14250:14250 jaegertracing/all-in-one:1.16
```

+ 下载代码
```bash
git clone https://github.com/MintKame/one-java-agent.git -b bug-fix
```

```bash
cd one-java-agent
```

+ 打包
```bash
mvn clean package -P local -DskipTests 
```

+ 更改本文件中 “one-java-agent.jar路径”

# http



## bug重现方式

+ 打包

```bash
mvn clean package -f demo/trace-httpClient-demo
```

+ 运行

```bash
java -javaagent:"one-java-agent.jar路径"  -cp demo/trace-httpClient-demo/target/trace-httpClient-demo.jar com.trace.demo.httpclient.HttpClientDemo
```





## 具体异常栈

```
Exception in thread "main" java.lang.IncompatibleClassChangeError: Class org.apache.http.impl.client.InternalHttpClient$1 does not implement the requested interface io.opentelemetry.context.propagation.TextMapSetter
at io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator.inject(W3CTraceContextPropagator.java:133) 
at org.apache.http.impl.client.InternalHttpClient.doExecute(InternalHttpClient.java:123)
at org.apache.http.impl.client.CloseableHttpClient.execute(CloseableHttpClient.java:83)
at org.apache.http.impl.client.CloseableHttpClient.execute(CloseableHttpClient.java:108)
at com.trace.demo.httpclient.HttpClientDemo.main(HttpClientDemo.java:23)
```

# rocketMQ

## bug重现方式

+ 启动rocketmq的namesrv和broker

+ 打包

```bash
mvn clean package -f demo/trace-rocketMQ-demo
```

+ 运行

```bash
java -javaagent:"one-java-agent.jar路径"  -cp demo/trace-rocketMQ-demo/target/trace-rocketMQ-demo.jar com.trace.demo.rocketmq.Producer
```

```bash
java -javaagent:"one-java-agent.jar路径"  -cp demo/trace-rocketMQ-demo/target/trace-rocketMQ-demo.jar com.trace.demo.rocketmq.Consumer
```





## 具体异常栈

1. Producer不使用javaagent，Consumer使用有context propgation的javaagent时，Consumer未收到消息

2. Producer使用有context propgation的javaagent时，出现：

```
Exception in thread "main" java.lang.NoSuchMethodError: org.apache.rocketmq.client.impl.MQClientAPIImpl$1.<init>(Lorg/apache/rocketmq/client/impl/MQClientAPIImpl;)V
at org.apache.rocketmq.client.impl.MQClientAPIImpl.sendMessage(MQClientAPIImpl.java:76)
at org.apache.rocketmq.client.impl.MQClientAPIImpl.sendMessage(MQClientAPIImpl.java:431)
at org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl.sendKernelImpl(DefaultMQProducerImpl.java:853)
at org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl.sendDefaultImpl(DefaultMQProducerImpl.java:583)
at org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl.send(DefaultMQProducerImpl.java:1342)
at org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl.send(DefaultMQProducerImpl.java:1288)
at org.apache.rocketmq.client.producer.DefaultMQProducer.send(DefaultMQProducer.java:324)
at com.trace.demo.rocketmq.Producer.main(Producer.java:61)
```



# dubbo

## bug重现方式

+ 这个demo中也用到了httpclient，需要先删除oneagent中的其他插件，只留下`trace-dubbo-plugin@0.0.2-SNAPSHOT`

+ 打包

```bash
mvn clean package -f demo/trace-dubbo-demo
```

+ 运行

```bash
java -javaagent:"one-java-agent.jar路径"  -cp demo/trace-dubbo-demo/target/trace-dubbo-demo.jar org.apache.dubbo.samples.rest.RestProvider 
```



```bash
java -javaagent:"one-java-agent.jar路径"  -cp demo/trace-dubbo-demo/target/trace-dubbo-demo.jar org.apache.dubbo.samples.rest.RestConsumer
```





## 具体异常栈

1. 使用有context propgation的agent时，可以正常收发，但是jaeger中没有trace信息





