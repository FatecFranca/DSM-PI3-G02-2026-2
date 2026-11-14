package PI.Pizzaria.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import PI.Pizzaria.model.Auditoria;

public interface AuditoriaRepository
        extends Repository<Auditoria, String> {

    Auditoria save(Auditoria auditoria);

    List<Auditoria> findAllByOrderByDataHoraDesc();
}