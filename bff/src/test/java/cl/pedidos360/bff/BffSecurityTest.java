package cl.pedidos360.bff;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.pedidos360.bff.controller.PedidoBffController;
import cl.pedidos360.bff.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import cl.pedidos360.bff.service.PedidosClient;

// Pruebas de seguridad del BFF (SDD §22-23, §47): el acceso al endpoint
// depende de la validez del token y del rol.
@WebMvcTest(PedidoBffController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "security.cors.frontend-origin=http://localhost:5173",
    // issuer-uri estático para el test; el JwtDecoder real está mockeado (sin red).
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.microsoftonline.com/test-tenant/v2.0"
})
class BffSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // Evita que se construya el JwtDecoder real (que descargaría el JWKS por red).
    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private PedidosClient pedidosClient;

    @Test
    void sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/pedidos"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSinRolValido_devuelve403() throws Exception {
        mockMvc.perform(get("/pedidos")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Invitado"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void tokenConRolValido_devuelve200() throws Exception {
        when(pedidosClient.obtenerPedidos()).thenReturn("[]");

        mockMvc.perform(get("/pedidos")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Cliente"))))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }
}
