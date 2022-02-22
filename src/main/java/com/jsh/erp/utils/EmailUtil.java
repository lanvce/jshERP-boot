package com.jsh.erp.utils;

import com.jsh.erp.constants.BusinessConstants;
import org.apache.log4j.Logger;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

@Component
public class EmailUtil {
    private static final Logger logger = Logger.getLogger(EmailUtil.class);
    private static final String PROPERTIES_DEFAULT = "application.properties";
    private static String host;
    private static Integer port;
    private static String userName;
    private static String passWord;
    private static String emailForm;
    private static String timeOut;
    private static String cc;


    static{
        init();
    }

    private static JavaMailSenderImpl mailSender = createMailSender();

    /**
     * 初始化
     */
    private static void init() {
        Properties properties = new Properties();
        try{
            InputStream inputStream = EmailUtil.class.getClassLoader().getResourceAsStream(PROPERTIES_DEFAULT);
            properties.load(inputStream);
            inputStream.close();
            host = properties.getProperty("host");
            port = Integer.parseInt(properties.getProperty("port"));
            userName = properties.getProperty("userName");
            passWord = properties.getProperty("passWord");
            emailForm = properties.getProperty("emailForm");
            timeOut = properties.getProperty("timeOut");
            cc = properties.getProperty("cc");
        } catch(IOException e){
            logger.error(e.getMessage() , e);
        }
    }


    /**
     * 邮件发送器
     *
     * @return 配置好的工具
     */
    private static JavaMailSenderImpl createMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(userName);
        sender.setPassword(passWord);
        sender.setDefaultEncoding("Utf-8");
        Properties p = new Properties();
        p.setProperty("mail.smtp.timeout", timeOut);
        p.setProperty("mail.smtp.auth", "false");
        sender.setJavaMailProperties(p);
        return sender;
    }

    /**
     * 发送邮件
     *
     * @param to 接受人
     * @param subject 主题
     * @param html 发送内容
     * @throws MessagingException 异常
     * @throws UnsupportedEncodingException 异常
     */
    public static void sendHtmlMail(String to, String subject, String html,File file) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        // 设置utf-8或GBK编码，否则邮件会有乱码
        MimeMessageHelper messageHelper = new MimeMessageHelper(message, true, "UTF-8");
        messageHelper.setFrom(emailForm, "");
        messageHelper.setTo(to);
        // 设置主题
        messageHelper.setSubject(subject);
        messageHelper.setText(BusinessConstants.SEND_ORDER_EMIAL_WORDS, false);
        //添加附件
        messageHelper.addAttachment(file.getName(),file);
        //设置抄送
        messageHelper.setCc(cc);

        mailSender.send(message);
    }

    public static void main(String[] args) throws UnsupportedEncodingException, MessagingException {

//        sendHtmlMail("1569101820@qq.com", "的报价单", "测试");
    }
}
