package com.lagou.server;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class Bootstrap {

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
            // 获取host
            Element host = (Element)rootDocument.selectSingleNode("//Host");
            String localhost = host.attributeValue("name");
            String appBase = host.attributeValue("appBase");
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
        }
    }

}
