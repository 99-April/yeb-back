package com.xxxx.mail;

import com.xxxx.server.pojo.MailConstants;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;


@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
class MailApplication {
    public static void main(String[] args){
        SpringApplication.run(MailApplication.class,args);
    }

    @Bean
    public Queue queue(){
        return new org.springframework.amqp.core.Queue(MailConstants.MAIL_QUEUE_NAME);

    }
}
