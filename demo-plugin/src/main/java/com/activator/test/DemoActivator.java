package com.activator.test;

import com.alibaba.bytekit.ByteKit;
import com.alibaba.fastjson.JSON;
import com.alibaba.oneagent.plugin.PluginActivator;
import com.alibaba.oneagent.plugin.PluginContext;

public class DemoActivator implements PluginActivator {
    private String name = this.getClass().getSimpleName();

    @Override
    public boolean enabled(PluginContext context) {
        System.out.println("enabled " + this.getClass().getName());
        System.err.println(this.getClass().getSimpleName() + ": " + JSON.toJSONString(this));

        System.err.println("bytekit url: " + ByteKit.class.getProtectionDomain().getCodeSource().getLocation());
        return true;
    }

    @Override
    public void init(PluginContext context) throws Exception {
        System.out.println("init " + this.getClass().getName());
    }

    @Override
    public void start(PluginContext context) throws Exception {
        System.out.println("start " + this.getClass().getName());
    }

    @Override
    public void stop(PluginContext context) throws Exception {
        System.out.println("stop " + this.getClass().getName());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
