# test 시나리오

## Isolation test
1. id=1, value='initial'인 초기 데이터 추가
2. Thread-1 (write operation): value 값을 "updated"로 변경 작업, 3초 동안 트랜잭션 유지후 commit 
3. Thread-2 (read operation): Thread A 변경 작업 후 조회 작업 실행, isolation 값에 따라 어떤 값으로 보이는지 확인

### 격리 수준 별 결과

| 격리 수준            | Thread-2가 읽는 값 (트랜잭션 중에)           | Thread-2가 읽는 값 (트랜잭션 후) |
|------------------|------------------------------------|-------------------------|
| READ_UNCOMMITTED | "updated" (Dirty Read)             | "updated"               |
| READ_COMMITTED   | "initial" (Dirty Read 방지)          | "updated"               |
| REPEATABLE_READ  | "initial" (Non-repeatable Read 방지) | "updated"               |
| SERIALIZABLE     | 차단됨 (Phantom Read 방지)              | "updated"               |

### READ_COMMITTED: Thread B가 Thread A의 변경 사항을 커밋 전까지 보지 못함
1️⃣ Thread-1 (Write Transaction) 시작
``` text
[pool-2-thread-1] o.m.t.config.MyTransactionManager : Starting transaction with propagation: REQUIRES_NEW in thread: pool-2-thread-1
[pool-2-thread-1] o.m.t.config.MyTransactionManager : New writable connection created for thread: pool-2-thread-1
```
2️⃣ Thread-2 (Read Transaction) 시작
```text
[pool-2-thread-2] o.m.t.config.MyTransactionManager  : Starting transaction with propagation: REQUIRES_NEW in thread: pool-2-thread-2
[pool-2-thread-2] o.m.t.config.MyTransactionManager  : New read-only connection created for thread: pool-2-thread-2
```

3️⃣ Thread-2에서 데이터 읽기 (Read Transaction)
```text
[pool-2-thread-2] o.m.transactional.service.IsolationTest : Read Transaction: initial
```
- ✅Read_Committed 격리 수준에서는 커밋되지 않은 변경 사항을 읽을 수 없음

4️⃣ Thread-2 (Read Transaction) 종료
```text
[pool-2-thread-2] o.m.t.config.MyTransactionManager : Checking connection read-only state: false, isWriteOperation: false
[pool-2-thread-2] o.m.t.config.MyTransactionManager : Clearing transaction for thread: pool-2-thread-2
```

5️⃣ Thread-1 (Write Transaction) 커밋
```text
[pool-2-thread-1] o.m.t.config.MyTransactionManager : Checking connection read-only state: false, isWriteOperation: true
[pool-2-thread-1] o.m.t.config.MyTransactionManager : Clearing transaction for thread: pool-2-thread-1
```
- Read_Committed에서는 이때부터 Thread-2가 변경 사항을 읽을 수 있음

### REPEATABLE_READ: Thread-2이 같은 데이터를 두 번 읽을 때 **동일한 값이 유지**

3️⃣ Thread-1이 데이터를 변경 후 커밋
- initial -> updated 로 변경함
- 하지만 아직 Thread-2는 읽기 트랜잭션 중이므로 변경 사항을 읽어서는 안 됨

4️⃣ Thread-2 첫 번째 조회 (변경 전 데이터 읽기)
```text
[pool-2-thread-2] o.m.transactional.service.IsolationTest  : [1] Read Transaction: initial
```
- Thread-1이 변경했지만 커밋되지 않았기 때문에 'initial' 값이 유지

5️⃣ Thread-1이 커밋
```text
[pool-2-thread-1] o.m.t.config.MyTransactionManager : Checking connection read-only state: false, isWriteOperation: true
[pool-2-thread-1] o.m.t.config.MyTransactionManager : Clearing transaction for thread: pool-2-thread-1
```
- 새로운 트랜잭션에서만 조회시 updated로 보여져야 함

6️⃣ Thread-2 두 번째 조회 (같은 트랜잭션)
```text
[pool-2-thread-2] o.m.transactional.service.IsolationTest  : [2] Read Transaction: initial
```
- 두 번째 조회에서도 'initial' 값을 반환
- 같은 트랜잭션 내에서는 값이 바뀌지 않음

7️⃣ Thread-2 트랜잭션 종료 후 새로운 트랜잭션에서 조회
```text
[pool-2-thread-2] o.m.t.config.MyTransactionManager : Starting transaction with propagation: REQUIRES_NEW in thread: pool-2-thread-2
[pool-2-thread-2] o.m.t.config.MyTransactionManager : New read-only connection created for thread: pool-2-thread-2
[pool-2-thread-2] o.m.transactional.service.IsolationTest  : [3] Read Transaction: updated
```
- 새로운 트랜잭션에서 변경된 데이터(updated)가 조회

### SERIALIZABLE: Thread-2이 Thread-1가 실행 중인 동안 해당 레코드에 **접근하지 못하고 차단**

3️⃣ Thread-1이 데이터를 변경 후 5초 대기
```text
[pool-2-thread-1] o.m.transactional.service.IsolationTest : [Isolation: SERIALIZABLE] Write Transaction: Updated value to 'updated'
[pool-2-thread-1] o.m.transactional.service.IsolationTest : [Isolation: SERIALIZABLE] Write Transaction: Sleeping for 5s before commit...
```
- 데이터 변경후 바로 커밋되지 않은 상태
- SERIALIZABLE 격리 수준에서는 다른 트랜잭션이 이 레코드에 접근할 경우 블로킹될 수 있음

4️⃣ Thread-2 첫 번째 조회 (변경 전 데이터 읽기)
```text
[pool-2-thread-2] o.m.transactional.service.IsolationTest  : [1] Read Transaction: initial
```
- Thread-1의 변경 사항이 보이지 않음
- SERIALIZABLE에서는 다른 트랜잭션이 커밋되기 전까지 변경 사항이 반영되지 않음

5️⃣ Thread-2 두 번째 조회 (같은 트랜잭션)
```text
[pool-2-thread-2] o.m.transactional.service.IsolationTest  : [2] Read Transaction: initial
```
- 같은 트랜잭션 내에서는 데이터가 변경되지 않음

6️⃣ Thread-2 트랜잭션 종료 후 새로운 트랜잭션에서 조회
- SERIALIZABLE 격리 수준에서는 다른 트랜잭션이 커밋되지 않으면 기존 데이터를 유지

7️⃣ ⚠️Thread-1이 커밋
- 커밋을 완료하여 변경된 내용이 반영됨