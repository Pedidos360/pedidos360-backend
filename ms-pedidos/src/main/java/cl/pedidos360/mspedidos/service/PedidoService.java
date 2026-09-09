package cl.pedidos360.mspedidos.service;

import cl.pedidos360.mspedidos.entity.Pedido;
import cl.pedidos360.mspedidos.repository.PedidoRepository;
import java.util.List;
import org.springframework.stereotype.Service;

// Capa de servicio (SDD §19): lógica de negocio entre el controller
// y el repositorio.
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    public List<Pedido> listar() {
        return pedidoRepository.findAll();
    }
}
