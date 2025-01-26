package org.mardi2020.transactional.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mardi2020.transactional.annotation.MyTransactional;
import org.mardi2020.transactional.config.MyTransactionManager;
import org.mardi2020.transactional.config.Propagation;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final MyTransactionManager transactionManager;

    @MyTransactional(readOnly = true)
    public List<String> getItems() throws SQLException {
        Connection connection = transactionManager.getConnection(false);
        PreparedStatement statement = connection.prepareStatement("SELECT name FROM items");
        ResultSet resultSet = statement.executeQuery();

        List<String> items = new ArrayList<>();
        while (resultSet.next()) {
            items.add(resultSet.getString("name"));
        }
        return items;
    }

    @MyTransactional
    public void addItemWithRequired(String item) throws SQLException {
        addItem(item, false);
    }

    @MyTransactional(propagation = Propagation.REQUIRES_NEW)
    public void addItemWithRequiredNew(String item) throws SQLException {
        addItem(item, false);
    }

    @MyTransactional(propagation = Propagation.NEVER)
    public void addItemWithNever(String item) throws SQLException {
        addItem(item, true);
    }

    @MyTransactional(propagation = Propagation.MANDATORY)
    public void addItemWithMandatory(String item) throws SQLException {
        addItem(item, false);
    }

    @MyTransactional(propagation = Propagation.NOT_SUPPORTED)
    public void addItemWithNotSupported(String item) throws SQLException {
        addItem(item, true);
    }

    @MyTransactional(propagation = Propagation.SUPPORTS)
    public void addItemWithSupports(String item) throws SQLException {
        addItem(item, true);
    }

    private void addItem(final String item, final boolean allowNoTransaction) throws SQLException {
        Connection connection = transactionManager.getConnection(allowNoTransaction);
        if (connection == null) {
            log.warn("No active transaction, skipping database operation.");
            return;  // 트랜잭션 없이 실행을 허용해야 함
        }
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO items (name) VALUES (?)")) {
            statement.setString(1, item);
            statement.executeUpdate();
        }
    }
}
