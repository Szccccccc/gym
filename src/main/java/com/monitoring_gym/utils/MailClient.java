package com.monitoring_gym.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import javax.mail.internet.MimeMessage;

@Component
@Slf4j
public class MailClient {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from; // 发件方的邮箱
    // 参数依次为：收件方的邮箱、邮件主题、邮件内容

    @Async
    public void sendMail(String to, String subject, String content) {
        try {
            log.info("发送邮件中");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(helper.getMimeMessage());
            log.info("邮件发送成功");
        } catch (Exception e) {
            log.error("发送邮件失败："+e.getMessage());
        }
    }
}