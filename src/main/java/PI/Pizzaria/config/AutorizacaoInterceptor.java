package PI.Pizzaria.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.service.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AutorizacaoInterceptor implements HandlerInterceptor {
    private final SessaoService sessaoService;

    public AutorizacaoInterceptor(SessaoService sessaoService) {
        this.sessaoService = sessaoService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        Colaborador usuario = sessaoService.autenticar(request.getHeader("Authorization"));
        if (usuario == null) {
            responderErro(response, 401, "Faça login para acessar esta funcionalidade.");
            return false;
        }

        String caminho = request.getRequestURI();
        if ((caminho.startsWith("/colaboradores") || caminho.startsWith("/auth/admin/")
                || caminho.startsWith("/pedidos/admin/") || caminho.equals("/auditoria") || caminho.startsWith("/auditoria/"))
                && usuario.getPerfil() != Colaborador.Perfil.administrador) {
            responderErro(response, 403, "Acesso permitido apenas ao administrador.");
            return false;
        }

        request.setAttribute("usuarioAutenticado", usuario);
        return true;
    }

    private void responderErro(HttpServletResponse response, int codigo, String mensagem)
            throws IOException {
        response.setStatus(codigo);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"mensagem\":\"" + mensagem + "\"}");
    }
}
