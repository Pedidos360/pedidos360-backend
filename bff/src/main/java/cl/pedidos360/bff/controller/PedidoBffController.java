package cl.pedidos360.bff.controller;

import cl.pedidos360.bff.service.PedidosClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Endpoint del BFF que, tras validar el JWT y el rol (SecurityConfig),
// reenvía la petición a ms-pedidos y entrega la respuesta (SDD §21, §25).
@RestController
@RequestMapping("/pedidos")
public class PedidoBffController {

    private final PedidosClient pedidosClient;

    public PedidoBffController(PedidosClient pedidosClient) {
        this.pedidosClient = pedidosClient;
    }

    @GetMapping
    public ResponseEntity<String> getPedidos() {
        String pedidos = pedidosClient.obtenerPedidos();
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(pedidos);
    }
}
