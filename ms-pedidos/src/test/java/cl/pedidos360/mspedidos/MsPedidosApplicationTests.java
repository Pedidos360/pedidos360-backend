package cl.pedidos360.mspedidos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Prueba básica: verifica que el contexto de Spring levanta correctamente
// (compila y las capas se cablean) usando la BD H2 de pruebas (SDD §39, §49).
@SpringBootTest
@ActiveProfiles("test")
class MsPedidosApplicationTests {

    @Test
    void contextLoads() {
    }
}
