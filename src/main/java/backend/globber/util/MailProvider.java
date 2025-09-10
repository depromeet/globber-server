package backend.globber.util;

import backend.globber.exception.spec.MailException;
import backend.globber.repository.MailCertRedisRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailProvider {
    // 메일인증절차 -> redis에 (email : UUID) 저장, TTL 10분
    // 확인 되면 (email : "ACK")로 변경
    // 회원가입시에 Redis 확인.
    private final JavaMailSender mailSender;
    private final MailCertRedisRepository mailCertRedisRepository;

    @Value("${spring.mail.username}")
    private String adminName;
    public void sendMail(String email){
        // 메일 정보 구성.
        try {
            String uuid = UUID.randomUUID().toString();
            String from = adminName;
            String to = email;
            String subject = "GLOBBER 회원가입 인증 메일입니다.";
            String content = createHtmlContent(uuid);

            // Redis에 저장
            mailCertRedisRepository.save(email, uuid);

            // 메일 전송
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MailException() {
            };
        }
    }

    public boolean checkMail(String email, String uuid) {
        // 인증번호 확인후 레디스값을 ACK로 변경
        if (mailCertRedisRepository.findByEmail(email).isPresent()) {
            if (mailCertRedisRepository.findByEmail(email).get().equals(uuid)) {
                mailCertRedisRepository.update(email, "ACK");
                return true;
            }
            else {
                return false;
            }
        }
        return false;
    }

    public boolean checkAck(String email) {
        // 마지막 ACK 확인 후 삭제
        if (mailCertRedisRepository.findByEmail(email).isPresent()) {
            if (mailCertRedisRepository.findByEmail(email).get().equals("ACK")) {
                mailCertRedisRepository.delete(email);
                return true;
            }
            else {
                return false;
            }
        }
        return false;
    }
    private String createHtmlContent(String uuid) {
        return "<!DOCTYPE html>" +
            "<html lang='ko'>" +
            "<head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<title>인증 이메일</title>" +
            "<style>" +
            "body {font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5;}" +
            ".container {width: 100%; max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);}" +
            ".header {background-color: #007bff; color: #ffffff; padding: 20px; text-align: center;}" +
            ".header h1 {margin: 0; font-size: 24px;}" +
            ".body {padding: 20px;}" +
            ".body p {font-size: 16px; line-height: 1.5;}" +
            ".code {font-size: 18px; font-weight: bold; color: #007bff; padding: 10px; border: 1px solid #007bff; border-radius: 4px; display: inline-block; margin: 10px 0;}" +
            ".footer {background-color: #f1f1f1; color: #666666; text-align: center; padding: 10px; font-size: 14px;}" +
            ".footer p {margin: 0;}" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class='container'>" +
            "<div class='header'>" +
            "<h1>GLOBBER</h1>" +
            "</div>" +
            "<div class='body'>" +
            "<p>안녕하세요!</p>" +
            "<p>이메일 인증을 위해 아래 인증번호를 입력해 주세요:</p>" +
            "<div class='code'>" +
            "인증번호: " + uuid +
            "</div>" +
            "<p>위 인증번호를 입력하셔야만 인증이 완료됩니다. 인증번호는 10분간 유효합니다.</p>" +
            "<p>감사합니다!</p>" +
            "</div>" +
            "<div class='footer'>" +
            "<p>&copy; 2025 GLOBBER. All rights reserved.</p>" +
            "</div>" +
            "</div>" +
            "</body>" +
            "</html>";
    }

}
