package PI.Pizzaria.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PedidoRequestTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void rejeitaCamposDaSprintQuatroEmVezDeIgnorar() {
        String json = """
                {"clienteId":"cliente-1", "itens":[], "modalidade":"ENTREGA"}
                """;
        assertThrows(Exception.class, () -> mapper.readValue(json, PedidoRequest.class));
    }

    @Test
    void rejeitaPrecoEnviadoPeloNavegador() {
        String json = """
                {"clienteId":"cliente-1", "itens":[{"produtoId":"pizza-1", "precoUnitario":0}]}
                """;
        assertThrows(Exception.class, () -> mapper.readValue(json, PedidoRequest.class));
    }
}
