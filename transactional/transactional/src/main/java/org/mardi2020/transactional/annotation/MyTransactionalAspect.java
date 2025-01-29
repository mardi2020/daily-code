package org.mardi2020.transactional.annotation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.mardi2020.transactional.config.Isolation;
import org.mardi2020.transactional.config.MyTransactionManager;
import org.mardi2020.transactional.config.Propagation;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MyTransactionalAspect {

    private final MyTransactionManager transactionManager;

    @Around(value = "@annotation(myTransactional)", argNames = "joinPoint,myTransactional")
    public Object around(ProceedingJoinPoint joinPoint, MyTransactional myTransactional) throws Throwable {
        log.info("Applying MyTransactional with propagation: {}, readOnly: {}",
                myTransactional.propagation(), myTransactional.readOnly());
        final boolean isWriteOperation = isWriteOperation(joinPoint);
        log.info("Checking if {} is a write operation: {}",
                joinPoint.getSignature().getName(), isWriteOperation(joinPoint));
        boolean newTransaction = false;

        try {
            final boolean readOnly = myTransactional.readOnly();
            final Propagation propagation = myTransactional.propagation();
            final Isolation isolation = myTransactional.isolation();

            if (isWriteOperation && readOnly) {
                throw new SQLException("This transaction is read-only");
            }

            if (myTransactional.propagation() == Propagation.NEVER) {
                if (transactionManager.getConnection(true, isWriteOperation) != null) {
                    throw new SQLException("Transaction exists, but NEVER propagation specified");
                }
                log.info("Executing method without transaction due to NEVER propagation.");
            }
            else if (myTransactional.propagation() == Propagation.REQUIRES_NEW) {
                transactionManager.startTransaction(propagation, readOnly, isolation);
                newTransaction = true;
            }
            else if (myTransactional.propagation() == Propagation.REQUIRED) {
                if (transactionManager.getConnection(true, isWriteOperation) == null) {
                    transactionManager.startTransaction(propagation, readOnly, isolation);
                    newTransaction = true;
                }
            }

            Object result = joinPoint.proceed();

            if (newTransaction) {
                transactionManager.commitTransaction(isWriteOperation);
            }
            return result;
        } catch (Exception e) {
            if (newTransaction) {
                transactionManager.rollbackTransaction(isWriteOperation);
            }
            throw e;
        } finally {
            if (newTransaction) {
                transactionManager.endTransaction();
            }
        }
    }

    private boolean isWriteOperation(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        return methodName.startsWith("add") || methodName.equals("update") || methodName.equals("delete");
    }
}
