package org.mardi2020.tomcat.server;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mardi2020.tomcat.servlet.MyServlet;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@NoArgsConstructor
public class TomcatByServerSocket {

    private int port = 8080;

    private final Map<String, MyServlet> servlets = new HashMap<>();

    public TomcatByServerSocket(int port) {
        this.port = port;
    }

    public void registerServlet(String path, MyServlet servlet) {
        servlets.put(path, servlet);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Server started at port {}", port);

            while (true) {
                Socket socket = serverSocket.accept();
                handleRequest(socket);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleRequest(Socket socket) throws IOException {
        try {
            MyRequest request = new MyRequest(socket.getInputStream());
            MyResponse response = MyResponseFactory.create("", request.getPath(),
                    servlets.get(request.getPath()) != null);
            MyServlet servlet = servlets.get(request.getPath());
            if (servlet != null) {
                servlet.service(request, response);
            }
            response.sendResponse(socket.getOutputStream()); // 반드시 응답을 보내도록 보장
        } catch (IOException e) {
            throw new IOException(e);
        }
    }
}
