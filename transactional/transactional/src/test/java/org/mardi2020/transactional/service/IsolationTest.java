package org.mardi2020.transactional.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mardi2020.transactional.config.Isolation;
import org.mardi2020.transactional.config.MyTransactionManager;
import org.mardi2020.transactional.config.Propagation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class IsolationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MyTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        this.transactionManager = new MyTransactionManager(dataSource);
    }

    @Test
    @DisplayName("🚀read committed 테스트")
    void Read_Committed() throws InterruptedException, SQLException {
        runIsolationTest(Isolation.READ_COMMITTED);
    }

    @Test
    @DisplayName("🚀repeatable read 테스트")
    void RepeatableRead() throws InterruptedException, SQLException {
        runIsolationTest(Isolation.REPEATABLE_READ);
    }

    @Test
    @DisplayName("🚀serializable 테스트")
    void Serializable() throws InterruptedException, SQLException {
        runIsolationTest(Isolation.SERIALIZABLE);
    }

    private void runIsolationTest(Isolation isolation) throws InterruptedException, SQLException {
        ExecutorService executor = Executors.newFixedThreadPool(2); // 2개의 스레드 사용
        CountDownLatch latch = new CountDownLatch(1);

        initializeTestData();

        executor.submit(() -> {
            try {
                performWriteTx(isolation, latch);
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        });

        executor.submit(() -> {
            try {
                performReadTx(isolation, latch);
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        });

        executor.shutdown();
        Thread.sleep(7_000);
    }

    private void initializeTestData() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM items; INSERT INTO items (id, name) VALUES (1, 'initial');"
             )) {
            statement.executeUpdate();
            connection.commit();
        }
    }

    private void performWriteTx(Isolation isolation, CountDownLatch latch) throws SQLException {
        transactionManager.startTransaction(Propagation.REQUIRES_NEW, false, isolation);
        Connection connection = transactionManager.getConnection(false, true);

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE items SET name = 'updated' WHERE id = 1"
        )) {
            statement.executeUpdate();
            log.info("[Isolation: {}] Write Transaction: Updated value to 'updated'", isolation);

            latch.countDown(); // 트랜잭션 커밋 전 읽기 트랜잭션 시작하도록 신호

            // SERIALIZABLE 일 경우 읽기 트랜잭션이 대기하는지 확인
            if (isolation == Isolation.SERIALIZABLE) {
                log.info("[Isolation: {}] Write Transaction: Sleeping for 5s before commit...", isolation);
                Thread.sleep(5_000); // 읽기 트랜잭션이 블로킹되는지 확인
            } else {
                Thread.sleep(3_000); // 기본적으로 3초 대기 후 커밋
            }
            transactionManager.commitTransaction(true);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void performReadTx(Isolation isolation, CountDownLatch latch) throws SQLException {
        transactionManager.startTransaction(Propagation.REQUIRES_NEW, true, isolation);
        Connection connection = transactionManager.getConnection(false, false);

        try {
            latch.await(); // write 트랜잭션 진행되고 나서 실행

            /* 첫번째 read */
            final String firstRead = readValue(connection);
            log.info("[1] Read Transaction: {}", firstRead);

            // thread1에서 변경사항 커밋할때까지 대기
            Thread.sleep(3_000);

            /* 두번쨰 read */
            final String secondRead = readValue(connection);
            log.info("[2] Read Transaction: {}", secondRead);

            // 같은 트랜잭션 내에서는 REPEATABLE_READ 가 보장되어야 함
            assert isolation != Isolation.REPEATABLE_READ && isolation != Isolation.SERIALIZABLE
                    || Objects.requireNonNull(firstRead).equals(secondRead) :
                    "REPEATABLE_READ or SERIALIZABLE failed! Values changed within the same transaction";

            // 같은 트랜잭션 종료 후 새로운 트랜잭션 시작 (다른 트랜잭션에서 조회)
            transactionManager.commitTransaction(false);
            transactionManager.startTransaction(Propagation.REQUIRES_NEW, true, isolation);
            connection = transactionManager.getConnection(false, false);

            /* 세번째 read */
            final String thirdRead = readValue(connection);
            log.info("[3] Read Transaction: {}", thirdRead);

            switch (isolation) {
                case READ_UNCOMMITTED:
                    assert !Objects.requireNonNull(firstRead).equals(secondRead) :
                            "READ_UNCOMMITTED allows dirty reads, values should change";
                    break;
                case READ_COMMITTED:
                    assert Objects.requireNonNull(firstRead).equals("initial")
                            && Objects.requireNonNull(secondRead).equals("updated") :
                            "READ_COMMITTED should see committed updates";
                    break;
                case REPEATABLE_READ:
                    assert Objects.requireNonNull(firstRead).equals(secondRead) :
                            "REPEATABLE_READ should return the same value twice";
                    break;
                case SERIALIZABLE:
                    assert Objects.requireNonNull(firstRead).equals(secondRead) :
                            "SERIALIZABLE should return the same value twice";
                    break;
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            transactionManager.commitTransaction(false);
        }
    }

    private String readValue(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM items WHERE id = 1"
        )) {
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next() ? resultSet.getString("name") : null;
        }
    }
}
