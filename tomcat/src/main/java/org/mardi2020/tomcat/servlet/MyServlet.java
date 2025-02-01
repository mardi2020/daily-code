package org.mardi2020.tomcat.servlet;

import org.mardi2020.tomcat.server.MyRequest;
import org.mardi2020.tomcat.server.MyResponse;

public interface MyServlet {

    void service(MyRequest request, MyResponse response);
}
