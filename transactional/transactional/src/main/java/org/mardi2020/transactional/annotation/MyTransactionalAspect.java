package org.mardi2020.transactional.annotation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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
        boolean newTransaction = false;
        try {
            if (myTransactional.propagation() == Propagation.NEVER) {
                if (transactionManager.getConnection(true) != null) {
                    throw new SQLException("Transaction exists, but NEVER propagation specified");
                }
                log.info("Executing method without transaction due to NEVER propagation.");
            }
            else if (myTransactional.propagation() == Propagation.REQUIRES_NEW) {
                transactionManager.startTransaction(myTransactional.propagation());
                newTransaction = true;
            }
            else if (myTransactional.propagation() == Propagation.REQUIRED) {
                if (transactionManager.getConnection(true) == null) {
                    transactionManager.startTransaction(myTransactional.propagation());
                    newTransaction = true;
                }
            }

            Object result = joinPoint.proceed();

            if (newTransaction) {
                transactionManager.commitTransaction();
            }
            return result;
        } catch (Exception e) {
            if (newTransaction) {
                transactionManager.rollbackTransaction();
            }
            throw e;
        } finally {
            if (newTransaction) {
                transactionManager.endTransaction();
            }
        }
    }
}
