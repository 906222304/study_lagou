package com.lagou.mapper;

public class EngineMapper {

    /**
     * 映射server.xml的Host标签
     */
    private HostMapper hostMapper;

    public HostMapper getHostMapper() {
        return hostMapper;
    }

    public void setHostMapper(HostMapper hostMapper) {
        this.hostMapper = hostMapper;
    }
}
