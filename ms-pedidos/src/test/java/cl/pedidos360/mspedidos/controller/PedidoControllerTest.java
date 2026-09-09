package cl.pedidos360.mspedidos.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.pedidos360.mspedidos.entity.Pedido;
import cl.pedidos360.mspedidos.service.PedidoService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

// Prueba del endpoint GET /pedidos con MockMvc y el service mockeado
// (no requiere BD). Verifica 200 OK y el cuerpo JSON (SDD §18, §47.9).
@WebMvcTest(PedidoController.class)
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PedidoService pedidoService;

    @Test
    void getPedidosDevuelve200YListado() throws Exception {
        when(pedidoService.listar()).thenReturn(List.of(
            new Pedido("Cliente Demo", LocalDate.of(2026, 9, 9), "PENDIENTE")
        ));

        mockMvc.perform(get("/pedidos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].cliente").value("Cliente Demo"))
            .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
    }
}
