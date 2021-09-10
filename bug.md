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
2. provider 许多 ReflectionsException（如下），在不使用agent时也存在这个问题，是demo的问题，不影响trace效果 

```
[main] WARN org.reflections.Reflections - could not get type for name okhttp3.Callback from any class loader
org.reflections.ReflectionsException: could not get type for name okhttp3.Callback
at org.reflections.ReflectionUtils.forName(ReflectionUtils.java:390)
at org.reflections.Reflections.expandSuperTypes(Reflections.java:381)
at org.reflections.Reflections.<init>(Reflections.java:126)
at io.swagger.jaxrs.config.BeanConfig.classes(BeanConfig.java:276)
at io.swagger.jaxrs.config.BeanConfig.scanAndRead(BeanConfig.java:240)
at io.swagger.jaxrs.config.BeanConfig.setScan(BeanConfig.java:221)
at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
at java.lang.reflect.Method.invoke(Method.java:498)
at org.springframework.beans.BeanWrapperImpl$BeanPropertyHandler.setValue(BeanWrapperImpl.java:354)
at org.springframework.beans.AbstractNestablePropertyAccessor.processLocalProperty(AbstractNestablePropertyAccessor.java:467)
at org.springframework.beans.AbstractNestablePropertyAccessor.setPropertyValue(AbstractNestablePropertyAccessor.java:290)
at org.springframework.beans.AbstractNestablePropertyAccessor.setPropertyValue(AbstractNestablePropertyAccessor.java:278)
at org.springframework.beans.AbstractPropertyAccessor.setPropertyValues(AbstractPropertyAccessor.java:95)
at org.springframework.beans.AbstractPropertyAccessor.setPropertyValues(AbstractPropertyAccessor.java:75)
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyPropertyValues(AbstractAutowireCapableBeanFactory.java:1566)
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.populateBean(AbstractAutowireCapableBeanFactory.java:1280)
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:553)
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:483)
at org.springframework.beans.factory.support.AbstractBeanFactory$1.getObject(AbstractBeanFactory.java:312)
at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:230)
at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:308)
at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:197)
at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:761)
at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:867)
at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:543)
at org.springframework.context.support.ClassPathXmlApplicationContext.<init>(ClassPathXmlApplicationContext.java:139)
at org.springframework.context.support.ClassPathXmlApplicationContext.<init>(ClassPathXmlApplicationContext.java:93)
at org.apache.dubbo.samples.rest.RestProvider.main(RestProvider.java:31)
Caused by: java.lang.ClassNotFoundException: okhttp3.Callback
at java.net.URLClassLoader.findClass(URLClassLoader.java:382)
at java.lang.ClassLoader.loadClass(ClassLoader.java:418)
at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:355)
at java.lang.ClassLoader.loadClass(ClassLoader.java:351)
at org.reflections.ReflectionUtils.forName(ReflectionUtils.java:388)
... 29 more
```





