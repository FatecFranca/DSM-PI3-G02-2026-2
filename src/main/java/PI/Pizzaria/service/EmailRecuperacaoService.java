package PI.Pizzaria.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailRecuperacaoService {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String host;
    private final String remetente;

    public EmailRecuperacaoService(ObjectProvider<JavaMailSender> mailSender,
                                  @Value("${spring.mail.host:}") String host,
                                  @Value("${app.mail.from:}") String remetente) {
        this.mailSender = mailSender;
        this.host = host;
        this.remetente = remetente;
    }

    public void verificarConfiguracao() {
        // A mesma resposta é usada para todos os e-mails se não houver SMTP configurado.
        if (host == null || host.isBlank() || remetente == null || remetente.isBlank()
                || mailSender.getIfAvailable() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Recuperação por e-mail não configurada. Verifique o SMTP e o remetente.");
        }
    }

    public void enviarCodigo(String destinatario, String codigo) {
        verificarConfiguracao();
        JavaMailSender servidor = mailSender.getIfAvailable();
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject("City Pizzas - Código de recuperação de senha");
        mensagem.setText("Seu código de recuperação é: " + codigo
                + "\nEste código expira em 15 minutos. Não o compartilhe.");
        try {
            servidor.send(mensagem);
        } catch (MailException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível enviar o e-mail. Verifique a configuração SMTP e tente novamente.");
        }
    }
}
