package PI.Pizzaria.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.repository.ColaboradorRepository;
import PI.Pizzaria.service.ColaboradorService;

@Configuration
public class AdminInicialConfig {
    @Bean
    public CommandLineRunner administradorInicial(ColaboradorRepository repository,
            ColaboradorService colaboradorService,
            @Value("${bootstrap.admin.nome:}") String nome,
            @Value("${bootstrap.admin.matricula:}") String matricula,
            @Value("${bootstrap.admin.email:}") String email,
            @Value("${bootstrap.admin.senha:}") String senha) {
        return args -> {
            // Executa apenas em um banco novo. Nunca utiliza credenciais padrão públicas.
            if (nome != null && !nome.isBlank()
                    && matricula != null && !matricula.isBlank()
                    && email != null && !email.isBlank()
                    && senha != null && !senha.isBlank()
                    && repository.count() == 0
                    && !repository.existsByMatricula(matricula)) {
                Colaborador adm = new Colaborador();
                adm.setNome(nome);
                adm.setMatricula(matricula);
                adm.setEmail(email);
                adm.setSenha(senha);
                adm.setPerfil(Colaborador.Perfil.administrador);
                colaboradorService.cadastrar(adm);
            }
        };
    }
}
