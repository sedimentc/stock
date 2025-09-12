package com.ailu.exchange;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@Slf4j
@EnableScheduling
@SpringBootApplication
@MapperScan(basePackages = "com.ailu.exchange.dao.mapper")
public class ExchangeInfoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExchangeInfoApplication.class, args);
    }
}
