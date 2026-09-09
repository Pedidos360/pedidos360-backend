package cl.pedidos360.bff.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

// Cliente hacia ms-pedidos (SDD §21 paso 11, §29). El BFF reenvía la
// petición al microservicio por la red interna una vez validado el token.
@Service
public class PedidosClient {

    private final RestClient restClient;

    public PedidosClient(@Value("${ms.pedidos.url}") String msPedidosUrl) {
        this.restClient = RestClient.builder()
            .baseUrl(msPedidosUrl)
            .build();
    }

    public String obtenerPedidos() {
        return restClient.get()
            .uri("/pedidos")
            .retrieve()
            .body(String.class);
    }
}
