package com.xxxx.mail;

import com.rabbitmq.client.Channel;
import com.xxxx.server.pojo.Employee;
import com.xxxx.server.pojo.MailConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Date;
/**
 * 消息接收者
 */
@Component
public class MailReceiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailReceiver.class);

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private MailProperties mailProperties;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private RedisTemplate redisTemplate;

    @RabbitListener(queues = MailConstants.MAIL_QUEUE_NAME)
    public void handler(Message message, Channel channel){
        Employee employee = (Employee) message.getPayload();
        MessageHeaders headers = message.getHeaders();
        //消息序号
        Long tag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);
        String msgId = (String) headers.get("spring_returned_message_correlation");
        HashOperations hashOperations = redisTemplate.opsForHash();

        try {
            if(hashOperations.entries("mail_log").containsKey(msgId)){
                LOGGER.error("消息以及被消费=======>{}",msgId);
                /**
                 * 手动确认消息
                 * tag：消息序号
                 * multiple：是否确认多条
                 */
                channel.basicAck(tag,false);
                return;
            }
            MimeMessage msg = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg);
            //发件人
            helper.setFrom(mailProperties.getUsername());
            //收件人
            helper.setTo(employee.getEmail());
            //主题
            helper.setSubject("入职欢迎邮件");
            //发送日期
            helper.setSentDate(new Date());
            //邮件内容
            Context context = new Context();
            context.setVariable("name",employee.getName());
            context.setVariable("posName",employee.getPosition().getName());
            context.setVariable("joblevelName",employee.getJoblevel().getName());
            context.setVariable("departmentName",employee.getDepartment().getName());
            String mail = templateEngine.process("mail", context);
            helper.setText(mail,true);
            //发送邮件
            javaMailSender.send(msg);
            LOGGER.info("邮件发送成功");
            //将消息id存入redis
            hashOperations.put("mail_log",msgId,"OK");
           //手动确认消息
            channel.basicAck(tag,false);
        } catch (Exception e) {
            try {
                channel.basicAck(tag,false);
            } catch (IOException ex) {
                LOGGER.error("邮件发送失败======>{}",e.getMessage());
            }
            LOGGER.error("邮件发送失败======>{}",e.getMessage());
        }
    }

}
