## AOP

### 흐름
1. 메서드 실행전 - `@Before` 에서 트랜잭션 정보 로그 남기기
2. 트랜잭션 시작 및 메서드 실행 - `@Around` 에서 proceed() 실행
3. 메서드 실행 완료 (정상적으로 종료) - `@AfterReturning` 에서 트랜잭션 커밋
4. 메서드 실행 중 예외 발생 - `@AfterThrowing` 에서 트랜잭션 롤백
5. 메서드 종료 후 트랜잭션 종료 - `@After` 에서 endTransaction() 실행

### Spring AOP advice annotation

| 어노테이션           | 실행 시점                            | 설명                                | 사용가능한 매개변수             |
|-----------------|----------------------------------|-----------------------------------|------------------------|
| @Before         | 대상 메서드 실행 직전                     | 메서드 실행 전, 공통 로직 수행(로그, 보안 확인 등)   | JoinPoint              |
| @AfterReturning | 대상 메서드 실행 성공 후 (예외 X)            | 정상 실행되고 나서 추가 로직 실행(결과 로깅, 후처리 등) | JoinPoint, returning 값 |
| @AfterThrowing  | 대상 메서드 실행 중에 예외 발생시              | 예외발생시 특정 로직 실행(롤백 등)              | JoinPoint, throwing 값  |
| @After          | 대상 메서드 실행 완료후 (정상여부 무관하게 무조건 실행) | 리소스 정리 및 트랜잭션 종료                  | JoinPoint              |
| @Around         | 대상 메서드 실행 전후를 감싸 실행              | 메서드 실행을 직접 제어 (proceed() 호출해서)    | ProceedingJoinPoint    |

### 📊 ConcurrentHashMap vs. ThreadLocal vs. SynchronizedMap
| 데이터 구조                           | 동시성 처리       | 메모리 사용 | 속도 | 특징                        |
|----------------------------------|--------------|--------|----|---------------------------|
| ConcurrentHashMap                | ✅ 멀티스레드 안전   | 중간     | 빠름 | 트랜잭션을 메서드 단위로 저장할 때 적합    |
| ThreadLocal                      | ✅ 각 스레드별로 관리 | 적음     | 빠름 | 각 스레드가 독립적인 트랜잭션을 가질 때 적합 |
| Collections.synchronizedMap(...) | ❌ 전체 락 사용    | 높음     | 느림 | 멀티스레드 환경에서 성능 저하          |