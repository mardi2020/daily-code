package org.mardi2020.tomcat.server;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

@Getter
public class MyRequest {

    private String method;

    private String path;

    public MyRequest(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(inputStream));
        String line = reader.readLine();
        if (line != null) {
            String[] parts = line.split(" ");
            this.method = parts[0];
            this.path = parts[1];
        }
    }
}
