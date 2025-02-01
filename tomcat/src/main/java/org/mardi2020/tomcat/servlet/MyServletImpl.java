package org.mardi2020.tomcat.servlet;

import lombok.Getter;
import org.mardi2020.tomcat.server.MyRequest;
import org.mardi2020.tomcat.server.MyResponse;
import org.springframework.http.HttpStatus;

@Getter
public class MyServletImpl implements MyServlet {

    @Override
    public void service(MyRequest request, MyResponse response) {
        response.setStatus(HttpStatus.OK.value());
        response.addHeader("Custom-header", "myFirstHttpResponse");
        response.setBody("<h1>Hello, world!</h1>");
    }
}
