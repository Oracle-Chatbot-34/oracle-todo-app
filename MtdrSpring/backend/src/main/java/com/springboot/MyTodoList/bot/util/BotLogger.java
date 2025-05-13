package com.springboot.MyTodoList.bot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around SLF4J logger for standardized bot-related logging
 */
public class BotLogger {
    private final Logger logger;

    public BotLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    // Chat and user-specific logging
    
    public void info(long chatId, String message) {
        logger.info("[Chat ID: {}] {}", chatId, message);
    }
    
    public void info(long chatId, String message, Object... args) {
        Object[] newArgs = new Object[args.length + 1];
        newArgs[0] = chatId;
        System.arraycopy(args, 0, newArgs, 1, args.length);
        logger.info("[Chat ID: {}] " + message, newArgs);
    }
    
    public void debug(long chatId, String message) {
        logger.debug("[Chat ID: {}] {}", chatId, message);
    }
    
    public void debug(long chatId, String message, Object... args) {
        Object[] newArgs = new Object[args.length + 1];
        newArgs[0] = chatId;
        System.arraycopy(args, 0, newArgs, 1, args.length);
        logger.debug("[Chat ID: {}] " + message, newArgs);
    }
    
    public void warn(long chatId, String message) {
        logger.warn("[Chat ID: {}] {}", chatId, message);
    }
    
    public void warn(long chatId, String message, Object... args) {
        Object[] newArgs = new Object[args.length + 1];
        newArgs[0] = chatId;
        System.arraycopy(args, 0, newArgs, 1, args.length);
        logger.warn("[Chat ID: {}] " + message, newArgs);
    }
    
    public void error(long chatId, String message) {
        logger.error("[Chat ID: {}] {}", chatId, message);
    }
    
    public void error(long chatId, String message, Throwable t) {
        logger.error("[Chat ID: {}] {}", chatId, message, t);
    }
    
    public void error(long chatId, String message, Object... args) {
        Object[] newArgs = new Object[args.length + 1];
        newArgs[0] = chatId;
        System.arraycopy(args, 0, newArgs, 1, args.length);
        logger.error("[Chat ID: {}] " + message, newArgs);
    }

    // Standard logging methods
    
    public void info(String message) {
        logger.info(message);
    }
    
    public void info(String message, Object... args) {
        logger.info(message, args);
    }
    
    public void debug(String message) {
        logger.debug(message);
    }
    
    public void debug(String message, Object... args) {
        logger.debug(message, args);
    }
    
    public void warn(String message) {
        logger.warn(message);
    }
    
    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }
    
    public void error(String message) {
        logger.error(message);
    }
    
    public void error(String message, Throwable t) {
        logger.error(message, t);
    }
    
    public void error(String message, Object... args) {
        logger.error(message, args);
    }
}