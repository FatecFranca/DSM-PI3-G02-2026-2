package PI.Pizzaria.config;

import java.util.Comparator;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import PI.Pizzaria.model.Pedido;
import PI.Pizzaria.repository.PedidoRepository;
import PI.Pizzaria.service.NumeracaoPedidoService;

/** Numera pedidos antigos na ordem de criação, sem mudar seus IDs ou valores. */
@Component
@ConditionalOnProperty(name = "app.pedido.migracao-habilitada", havingValue = "true", matchIfMissing = true)
public class MigracaoNumeracaoPedidos implements ApplicationRunner {
    private final PedidoRepository pedidoRepository;
    private final NumeracaoPedidoService numeracaoPedidoService;

    public MigracaoNumeracaoPedidos(PedidoRepository pedidoRepository,
                                   NumeracaoPedidoService numeracaoPedidoService) {
        this.pedidoRepository = pedidoRepository;
        this.numeracaoPedidoService = numeracaoPedidoService;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        List<Pedido> pedidos = pedidoRepository.findAll();
        long maior = pedidos.stream().map(Pedido::getNumero)
                .filter(n -> n != null && n > 0).mapToLong(Long::longValue).max().orElse(0L);
        numeracaoPedidoService.garantirMinimo(maior);

        pedidos.stream().filter(p -> p.getNumero() == null || p.getNumero() <= 0)
                .sorted(Comparator.comparing(Pedido::getCriadoEm,
                        Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(Pedido::getId, Comparator.nullsFirst(String::compareTo)))
                .forEach(pedido -> {
                    pedido.setNumero(numeracaoPedidoService.proximoNumero());
                    pedidoRepository.save(pedido);
                });
    }
}
