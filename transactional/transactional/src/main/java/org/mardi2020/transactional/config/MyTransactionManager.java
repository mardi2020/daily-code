package org.mardi2020.transactional.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyTransactionManager {

    private static final ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();

    private static final ThreadLocal<Connection> suspendedConnectionThreadLocal = new ThreadLocal<>();

    private final DataSource dataSource;

    public void startTransaction(final Propagation propagation, final boolean readOnly) throws SQLException {
        log.info("Starting transaction with propagation: {} in thread: {}", propagation,
                Thread.currentThread().getName());
        Connection existingConnection = connectionThreadLocal.get();

        if (existingConnection != null && existingConnection.isClosed()) {
            connectionThreadLocal.remove();  // 닫힌 커넥션을 제거
            existingConnection = null;
        }

        switch (propagation) {
            case REQUIRED -> handleRequired(existingConnection, readOnly);
            case REQUIRES_NEW -> handleRequiresNew(existingConnection, readOnly);
            case SUPPORTS -> handleSupports(existingConnection);
            case MANDATORY -> handleMandatory(existingConnection);
            case NEVER -> handleNever(existingConnection);
            case NOT_SUPPORTED -> handleNotSupported(existingConnection);
        }
    }

    public void commitTransaction(final boolean isWriteOperation) throws SQLException {
        Connection connection = getConnection(false, isWriteOperation);
        if (connection != null) {
            connection.commit();
            connection.close();
            connectionThreadLocal.remove();
        }
        endTransaction();
    }

    public void rollbackTransaction(final boolean isWriteOperation) throws SQLException {
        Connection connection = getConnection(false, isWriteOperation);
        if (connection != null) {
            connection.rollback();
            connection.close();
            connectionThreadLocal.remove();
        }
        endTransaction();
    }

    public void endTransaction() {
        Connection suspendedConnection = suspendedConnectionThreadLocal.get();
        if (suspendedConnection != null) {
            connectionThreadLocal.set(suspendedConnection);
            suspendedConnectionThreadLocal.remove();
            log.info("Restored suspended transaction for thread: {}", Thread.currentThread().getName());
        } else {
            log.info("Clearing transaction for thread: {}", Thread.currentThread().getName());
            connectionThreadLocal.remove();
        }
    }

    public Connection getConnection(final boolean allowNoTransaction, final boolean isWriteOperation) throws SQLException {
        Connection connection = connectionThreadLocal.get();
        if (connection == null) {
            if (allowNoTransaction) {
                log.warn("No active transaction available, proceeding without connection.");
                return null;  // 트랜잭션 없이 진행할 수 있도록 null 반환
            }
            throw new SQLException("No active transaction available");
        }
        if (connection.isClosed()) {
            throw new SQLException("Connection is already closed");
        }
        log.info("Checking connection read-only state: {}, isWriteOperation: {}",
                connection.isReadOnly(), isWriteOperation);
        if (connection.isReadOnly() && isWriteOperation) { // readOnly 일때는 데이터의 변경이 불가능
            throw new SQLException("Cannot perform write operations in a read-only transaction");
        }
        return connection;
    }

    private void createNewConnection(final boolean readOnly) throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false); // 트랜잭션 자동 커밋 해제
        if (readOnly) {
            connection.setReadOnly(true);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        }
        connectionThreadLocal.set(connection);
        log.info("New {} connection created for thread: {}", readOnly ? "read-only" : "writable",
                Thread.currentThread().getName());
    }

    private void handleRequired(Connection existingConnection, final boolean readOnly) throws SQLException {
        // 트랜잭션이 없거나 닫혀있다면 생성
        if (existingConnection == null || existingConnection.isClosed()) {
            createNewConnection(readOnly);
            return;
        }
        connectionThreadLocal.set(existingConnection); // 기존 트랜잭션 사용
    }

    private void handleRequiresNew(Connection existingConnection, final boolean readOnly) throws SQLException {
        if (existingConnection != null && !existingConnection.isClosed()) {
            log.info("Suspending existing transaction for thread: {}", Thread.currentThread().getName());
            suspendedConnectionThreadLocal.set(existingConnection); // 보류된 트랜잭션 연결 따로 저장
            connectionThreadLocal.remove(); // 현재 연결 제거
        }
        createNewConnection(readOnly);
    }

    private void handleSupports(Connection existingConnection) throws SQLException {
        if (existingConnection == null || existingConnection.isClosed()) {
            log.info("No active transaction, proceeding without it in SUPPORTS propagation.");
            // 트랜잭션 없이 진행하므로 아무 작업도 하지 않음
            connectionThreadLocal.remove();
        } else {
            connectionThreadLocal.set(existingConnection);
        }
    }

    private void handleMandatory(Connection existingConnection) throws SQLException {
        if (existingConnection == null || existingConnection.isClosed()) {
            log.error("MANDATORY propagation requires an existing transaction, but none found.");
            throw new SQLException("MANDATORY propagation: Existing transaction required but not found");
        }
        connectionThreadLocal.set(existingConnection);
    }

    private void handleNever(Connection existingConnection) throws SQLException {
        if (existingConnection != null && !existingConnection.isClosed()) {
            log.error("Existing transaction detected in NEVER propagation. Throwing exception.");
            throw new SQLException("Transaction exists, but NEVER propagation specified");
        }
        connectionThreadLocal.remove();
        log.info("Proceeding without a transaction in NEVER propagation.");
    }

    private void handleNotSupported(Connection existingConnection) throws SQLException {
        if (existingConnection != null && !existingConnection.isClosed()) {
            suspendedConnectionThreadLocal.set(existingConnection);
            connectionThreadLocal.remove();
            log.info("Proceeding without a transaction in NOT_SUPPORTED propagation.");
        }
    }
}
