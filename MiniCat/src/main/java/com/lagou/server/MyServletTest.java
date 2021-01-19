package com.lagou.server;

import java.io.IOException;

public class MyServletTest extends HttpServlet {
    @Override
    public void doGet(Request request, Response response) {
        String contents = "<h2> GET 外部部署业务请求 </h2>";
        System.out.println(contents);

        try {
            response.output(contents);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doPost(Request request, Response response) {
        doGet(request, response);
    }

    @Override
    public void init() throws Exception {

    }

    @Override
    public void destory() throws Exception {

    }
}
