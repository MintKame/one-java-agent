# demo演示

https://github.com/MintKame/one-java-agent-demo

# 使用方法

1. 配置[trace.property](../trace-configuration/src/main/resources/trace.properties)

   + appName：应用名，用于在jaeger中区分（默认appname）

   + jaegerPort：Trace数据导出到jaeger（默认14250） 

2. 配置环境变量

   + JAEGER_AGENT_HOST（若未设置，则为默认的localhost） 

3. 运行Jaeger

```shell script
docker run --rm -i --name jaeger -p 16686:16686 -p 14250:14250 jaegertracing/all-in-one:1.16
```

Jaeger UI 端口：默认16686

4. 查看Jaeger UI

   http://localhost:16686
