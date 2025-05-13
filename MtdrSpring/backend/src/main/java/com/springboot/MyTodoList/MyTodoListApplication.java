package com.springboot.MyTodoList;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.springboot.MyTodoList.bot.config.TelegramBotConfig;

@SpringBootApplication
@Import(TelegramBotConfig.class)
public class MyTodoListApplication {
    private static final Logger logger = LoggerFactory.getLogger(MyTodoListApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(MyTodoListApplication.class, args);
        logger.info("DashMaster application started successfully");
    }
}