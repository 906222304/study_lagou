package com.lagou.demo.controller;

import com.lagou.demo.service.IDemoService;
import com.lagou.edu.mvcframework.annotations.LagouAutowired;
import com.lagou.edu.mvcframework.annotations.LagouController;
import com.lagou.edu.mvcframework.annotations.LagouRequestMapping;
import com.lagou.edu.mvcframework.annotations.Security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@LagouController
@LagouRequestMapping("/demo")
@Security({"zhangsan","lisi","wangwu"})
public class DemoController {


    @LagouAutowired
    private IDemoService demoService;


    /**
     * URL: /demo/query?name=lisi
     * @param request
     * @param response
     * @param name
     * @return
     */
    @LagouRequestMapping("/query")
    public String query(HttpServletRequest request, HttpServletResponse response,String name) {
        return demoService.get(name);
    }

    @Security({"zhangsan"})
    @LagouRequestMapping("/handle1")
    public String handle1(HttpServletRequest request, HttpServletResponse response,String username) {
        return demoService.security("handle1" + username);
    }

    @Security({"lisi"})
    @LagouRequestMapping("/handle2")
    public String handle2(HttpServletRequest request, HttpServletResponse response,String username) {
        return demoService.security("handle2" + username);
    }
}
