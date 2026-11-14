package PI.Pizzaria.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.service.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class AutorizacaoInterceptorTest {
    @Test
    void proibeCadastroDeColaboradorParaFuncionario() throws Exception {
        SessaoService sessao = mock(SessaoService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/colaboradores");
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        Colaborador funcionario = new Colaborador();
        funcionario.setPerfil(Colaborador.Perfil.funcionario);
        when(sessao.autenticar("Bearer token")).thenReturn(funcionario);

        boolean resultado = new AutorizacaoInterceptor(sessao).preHandle(request, response, new Object());
        assertFalse(resultado);
        verify(response).setStatus(403);
    }

    @Test
    void proibeAuditoriaEAlteracaoAdministrativaParaFuncionario() throws Exception {
        SessaoService sessao = mock(SessaoService.class);
        Colaborador funcionario = new Colaborador();
        funcionario.setPerfil(Colaborador.Perfil.funcionario);
        when(sessao.autenticar("Bearer token")).thenReturn(funcionario);

        for (String caminho : new String[]{"/auditoria", "/pedidos/admin/123"}) {
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            when(request.getRequestURI()).thenReturn(caminho);
            when(request.getHeader("Authorization")).thenReturn("Bearer token");
            when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
            assertFalse(new AutorizacaoInterceptor(sessao).preHandle(request, response, new Object()));
            verify(response).setStatus(403);
        }
    }

    @Test
    void recusaRequisicaoSemSessao() throws Exception {
        SessaoService sessao = mock(SessaoService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/clientes");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        boolean resultado = new AutorizacaoInterceptor(sessao).preHandle(request, response, new Object());
        assertFalse(resultado);
        verify(response).setStatus(401);
    }

    @Test
    void liberaClienteParaFuncionarioAutenticado() throws Exception {
        SessaoService sessao = mock(SessaoService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/clientes");
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        Colaborador funcionario = new Colaborador();
        funcionario.setPerfil(Colaborador.Perfil.funcionario);
        when(sessao.autenticar("Bearer token")).thenReturn(funcionario);
        assertTrue(new AutorizacaoInterceptor(sessao).preHandle(request, response, new Object()));
    }
}
