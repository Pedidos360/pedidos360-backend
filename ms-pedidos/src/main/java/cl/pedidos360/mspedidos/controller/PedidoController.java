package cl.pedidos360.mspedidos.controller;

import cl.pedidos360.mspedidos.entity.Pedido;
import cl.pedidos360.mspedidos.service.PedidoService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controlador REST (SDD §18, §19). Expone el endpoint mínimo GET /pedidos.
// La validación del JWT y la autorización por rol las realiza el BFF
// (SDD §21-23); este microservicio es interno y no se expone directamente
// a Internet (SDD §29, §31).
@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public List<Pedido> listar() {
        return pedidoService.listar();
    }
}
