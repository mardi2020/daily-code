package org.mardi2020.tomcat.server;

import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class MyResponse {

    private PrintWriter writer;

    private int statusCode = HttpStatus.OK.value();

    private Map<String, String> headers = new HashMap<>();

    private StringBuilder body = new StringBuilder();

    @Builder
    public MyResponse(int statusCode, Map<String, String> headers, StringBuilder body) {
        this.headers.put("Content-Type", "text/html; charset=UTF-8");
        this.statusCode = statusCode;
        if (!headers.isEmpty()) this.headers = headers;
        this.body = body;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void setBody(String content) {
        body.append(content);
    }

    public void setStatus(int value) {
        this.statusCode = value;
    }

    public void sendResponse(OutputStream outputStream) {
        this.writer = new PrintWriter(outputStream, true);
        writer.println("HTTP/1.1 " + statusCode + " " + HttpStatus.valueOf(statusCode).getReasonPhrase());
        headers.forEach((key, value) -> writer.println(key + ": " + value));
        writer.println(""); // 헤더와 바디 사이 빈 줄 추가
        writer.println(body); // 응답 바디 출력
        writer.flush(); // 💡 클라이언트로 즉시 전송
        writer.close(); // 💡 스트림 종료
    }
}
