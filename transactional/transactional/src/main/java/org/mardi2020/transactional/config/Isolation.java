package org.mardi2020.transactional.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;

@RequiredArgsConstructor
public enum Isolation {

    DEFAULT(Connection.TRANSACTION_READ_COMMITTED),
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE)
    ;

    @Getter
    private final int level;

}
