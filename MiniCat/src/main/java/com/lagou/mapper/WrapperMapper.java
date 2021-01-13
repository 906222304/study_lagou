package com.lagou.mapper;

public class WrapperMapper {

    /**
     * 请求url
     */
    private String url;

    /**
     * servlet实例
     */
    private Object object;

    /**
     * web.xml里面配置的全限定名
     */
    private String servletClassName;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public String getServletClassName() {
        return servletClassName;
    }

    public void setServletClassName(String servletClassName) {
        this.servletClassName = servletClassName;
    }
}
