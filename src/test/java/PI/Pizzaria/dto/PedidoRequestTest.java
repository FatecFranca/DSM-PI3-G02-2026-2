package PI.Pizzaria.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class PedidoRequestTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void aceitaCamposDePersistenciaDaSprintQuatro() throws Exception {
        String json = """
                {
                  "clienteId":"cliente-1",
                  "itens":[],
                  "modalidade":"ENTREGA",
                  "enderecoEntrega":{
                    "cep":"14400000",
                    "rua":"Rua Exemplo",
                    "numero":"100",
                    "bairro":"Centro",
                    "complemento":null,
                    "cidade":"Franca"
                  },
                  "formaPagamento":"PIX"
                }
                """;

        PedidoRequest pedido = mapper.readValue(json, PedidoRequest.class);

        assertEquals("cliente-1", pedido.getClienteId());
        assertEquals("ENTREGA", pedido.getModalidade());
        assertEquals("PIX", pedido.getFormaPagamento());
        assertNotNull(pedido.getEnderecoEntrega());
        assertEquals("14400000", pedido.getEnderecoEntrega().getCep());
    }

    @Test
    void rejeitaTaxaETotalEnviadosPeloNavegador() {
        String json = """
                {
                  "clienteId":"cliente-1",
                  "itens":[],
                  "taxaEntrega":10,
                  "total":50
                }
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
