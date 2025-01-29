# Transaction

## Propagation
| 전파유형          | 설명                                     | 동작 방식                                     |
|---------------|----------------------------------------|-------------------------------------------|
| REQUIRED      | 현재 트랜잭션이 있으면 참여하고 없다면 새 트랜잭션 시작        | 기존 트랜잭션 참여, 없으면 새 트랜잭션 시작                 |
| REQUIRES_NEW  | 항상 새 트랜잭션을 시작하고 기존 트랜잭션은 보류함           | 기존 트랜잭션 일시 중지 후 새로운 트랜잭션 실행               |
| SUPPORTS      | 현재 트랜잭션이 있으면 참여하고 없다면 트랜잭션 없이 실행       | 트랜잭션이 있으면 참여, 없으면 일반 실행                   |
| MANDATORY     | 반드시 기존 트랜잭션이 있어야하고 없으면 예외 발생           | 기존 트랜잭션이 없으면 SQLException 발생              |
| NEVER         | 트랜잭션이 있으면 예외 발생, 없으면 트랜잭션 없이 실행        | 기존 트랜잭션 있으면 SQLException, 없으면 일반 실행       |
| NOT_SUPPORTED | 현재 트랜잭션을 중단하고 트랜잭션 없이 실행               | 기존 트랜잭션 보류 후 트랜잭션 없이 실행                   |
| NESTED        | 현재 트랜잭션이 있으면 중첩 트랜잭션 시작, 없다면 새 트랜잭션 시작 | 트랜잭션 내에서 별도의 세이브포인트(rollback point) 관리 가능 |

### 전파 유형 적용
- Required: 일반적 CRUD 비즈니스 로직에 사용
- Requires_new: 독립적인 트랜잭션을 실행해야 하고 메인 작업과는 별개로 반드시 성공하거나 실패해야 하는 보조 작업이 있을 때
  - ex) 주문 생성중 오류가 발생하더라도 오류 로그는 남겨야함
- Supports: 읽기 전용 작업에서 트랜잭션 여부에 상관없이 작동하도록 할 때
  - Mandatory: 상위 계층에서 트랜잭션을 보장해야 하는 로직이 있을 때
    - ex) 이체 전 검증 작업
    ```java
      @MyTransactional(propagation = CustomPropagation.REQUIRED)
      public void transferMoney() {
          verifyAccount(); // 검증로직
          accountRepository.updateBalance();
      }
    
      @MyTransactional(propagation = CustomPropagation.MANDATORY) // 트랜잭션 필수
      public void verifyAccount() {
        if (!accountRepository.existsById(1)) {
        throw new RuntimeException("Account not found");
        }
      }
      ```
- Never: 캐시 업데이트 또는 외부 시스템 api 호출
- Not_supported: 레포트 생성 및 대용량 조회 작업시(데이터베이스 lock을 피하기 위해)
- Nested: 부모 트랜잭션 내에서 독립적인 서브 트랜잭션 실행, 일부 작업만 롤백 필요한 경우
  - ex) 분할 결제 시 일부 항목이 실패했을 때 전체 작업 롤백 없이 해당 항목만 롤백처리
  ```java
    @MyTransactional(propagation = CustomPropagation.REQUIRED)
    public void processPayment() {
        try {
            makePartialPayment(); // 여기서 문제가 발생하더라도 문제가 발생한 서브 트랜잭션만 롤백
        } catch (Exception e) {
            System.out.println("Partial payment failed, but proceeding.");
        }
    }
    
    @MyTransactional(propagation = CustomPropagation.NESTED)
        public void makePartialPayment() {
        paymentRepository.save(new Payment());
    }
    ```
  
## Isolation
| 고립 수준            | Dirty Read | Non-Repeatable Read | Phantom Read |
|------------------|------------|---------------------|--------------|
| READ UNCOMMITTED | 허용         | 허용                  | 허용           |
| READ COMMITTED   | 불가         | 허용                  | 허용           |
| REPEATABLE READ  | 불가         | 방지                  | 허용           |
| SERIALIZABLE     | 불가         | 방지                  | 방지           |

Spring에서의 @Transactional isolation 디폴트 값은 READ COMMITTED

MySQL에서는 READ_COMMITTED나 REPEATABLE_READ가 주로 사용되며, SERIALIZABLE은 제한적이라고 함

높은 격리 수준은 동시성을 감소시켜 필요 이상으로 높은 수준을 설정하여 성능 저하가 발생되지 않게 조심

