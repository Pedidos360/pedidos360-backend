package cl.pedidos360.mspedidos.config;

import cl.pedidos360.mspedidos.entity.Pedido;
import cl.pedidos360.mspedidos.repository.PedidoRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Siembra datos de demostración en la BD si está vacía, para que
// GET /pedidos devuelva información proveniente de la base de datos
// (SDD §18, §47.10). Solo inserta la primera vez (idempotente).
@Component
public class DataInitializer implements CommandLineRunner {

    private final PedidoRepository pedidoRepository;

    public DataInitializer(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    public void run(String... args) {
        if (pedidoRepository.count() == 0) {
            pedidoRepository.saveAll(List.of(
                new Pedido("Cliente Demo", LocalDate.of(2026, 9, 9), "PENDIENTE"),
                new Pedido("Distribuidora Sur", LocalDate.of(2026, 9, 10), "EN_PROCESO"),
                new Pedido("Comercial Norte", LocalDate.of(2026, 9, 11), "ENTREGADO")
            ));
        }
    }
}
