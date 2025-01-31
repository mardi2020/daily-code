## Tomcat
Servlet 기반의 HTTP request 처리 컨테이너

### 톰캣을 구성하는 주요 기술
1. Servlet API (Jakarta Servlet)
    - 톰캣은 서블릿 컨테이너로서 servlet API를 기반으로 동장
    - HttpServletRequest, HttpServletResponse, ServletContext, ServletConfig, Filter, ServletContextListener
2. Connector & Protocols
    - 톰캣은 다양한 커넥터(프로토콜 핸들러)를 지원하여 HTTP 요청 처리
    - 대표적인 커넥터들
      - HTTP/1.1 Connector (기본)
      - HTTP/2 지원
3. Coyote (HTTP engine)
    - HTTP 요청을 파싱하고 처리함
    - 서블릿 컨테이너인 Catalina와 통합하여 동작하고 스레드 풀을 관리해 동시 요청을 효율적으로 관리
4. Catalina (servlet 컨테이너)
    - Servlet API를 구현하고 웹 애플리케이션을 실행하는 역할
5. Valve & Filters (요청 처리 흐름)
    - Valve라는 인터셉터를 사용해 요청을 가로채고 처리 가능
    - SpringBoot에서 사용되는 톰캣 내부 valve: AccessLogValve, ErrorReportValve
    - SpringBoot는 Servlet Filter를 등록하여 요청 제어
6. Web Application ClassLoader
    - 각 웹 애플리케이션마다 별도의 클래스 로더를 할당해 격리된 환경에서 실행할 수 있도록 함
    - Spring Boot의 embedded Tomcat에서 `org.springframework.boot.loader.LaunchedURLClassLoader`로 클래스를 로딩
7. Thread Pool (Executor)
    - 톰캣은 내부적으로 스레드 풀을 관리하여 요청을 효율적으로 처리
    - 기본적으로 Non-blocking IO 기반으로 동작, 설정을 통해 스레드 풀의 최대 크기 지정 가능
8. Embedded vs. Standalone 모드 차이
    - SpringBoot는 내장 톰캣을 사용하나 독립 실행형 톰캣을 따로 설치해서 배포 가능
    - 내장 톰캣 동작 방식
      - `TomcatServletWebServerFactory`를 사용해 인스턴스 생성
      - Connector를 등록해 HTTP 요청 수신
      - ServletContext를 초기화하여 Spring 애플리케이션과 연결

즉, 
- HTTP 커넥터: 클라이언트 요청을 받고 응답을 반환하는 HTTP 서버
- 리퀘스트 파서: HTTP 요청을 분석해서 적절한 서블릿으로 전달
- 서블릿 컨테이너: 서블릿의 생명주기를 관리하고 요청 처리
- 스레드풀: 요청을 효율적으로 처리하기 위해 멀티스레드 지원
