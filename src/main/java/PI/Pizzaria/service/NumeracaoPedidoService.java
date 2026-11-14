package PI.Pizzaria.service;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import PI.Pizzaria.model.SequenciaPedido;

@Service
public class NumeracaoPedidoService {
    private final MongoTemplate mongoTemplate;

    public NumeracaoPedidoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /** Incremento atômico: duas estações não recebem o mesmo número. */
    public long proximoNumero() {
        SequenciaPedido sequencia = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is("pedido")),
                new Update().inc("ultimoNumero", 1L),
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                SequenciaPedido.class);
        if (sequencia == null) throw new IllegalStateException("Não foi possível obter o número do pedido.");
        return sequencia.getUltimoNumero();
    }

    /** Não reinicia o contador após a migração ou reinício do servidor. */
    public void garantirMinimo(long minimo) {
        mongoTemplate.upsert(Query.query(Criteria.where("_id").is("pedido")),
                new Update().max("ultimoNumero", minimo), SequenciaPedido.class);
    }
}
