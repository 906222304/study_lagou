package com.lagou.server;

import com.lagou.mapper.ContextMapper;
import com.lagou.mapper.EngineMapper;
import com.lagou.mapper.HostMapper;
import com.lagou.mapper.WrapperMapper;
import com.lagou.util.MiniCatClassLoader;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;



public class Bootstrap {

    private String port;

    private EngineMapper engineMapper;

    public EngineMapper getEngineMapper() {
        return engineMapper;
    }

    public void setEngineMapper(EngineMapper engineMapper) {
        this.engineMapper = engineMapper;
    }

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
        // 2.解析webapps里的项目，解析当前项目的context,web.xml得到url映射关系；
        bootstrap.loadServerXml();
        // 3.最后处理请求，根据客户端的host以及上下文，还有url定位要处理的servlet然后提供请求返回给客户端；
        bootstrap.start();
    }

    /**
     * 开始监听端口，进行处理
     */
    private void start() {
        try {

            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(this.getPort()));
            Socket socket = serverSocket.accept();
            InputStream inputStream = socket.getInputStream();

            // 封装Request对象和Response对象
            Request request = new Request(inputStream);
            Response response = new Response(socket.getOutputStream());

            //请求url
            String url = request.getUrl();
            //获取上下文
            String context = url.substring(0, url.substring(1).indexOf("/") + 1);
            //真正请求的url
            String realUrl = url.replace(context, "");

            ContextMapper contextMapper = null;

            List<ContextMapper> contextMapperList = this.getEngineMapper().getHostMapper().getContextMapperList();

            // 从上下文中获取WrapperMapper
            for (ContextMapper mapper : contextMapperList) {
                String contextName = mapper.getContextName();
                if (context.equalsIgnoreCase("/" + contextName)) {
                    contextMapper = mapper;
                    break;
                }
            }
            // 不存在返回404
            if (contextMapper == null) {
                response.output(HttpProtocolUtil.getHttpHeader404());
                return;
            }

            List<WrapperMapper> wrapperMapperList = contextMapper.getWrapperMapperList();

            for (WrapperMapper wrapperMapper : wrapperMapperList) {
                // 根据url从mapper中获取处理的Object实例
                if (realUrl.equals(wrapperMapper.getUrl())) {
                    HttpServlet httpServlet = (HttpServlet) wrapperMapper.getObject();
                    httpServlet.service(request, response);
                    break;
                }
            }

            serverSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
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
            this.engineMapper = new EngineMapper();
            // 获取host
            Element host = (Element)rootDocument.selectSingleNode("//Host");
            // localhost
            String localhost = host.attributeValue("name");
            // 创建host映射，封装localhost
            HostMapper hostMapper = new HostMapper();
            hostMapper.setHostName(localhost);
            // 将HostMapper封装到EngineMapper中
            engineMapper.setHostMapper(hostMapper);
            // 获取webapp路径
            String appBase = host.attributeValue("appBase");
            // 通过webapps解析项目并存储
            loadWebApps(appBase);
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * 通过webapps路径处理路径下所有项目
     * @param appBase server.xml中的webapps路径
     */
    private void loadWebApps(String appBase) {
        File file = new File(appBase);
        // 获取webapp下所有项目
        File[] files = file.listFiles();

        // 维护一个HashMap暂时存储项目对应的web.xml路径
        HashMap<String, String> webXmlMap = new HashMap<>();

        // 维护一个HashMap暂时存储项目对应的.class文件路径
        HashMap<String, String> classMap = new HashMap<>();

        List<ContextMapper> contextMapperList = new ArrayList<>();

        for (File appFile : Objects.requireNonNull(files)) {
            String fileName = appFile.getName();
            // 设置ContextMapper中的项目名称
            contextMapperList.add(new ContextMapper(fileName));
            // 递归处理项目
            doApps(appFile.getPath(), fileName, webXmlMap, classMap);
        }
        // 配置上下文
        this.engineMapper.getHostMapper().setContextMapperList(contextMapperList);
        // 处理web.xml
        doWebXml(webXmlMap);
        // 类加载实例化
        doInstance(classMap);
    }

    /**
     * 类加载实例化
     * @param classMap 存储项目对应的.class文件路径
     */
    private void doInstance(HashMap<String, String> classMap) {
        for (Map.Entry<String, String> entry : classMap.entrySet()) {
            String webappName = entry.getKey();
            String classPath = entry.getValue();

            // 加载class，实例化
            MiniCatClassLoader miniCatClassLoader = new MiniCatClassLoader();
            try {
                Class<?> aClass = miniCatClassLoader.findClass(classPath);
                List<ContextMapper> contextMapperList = this.getEngineMapper().getHostMapper().getContextMapperList();
                for (ContextMapper contextMapper : contextMapperList) {
                    if (webappName.equals(contextMapper.getContextName())) {
                        List<WrapperMapper> wrapperMapperList = contextMapper.getWrapperMapperList();
                        //判断当前类是否在web.xml配置的servlet class里面
                        for (WrapperMapper wrapperMapper : wrapperMapperList) {
                            if (classPath.replaceAll("/", ".").contains(wrapperMapper.getServletClassName())) {
                                // 创建保存实例对象
                                try {
                                    wrapperMapper.setObject(aClass.newInstance());
                                } catch (InstantiationException | IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据路径遍历并处理对应web.xml文件
     * @param webXmlMap 存储项目对应的web.xml路径
     */
    private void doWebXml(HashMap<String, String> webXmlMap) {
        for (Map.Entry<String, String> entry : webXmlMap.entrySet()) {
            String webappName = entry.getKey();
            String webappPath = entry.getValue();
            // 根据项目路径，读取解析web.xml文件
            loadServlet(webappName, webappPath);
        }
    }

    /**
     * 加载解析web.xml文件
     * @param webappName 项目名称
     * @param webappPath web.xml路径
     */
    private void loadServlet(String webappName, String webappPath) {
        // 获取上下文
        List<ContextMapper> contextMapper = new ArrayList<>();
        for (ContextMapper mapper : this.engineMapper.getHostMapper().getContextMapperList()) {
            if (webappName.equals(mapper.getContextName())) {
                contextMapper.add(mapper);
            }
        }
        // 存储url，以及servlet和请求url
        List<WrapperMapper> wrapperMapperList = new ArrayList<>();

        // 开始解析web.xml文件
        try (InputStream inputStream = new FileInputStream(webappPath)) {
            SAXReader saxReader = new SAXReader();
            Document read = saxReader.read(inputStream);
            Element rootElement = read.getRootElement();
            List<Element> selectNodes = rootElement.selectNodes("//servlet");
            for (Element element : selectNodes) {
                // <servlet-name>lagou</servlet-name>
                Element servletNameElement = (Element) element.selectSingleNode("servlet-name");
                String servletName = servletNameElement.getStringValue();
                // 获取全限定类名 例如：<servlet-class>server.LagouServlet</servlet-class>
                Element servletClassElement = (Element) element.selectSingleNode("servlet-class");
                String servletClass = servletClassElement.getStringValue();

                // 根据servlet-name的值找到url-pattern
                Element servletMapping = (Element) rootElement.selectSingleNode("/web-app/servlet-mapping[servlet-name='" + servletName + "']");
                // urlPattern 例如:/servlet
                String urlPattern = servletMapping.selectSingleNode("url-pattern").getStringValue();

                // 创建WrapperMapper映射
                WrapperMapper wrapperMapper = new WrapperMapper();
                wrapperMapper.setServletClassName(servletClass);
                wrapperMapper.setUrl(urlPattern);
                wrapperMapperList.add(wrapperMapper);
            }
            // 将映射封装到上下文对象中
            contextMapper.get(0).setWrapperMapperList(wrapperMapperList);

        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 递归处理webapp项目
     * @param path 路径
     * @param webappName 项目名
     * @param webXmlMap 存储项目对应的web.xml路径
     */
    private void doApps(String path, String webappName, HashMap<String, String> webXmlMap, HashMap<String, String> classMap) {

        File file = new File(path);
        File[] files = file.listFiles();
        // 没有文件时,结束递归
        if (files == null) {
            return;
        }
        for (File webappsFile : files) {
            if (webappsFile.isDirectory()) {
                // 如果是文件夹,递归处理
                doApps(webappsFile.getPath(), webappName, webXmlMap, classMap);
            } else {
                // 存储web.xml路径
                if ("web.xml".equals(webappsFile.getName())) {
                    webXmlMap.put(webappName, webappsFile.getPath());
                } else if (webappsFile.getName().endsWith(".class")) {
                // 存储.class文件路径
                    classMap.put(webappName, webappsFile.getPath());
                }
            }
        }
    }

}
