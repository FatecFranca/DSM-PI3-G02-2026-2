package PI.Pizzaria.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import PI.Pizzaria.model.Auditoria;
import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.repository.AuditoriaRepository;

class AuditoriaServiceTest {

    private AuditoriaRepository auditoriaRepository;
    private AuditoriaService auditoriaService;
    private Colaborador usuario;

    @BeforeEach
    void iniciar() {

        auditoriaRepository =
                mock(AuditoriaRepository.class);

        auditoriaService =
                new AuditoriaService(
                        auditoriaRepository);

        usuario = new Colaborador();
        usuario.setId("colaborador-1");
        usuario.setMatricula("1001");

        when(auditoriaRepository.save(
                any(Auditoria.class)))
                .thenAnswer(invocacao ->
                        invocacao.getArgument(0));
    }

    @Test
    void registraAuditoriaComUsuarioAcaoRegistroEDataHora() {

        Auditoria auditoria =
                auditoriaService.registrar(
                        usuario,
                        "ALTERAR",
                        "PEDIDO",
                        "pedido-1");

        assertEquals(
                "colaborador-1",
                auditoria.getUsuarioId());

        assertEquals(
                "1001",
                auditoria.getUsuarioMatricula());

        assertEquals(
                "ALTERAR",
                auditoria.getAcao());

        assertEquals(
                "PEDIDO",
                auditoria.getTipoRegistro());

        assertEquals(
                "pedido-1",
                auditoria.getRegistroId());

        assertNotNull(
                auditoria.getDataHora());

        verify(auditoriaRepository)
                .save(any(Auditoria.class));
    }

    @Test
    void rejeitaAuditoriaSemUsuarioResponsavel() {

        assertThrows(
                IllegalArgumentException.class,
                () -> auditoriaService.registrar(
                        null,
                        "ALTERAR",
                        "PEDIDO",
                        "pedido-1"));

        Colaborador semId =
                new Colaborador();

        semId.setMatricula("1001");

        assertThrows(
                IllegalArgumentException.class,
                () -> auditoriaService.registrar(
                        semId,
                        "ALTERAR",
                        "PEDIDO",
                        "pedido-1"));

        verify(
                auditoriaRepository,
                never())
                .save(any());
    }

    @Test
    void rejeitaAuditoriaSemAcao() {

        assertThrows(
                IllegalArgumentException.class,
                () -> auditoriaService.registrar(
                        usuario,
                        "",
                        "PEDIDO",
                        "pedido-1"));

        verify(
                auditoriaRepository,
                never())
                .save(any());
    }

    @Test
    void rejeitaAuditoriaSemTipoDeRegistro() {

        assertThrows(
                IllegalArgumentException.class,
                () -> auditoriaService.registrar(
                        usuario,
                        "ALTERAR",
                        "",
                        "pedido-1"));

        verify(
                auditoriaRepository,
                never())
                .save(any());
    }

    @Test
    void rejeitaAuditoriaSemRegistroAfetado() {

        assertThrows(
                IllegalArgumentException.class,
                () -> auditoriaService.registrar(
                        usuario,
                        "ALTERAR",
                        "PEDIDO",
                        ""));

        verify(
                auditoriaRepository,
                never())
                .save(any());
    }

    @Test
    void listaAuditoriasMaisRecentes() {

        Auditoria primeira =
                new Auditoria(
                        "colaborador-1",
                        "1001",
                        "ALTERAR",
                        "PEDIDO",
                        "pedido-1");

        Auditoria segunda =
                new Auditoria(
                        "colaborador-2",
                        "2002",
                        "EXCLUIR",
                        "PEDIDO",
                        "pedido-2");

        when(
                auditoriaRepository
                        .findAllByOrderByDataHoraDesc())
                .thenReturn(
                        List.of(
                                segunda,
                                primeira));

        List<Auditoria> resultado =
                auditoriaService
                        .listarMaisRecentes();

        assertEquals(
                2,
                resultado.size());

        assertEquals(
                "EXCLUIR",
                resultado.get(0).getAcao());

        assertEquals(
                "ALTERAR",
                resultado.get(1).getAcao());

        verify(auditoriaRepository)
                .findAllByOrderByDataHoraDesc();
    }

    @Test
    void repositoryNaoExpoeOperacoesDeExclusao() {

        boolean possuiDelete =
                List.of(
                        AuditoriaRepository.class
                                .getMethods())
                        .stream()
                        .anyMatch(
                                metodo ->
                                        metodo.getName()
                                                .toLowerCase()
                                                .startsWith("delete"));

        assertFalse(possuiDelete);
    }
}