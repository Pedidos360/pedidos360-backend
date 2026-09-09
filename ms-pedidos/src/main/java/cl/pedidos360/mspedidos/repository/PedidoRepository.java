package cl.pedidos360.mspedidos.repository;

import cl.pedidos360.mspedidos.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Repositorio Spring Data JPA (SDD §19, §20).
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
