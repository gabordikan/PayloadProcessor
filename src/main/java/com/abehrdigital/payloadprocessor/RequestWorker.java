package com.abehrdigital.payloadprocessor;

import com.abehrdigital.payloadprocessor.models.Request;
import com.abehrdigital.payloadprocessor.models.RequestRoutine;
import com.abehrdigital.payloadprocessor.models.RequestRoutineExecution;
import com.abehrdigital.payloadprocessor.utils.DaoFactory;
import com.abehrdigital.payloadprocessor.utils.RoutineScriptAccessor;
import com.abehrdigital.payloadprocessor.utils.Status;

import javax.persistence.OptimisticLockException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.abehrdigital.payloadprocessor.utils.StackTraceUtil.getStackTraceAsString;

public class RequestWorker implements Runnable {
    private int requestId;
    private final RequestThreadListener threadListener;
    private RequestWorkerService service;
    private int successfulRoutineCount;
    private int failedRoutineCount;
    private String logMessage;

    public RequestWorker(int requestId, String requestQueueName,
                         RequestThreadListener threadListener,
                         RoutineScriptAccessor routineScriptAccessor) {
        this.requestId = requestId;
        this.threadListener = threadListener;
        service = new RequestWorkerService(
                DaoFactory.createScriptEngineDaoManager(),
                requestId,
                requestQueueName,
                routineScriptAccessor
        );
        successfulRoutineCount = 0;
        failedRoutineCount = 0;
    }

    @Override
    public void run() {
        try {
            while (true) {
                service.beginTransaction();
                service.clearCache();
                Request lockedRequest = service.getRequestWithLock();

                if (lockedRequest == null) {
                    throw new Exception("Failed to lock the request row");
                }

                RequestRoutine routineForProcessing = service.getNextRoutineToProcess();
                if (routineForProcessing != null) {
                    resetRoutineVariables();
                    String engineLogMessage = executeRequestRoutine(routineForProcessing);
                    evaluateRoutineScriptExecution(engineLogMessage, routineForProcessing);
                } else {
                    break;
                }
            }
        } catch (Exception exception) {
            Logger.getLogger(RequestWorker.class.getName()).log(Level.SEVERE, exception.toString());
        } finally {
            service.shutDown();
            threadListener.deQueue(requestId, successfulRoutineCount, failedRoutineCount);
        }
    }

    private void resetRoutineVariables() {
        logMessage = "";
    }

    private String executeRequestRoutine(RequestRoutine routineForProcessing) {

        try {
            JavascriptScriptExecutor scriptExecutor = new JavascriptScriptExecutor(
                    service.getRoutineBody(routineForProcessing.getRoutineName()),
                    service.getScriptService()
            );
            routineForProcessing.setHashCode(scriptExecutor.getScriptHashCode());
            logMessage += scriptExecutor.execute();
            routineForProcessing.updateFieldsByStatus(Status.COMPLETE);
            service.updateRequestRoutine(routineForProcessing);
            //Request table lock released when transaction is committed
            service.commit();
        } catch (OptimisticLockException lockException) {
            // We dont update the routine if the exception is optimistic lock as the request was changed while
            // we we're executing the engine and we don't know what the changes were
            // so we leave it as it is and it will run again if it has the right conditions
            service.rollback();
            logMessage += getStackTraceAsString(lockException);
        } catch (Exception | Error exception) {
            processWorkerError(routineForProcessing,  getStackTraceAsString(exception));
        }

        return logMessage;
    }

    private void processWorkerError(RequestRoutine routineForProcessing, String stackTraceAsString) {
        service.rollback();
        Logger.getLogger(RequestWorker.class.getName()).log(Level.SEVERE,
                "REQUEST WORKER ERROR WHEN EVALUATING JAVASCRIPT ->  " + stackTraceAsString);
        logMessage += stackTraceAsString;
        routineForProcessing.updateFieldsByStatus(Status.FAILED);
    }

    private void evaluateRoutineScriptExecution(String logMessage, RequestRoutine routineForProcessing) {
        try {
            service.beginTransaction();
            if (routineForProcessing.getStatus() == Status.FAILED || routineForProcessing.getStatus() == Status.RETRY) {
                service.updateRequestRoutine(routineForProcessing);
                failedRoutineCount++;
            } else if (routineForProcessing.getStatus() == Status.COMPLETE) {
                successfulRoutineCount++;
            }
            RequestRoutineExecution routineExecution = createRequestExecution(routineForProcessing, logMessage);
            service.saveRequestRoutineExecution(routineExecution);
            service.commit();
        } catch (Exception exception) {
            service.rollback();
            Logger.getLogger(RequestWorker.class.getName(), getStackTraceAsString(exception));
        }
    }

    private RequestRoutineExecution createRequestExecution(RequestRoutine routineForProcessing, String logMessage) {
        return new RequestRoutineExecution(
                logMessage,
                routineForProcessing.getId(),
                new java.sql.Timestamp(Calendar.getInstance().getTimeInMillis()),
                routineForProcessing.getStatus(),
                routineForProcessing.getTryCount()
        );
    }
}