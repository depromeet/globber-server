package backend.globber.config;


import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {
    @Value("${spring.mail.username}")
    private String adminName;
    @Value("${spring.mail.password}")
    private String adminPW;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com"); // smtp 서버 주소
        mailSender.setPort(587); // smtp 포트
        mailSender.setUsername(adminName); // 계정
        mailSender.setPassword(adminPW); // 비밀번호

        Properties properties = new Properties();
        properties.put("mail.transport.protocol", "smtp"); // 프로토콜 설정
        properties.put("mail.smtp.auth", "true"); // SMTP 서버 인증 설정
        properties.put("mail.smtp.starttls.enable", "true"); // TLS 설정
        properties.put("mail.debug", "true"); // 디버깅 설정
        properties.put("mail.smtp.ssl.trust", "smtp.gmail.com"); // SSL 설정
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2"); // SSL 프로토콜 설정

        mailSender.setJavaMailProperties(properties);
        return mailSender;
    }
}
