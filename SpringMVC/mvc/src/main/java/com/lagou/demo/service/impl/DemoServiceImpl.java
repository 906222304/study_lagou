package com.lagou.demo.service.impl;

import com.lagou.demo.service.IDemoService;
import com.lagou.edu.mvcframework.annotations.LagouService;

@LagouService("demoService")
public class DemoServiceImpl implements IDemoService {
    @Override
    public String get(String name) {
        System.out.println("service 实现类中的name参数：" + name) ;
        return name;
    }

    @Override
    public String security(String s) {
        System.out.println("成功访问：" + s);
        return s;
    }
}
