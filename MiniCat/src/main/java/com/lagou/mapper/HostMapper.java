package com.lagou.mapper;

import java.util.List;

public class HostMapper {

    /**
     * host地址
     */
    private String hostName;

    /**
     * 一个Host可以对应多个Context
     * Context：项目名
     */
    private List<ContextMapper> contextMapperList;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public List<ContextMapper> getContextMapperList() {
        return contextMapperList;
    }

    public void setContextMapperList(List<ContextMapper> contextMapperList) {
        this.contextMapperList = contextMapperList;
    }

}
