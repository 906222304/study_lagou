package com.lagou.server;

import com.lagou.mapper.HostMapper;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


public class Bootstrap {

    private String port;

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    /**
     * MiniCat 程序启动入口
     * @param args String[]
     */
    public static void main(String[] args) {
        Bootstrap bootstrap = new Bootstrap();
        // 1.解析server.xml, 获取监听的端口号以及webapp项目的所在路径
        bootstrap.loadServerXml();
        // 2.解析webapps里的项目，解析当前项目的context,web.xml得到url映射关系；
        // 3.最后处理请求，根据客户端的host以及上下文，还有url定位要处理的servelt然后提供请求返回给客户端；
    }

    /**
     * 读取解析server.xml
     */
    private void loadServerXml() {
        String server = "server.xml";
        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(server)) {
            SAXReader saxReader = new SAXReader();
            Document document = saxReader.read(resourceAsStream);
            // 获取根路径
            Element rootElement = document.getRootElement();
            Document rootDocument = rootElement.getDocument();
            // 获取端口号
            Element connector = (Element)rootDocument.selectSingleNode("//Connector");
            String port = connector.attributeValue("port");
            this.setPort(port);
            // 获取host
            Element host = (Element)rootDocument.selectSingleNode("//Host");
            // localhost
            String localhost = host.attributeValue("name");
            // 创建host映射，封装localhost
            HostMapper hostMapper = new HostMapper();
            hostMapper.setHostName(localhost);
            // 获取webapp路径
            String appBase = host.attributeValue("appBase");
            // 通过webapps解析项目并存储
            loadWebApps(appBase);
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
        }
    }

    private void loadWebApps(String appBase) {
        File file = new File(appBase);

    }

}
