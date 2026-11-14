package PI.Pizzaria.service;

import java.util.List;

import org.springframework.stereotype.Service;

import PI.Pizzaria.model.Auditoria;
import PI.Pizzaria.model.Colaborador;
import PI.Pizzaria.model.Pedido;
import PI.Pizzaria.repository.AuditoriaRepository;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(
            AuditoriaRepository auditoriaRepository) {

        this.auditoriaRepository = auditoriaRepository;
    }

    public Auditoria registrar(
            Colaborador usuario,
            String acao,
            String tipoRegistro,
            String registroId) {

        if (usuario == null
                || usuario.getId() == null
                || usuario.getId().isBlank()) {

            throw new IllegalArgumentException(
                    "Usuário responsável pela auditoria é obrigatório.");
        }

        if (acao == null || acao.isBlank()) {
            throw new IllegalArgumentException(
                    "A ação da auditoria é obrigatória.");
        }

        if (tipoRegistro == null
                || tipoRegistro.isBlank()) {

            throw new IllegalArgumentException(
                    "O tipo de registro afetado é obrigatório.");
        }

        if (registroId == null
                || registroId.isBlank()) {

            throw new IllegalArgumentException(
                    "O registro afetado é obrigatório.");
        }

        Auditoria auditoria =
                new Auditoria(
                        usuario.getId(),
                        usuario.getMatricula(),
                        acao,
                        tipoRegistro,
                        registroId);

        return auditoriaRepository.save(auditoria);
    }

    public Auditoria registrar(Colaborador usuario, String acao, String tipoRegistro,
                              String registroId, String detalhes) {
        Auditoria registro = registrar(usuario, acao, tipoRegistro, registroId);
        registro.setDetalhes(detalhes);
        return auditoriaRepository.save(registro);
    }

    public Auditoria registrarPedido(Colaborador usuario, String acao, Pedido pedido) {
        return registrarPedido(usuario, acao, pedido, null);
    }

    public Auditoria registrarPedido(Colaborador usuario, String acao, Pedido pedido, String detalhes) {
        if (pedido == null || pedido.getId() == null) {
            throw new IllegalArgumentException("Pedido não identificado para auditoria.");
        }
        Auditoria auditoria = new Auditoria(usuario.getId(), usuario.getMatricula(),
                acao, "PEDIDO", pedido.getId());
        auditoria.setNumeroPedido(pedido.getNumero());
        auditoria.setDetalhes(detalhes);
        return auditoriaRepository.save(auditoria);
    }

    public List<Auditoria> listarMaisRecentes() {
        return auditoriaRepository
                .findAllByOrderByDataHoraDesc();
    }
}