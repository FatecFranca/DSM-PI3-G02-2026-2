package PI.Pizzaria.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "auditorias")
public class Auditoria {

    @Id
    private String id;

    @Indexed
    private String usuarioId;

    private String usuarioMatricula;

    private String acao;

    @Indexed
    private String tipoRegistro;

    @Indexed
    private String registroId;

    @Indexed
    private Instant dataHora;

    public Auditoria() {
    }

    public Auditoria(
            String usuarioId,
            String usuarioMatricula,
            String acao,
            String tipoRegistro,
            String registroId) {

        this.usuarioId = usuarioId;
        this.usuarioMatricula = usuarioMatricula;
        this.acao = acao;
        this.tipoRegistro = tipoRegistro;
        this.registroId = registroId;
        this.dataHora = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public String getUsuarioMatricula() {
        return usuarioMatricula;
    }

    public String getAcao() {
        return acao;
    }

    public String getTipoRegistro() {
        return tipoRegistro;
    }

    public String getRegistroId() {
        return registroId;
    }

    public Instant getDataHora() {
        return dataHora;
    }
}