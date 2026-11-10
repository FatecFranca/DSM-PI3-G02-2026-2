package PI.Pizzaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.ArgumentCaptor;

class EmailRecuperacaoServiceTest {
    @SuppressWarnings("unchecked")
    @Test
    void enviaCodigoParaEmailCadastrado() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender sender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        EmailRecuperacaoService service = new EmailRecuperacaoService(
                provider, "smtp.exemplo.com", "sistema@exemplo.com");

        service.enviarCodigo("funcionario@exemplo.com", "123456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        SimpleMailMessage email = captor.getValue();
        assertArrayEquals(new String[]{"funcionario@exemplo.com"}, email.getTo());
        assertEquals("sistema@exemplo.com", email.getFrom());
        assertTrue(email.getText().contains("123456"));
        assertTrue(email.getText().contains("15 minutos"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void naoSimulaEnvioQuandoSmtpNaoEstaConfigurado() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        EmailRecuperacaoService service = new EmailRecuperacaoService(provider, "", "");
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.enviarCodigo("funcionario@exemplo.com", "123456"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, erro.getStatusCode());
        verify(provider).getIfAvailable();
    }
}
