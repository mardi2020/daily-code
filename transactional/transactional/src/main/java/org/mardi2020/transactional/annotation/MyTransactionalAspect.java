package org.mardi2020.transactional.annotation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.mardi2020.transactional.config.Isolation;
import org.mardi2020.transactional.config.MyTransactionManager;
import org.mardi2020.transactional.config.Propagation;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MyTransactionalAspect {

    private final MyTransactionManager transactionManager;

    private final Map<String, Boolean> transactionStatus = new ConcurrentHashMap<>(); // 동기화

    @Before(value = "@annotation(myTransactional)")
    public void beforeLog(JoinPoint joinPoint, MyTransactional myTransactional) {
        log.info("Applying MyTransactional with propagation: {}, readOnly: {}",
                myTransactional.propagation(), myTransactional.readOnly());
    }

    @Around(value = "@annotation(myTransactional)", argNames = "joinPoint,myTransactional")
    public Object around(ProceedingJoinPoint joinPoint, MyTransactional myTransactional) throws Throwable {
        final String methodName = joinPoint.getSignature().getName();
        final boolean isWriteOperation = isWriteOperation(methodName);
        boolean newTransaction = false;

        try {
            final boolean readOnly = myTransactional.readOnly();
            final Propagation propagation = myTransactional.propagation();
            final Isolation isolation = myTransactional.isolation();

            if (isWriteOperation && readOnly) {
                throw new SQLException("This transaction is read-only");
            }

            if (propagation == Propagation.NEVER) {
                if (transactionManager.getConnection(true, isWriteOperation) != null) {
                    throw new SQLException("Transaction exists, but NEVER propagation specified");
                }
                log.info("Executing method without transaction due to NEVER propagation.");
            } else if (propagation == Propagation.REQUIRES_NEW) {
                transactionManager.startTransaction(propagation, readOnly, isolation);
                newTransaction = true;
            } else if (propagation == Propagation.REQUIRED) {
                if (transactionManager.getConnection(true, isWriteOperation) == null) {
                    transactionManager.startTransaction(propagation, readOnly, isolation);
                    newTransaction = true;
                }
            }
            transactionStatus.put(methodName, newTransaction);
            return joinPoint.proceed();
        } finally {
            if (newTransaction) {
                transactionManager.endTransaction();
            }
        }
    }

    @AfterReturning(value = "@annotation(myTransactional)")
    public void afterReturningTx(JoinPoint joinPoint, MyTransactional myTransactional) throws Throwable {
        final String methodName = joinPoint.getSignature().getName();
        final boolean isWriteOperation = isWriteOperation(methodName);

        if (transactionStatus.getOrDefault(methodName, false)) {
            transactionManager.commitTransaction(isWriteOperation);
            log.info("Transaction committed for method: {}", methodName);
            transactionStatus.remove(methodName); // 트랜잭션 상태 제거
        }
    }

    @AfterThrowing(value = "@annotation(myTransactional)")
    public void afterThrowingTx(JoinPoint joinPoint, MyTransactional myTransactional) throws Throwable {
        final String methodName = joinPoint.getSignature().getName();
        final boolean isWriteOperation = isWriteOperation(methodName);
        // 에외 발생시 트랜잭션 롤백 처리
        if (transactionStatus.getOrDefault(methodName, false)) {
            transactionManager.rollbackTransaction(isWriteOperation);
            log.info("Transaction rolled back for method: {}", methodName);
            transactionStatus.remove(methodName);
        }
    }

    @After(value = "@annotation(myTransactional)")
    public void afterTx(JoinPoint joinPoint, MyTransactional myTransactional) {
        final String methodName = joinPoint.getSignature().getName();
        // 트랜잭션 종료 처리
        if (transactionStatus.remove(methodName) != null) {
            transactionManager.endTransaction();
            log.info("Transaction ended for method: {}", methodName);
        }
    }

    private boolean isWriteOperation(final String methodName) {
        return methodName.startsWith("add") || methodName.equals("update") || methodName.equals("delete");
    }
}
