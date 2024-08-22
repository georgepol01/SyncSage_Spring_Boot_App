//package com.syncsage.syncsage.aspect;
//
//import org.aspectj.lang.JoinPoint;
//import org.aspectj.lang.annotation.AfterThrowing;
//import org.aspectj.lang.annotation.Aspect;
//import org.openqa.selenium.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Component;
//
//import java.text.ParseException;
//import java.time.format.DateTimeParseException;
//
//@Aspect
//@Component
//public class ExceptionHandlingAspect {
//
//    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlingAspect.class);
//    private static final long RETRY_DELAY = 2000;
//
//    @AfterThrowing(pointcut = "execution(* com.syncsage.syncsage.service.*(..))", throwing = "ex")
//    public void handleAirbnbSyncExceptions(JoinPoint joinPoint, Throwable ex) throws Throwable {
//        String methodName = joinPoint.getSignature().getName();
//
//        if (ex instanceof NoSuchElementException) {
//            handleNoSuchElementException((NoSuchElementException) ex, joinPoint);
//        } else if (ex instanceof ElementClickInterceptedException || ex instanceof StaleElementReferenceException) {
//            handleClickInterceptedOrStaleElementException(ex, joinPoint);
//        } else if (ex instanceof WebDriverException) {
//            if (ex instanceof TimeoutException) {
//                handleTimeoutException((TimeoutException) ex, joinPoint);
//            } else {
//                handleWebDriverException((WebDriverException) ex, joinPoint);
//            }
//        } else if (ex instanceof DateTimeParseException || ex instanceof ParseException) {
//            handleDateTimeParseException(ex, joinPoint);
//        } else {
//            logger.error("Unexpected error in method {}: {}", methodName, ex.getMessage(), ex);
//            throw ex;
//        }
//    }
//
//    private void handleTimeoutException(TimeoutException ex, JoinPoint joinPoint) {
//        logger.error("Timeout occurred while waiting for an element in method {}: {}",
//                joinPoint.getSignature().getName(), ex.getMessage());
//        throw ex;
//    }
//
//    private void handleWebDriverException(WebDriverException ex, JoinPoint joinPoint) throws Throwable {
//        logger.error("WebDriverException in method {}: {}. Retrying with delay...",
//                joinPoint.getSignature().getName(), ex.getMessage());
//
//        Thread.sleep(RETRY_DELAY);
//        throw ex;
//    }
//
//    private void handleDateTimeParseException(Throwable ex, JoinPoint joinPoint) throws Throwable {
//        logger.error("Date parsing error in method {}: {}",
//                joinPoint.getSignature().getName(), ex.getMessage());
//        throw ex;
//    }
//
//    private void handleClickInterceptedOrStaleElementException(Throwable ex, JoinPoint joinPoint) throws Throwable {
//        logger.warn("Click intercepted or element became stale in method {}: {}. Retrying...",
//                joinPoint.getSignature().getName(), ex.getMessage());
//
//        throw ex;
//    }
//
//    private void handleNoSuchElementException(NoSuchElementException ex, JoinPoint joinPoint) {
//        logger.warn("NoSuchElementException in method {}: {}. Retrying after scrolling...",
//                joinPoint.getSignature().getName(), ex.getMessage());
//
//        throw ex;
//    }
//
//
//}
