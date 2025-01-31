package org.mardi2020.tomcat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//import org.mardi2020.tomcat.server.TomcatByServerSocket;
//import org.mardi2020.tomcat.servlet.MyServletImpl;

@SpringBootApplication
public class TomcatApplication {

	public static void main(String[] args) {
		SpringApplication.run(TomcatApplication.class, args);

//		TomcatByServerSocket server = new TomcatByServerSocket();
//		server.registerServlet("/hello", new MyServletImpl());
//		server.start();
	}

}
