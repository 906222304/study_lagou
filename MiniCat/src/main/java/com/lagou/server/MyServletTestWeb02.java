package com.lagou.server;

import java.io.IOException;

public class MyServletTestWeb02 extends HttpServlet {
    @Override
    public void doGet(Request request, Response response) {
        String contents = "<h2> GET web02</h2>";
        System.out.println(contents);

        try {
            response.output(HttpProtocolUtil.getHttpHeader200(contents.length()) + contents);
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
