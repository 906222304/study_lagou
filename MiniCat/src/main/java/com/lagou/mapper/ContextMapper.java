package com.lagou.mapper;


import java.util.List;

public class ContextMapper {

    /**
     * 项目名称
     */
    private String contextName;

    /**
     * 一个项目有多个请求url
     * 请求url 用来锁定servlet
     */
    private List<WrapperMapper> wrapperMapperList;

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public List<WrapperMapper> getWrapperMapperList() {
        return wrapperMapperList;
    }

    public void setWrapperMapperList(List<WrapperMapper> wrapperMapperList) {
        this.wrapperMapperList = wrapperMapperList;
    }
}
