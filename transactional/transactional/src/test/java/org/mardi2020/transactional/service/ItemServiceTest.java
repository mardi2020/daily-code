package org.mardi2020.transactional.service;

import org.junit.jupiter.api.*;
import org.mardi2020.transactional.config.MyTransactionManager;
import org.mardi2020.transactional.config.Propagation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
class ItemServiceTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private MyTransactionManager transactionManager;

    @BeforeEach
    void clean() throws SQLException {
        transactionManager.startTransaction(Propagation.REQUIRED);
        Connection connection = transactionManager.getConnection(false);
        PreparedStatement statement = connection.prepareStatement("DELETE FROM items");
        statement.executeUpdate();
        transactionManager.commitTransaction();
    }

    @AfterEach
    void tearDown() throws SQLException {
        transactionManager.endTransaction();
    }

    @Test
    @DisplayName("REQUIRED - 현재 트랜잭션이 있으면 참여하고 없다면 새 트랜잭션 시작")
    void testRequiredPropagation() throws SQLException {
        itemService.addItemWithRequired("Ice cream");
        List<String> items = itemService.getItems();
        assertEquals(1, items.size());
        assertEquals("Ice cream", items.get(0));
    }

    @Test
    @DisplayName("REQUIRES_NEW - 항상 새 트랜잭션을 시작하고 기존 트랜잭션은 보류")
    void testRequiresNewPropagation() throws SQLException {
        itemService.addItemWithRequired("Snack");
        itemService.addItemWithRequiredNew("Coffee");

        List<String> items = itemService.getItems();
        assertEquals(2, items.size());
    }

    @Test
    @DisplayName("NEVER - 트랜잭션이 있으면 예외 발생")
    void testNeverPropagation() throws SQLException {
        transactionManager.startTransaction(Propagation.REQUIRED); // 트랜잭션 존재

        try {
            // 트랜잭션이 존재하는 상태에서 Apple을 넣으려고 시도
            Exception e = assertThrows(SQLException.class, () -> itemService.addItemWithNever("Apple"),
                    "Expected SQLException when using NEVER propagation but transaction exists.");

            assertEquals("Transaction exists, but NEVER propagation specified", e.getMessage());
        } finally {
            transactionManager.commitTransaction();
        }
    }

    @Test
    @DisplayName("NEVER - 기존의 트랜잭션이 없으면 트랜잭션 없이 실행")
    void testNeverPropagationWithoutTransaction() throws SQLException {
        assertDoesNotThrow(() -> itemService.addItemWithNever("Water"));

        List<String> items = itemService.getItems();
        assertTrue(items.isEmpty());
    }

    @Test
    @DisplayName("MANDATORY - 반드시 기존 트랜잭션이 있어야하고 없으면 예외 발생")
    void testMandatoryPropagationWithoutTransaction() {
        Assertions.assertThrows(SQLException.class, () -> itemService.addItemWithMandatory("Banana"));
    }

    @Test
    @DisplayName("SUPPORTS - 현재 트랜잭션이 있으면 참여하고 없다면 트랜잭션 없이 실행")
    void testSupportsPropagationWithoutTransaction() throws SQLException {
        Assertions.assertDoesNotThrow(() -> itemService.addItemWithSupports("Soda"));

        List<String> items = itemService.getItems();
        assertTrue(items.isEmpty(), "SUPPORTS 전파 시 트랜잭션이 없으면 데이터가 삽입되지 않아야 합니다.");
    }

    @Test
    @DisplayName("NOT_SUPPORTED - 현재 트랜잭션을 중단하고 트랜잭션 없이 실행")
    void testNotSupportedPropagation() throws SQLException {
        Assertions.assertDoesNotThrow(() -> itemService.addItemWithNotSupported("Coke"));

        List<String> items = itemService.getItems();
        assertTrue(items.isEmpty(), "NOT_SUPPORTED 전파 시 트랜잭션이 없으면 데이터가 삽입되지 않아야 합니다.");
    }
}