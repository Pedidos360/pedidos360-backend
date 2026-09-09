# Pedidos360 — Backend

Backend del sistema **Pedidos360** (DSY1107 — Desarrollo Cloud Native I).
Arquitectura de microservicios Spring Boot detrás de un **BFF** que valida el
JWT emitido por Microsoft Entra ID. Ver el SDD para el diseño completo.

## Componentes

| Componente   | Puerto | Descripción                                                        |
| ------------ | -----: | ------------------------------------------------------------------ |
| `bff`        |   8080 | Backend for Frontend: valida el JWT (firma/issuer/audience/vigencia), autoriza por rol y reenvía a los microservicios |
| `ms-pedidos` |   8081 | Microservicio de pedidos (Controller → Service → Repository → Entity, JPA) |

> Para la Evaluación Parcial N°1 se entrega **un microservicio** (`ms-pedidos`).
> `ms-productos` y `ms-usuarios` quedan contemplados en el SDD para etapas
> posteriores.

## Seguridad — BFF (lo que evalúa la pauta)

El BFF es un Resource Server de Spring Security que, ante cada petición
(SDD §22):

- **Firma:** descarga el JWKS del issuer y verifica la firma del token.
- **Issuer:** el emisor debe ser el tenant de Entra ID configurado.
- **Audience:** el claim `aud` debe corresponder a esta API.
- **Vigencia:** valida `exp` y `nbf`.
- **Autorización por rol:** lee el claim `roles` (App Roles) y exige el rol
  según la ruta.
- **Códigos de error:** `401` si el token es inválido o ausente; `403` si el
  token es válido pero el rol es insuficiente.

Rutas:

| Ruta          | Requisito                     |
| ------------- | ----------------------------- |
| `GET /pedidos`| Rol `Admin`, `Operador` o `Cliente` → reenvía a `ms-pedidos` |
| `/admin/**`   | Rol `Admin`                   |

## Configuración en Microsoft Entra ID (Expose an API)

Para que el BFF pueda validar la **audiencia**, el token que envía el frontend
debe ser un **access token** dirigido a esta API:

1. App registration → **Expose an API** → *Set* Application ID URI:
   `api://<client-id>`.
2. **Add a scope**: `access_as_user` (admin/users consent).
3. En el **frontend**, definir `VITE_API_SCOPE=api://<client-id>/access_as_user`
   para que MSAL solicite ese scope.
4. En el **BFF**, `AZURE_AUDIENCE` debe coincidir (por defecto `api://<client-id>`).

## Variables de entorno

Ver `.env.example`. Para el backend:

```
# BD cloud (ms-pedidos)
DB_URL=jdbc:postgresql://<host>:5432/pedidos360
DB_USERNAME=pedidos360
DB_PASSWORD=<password>

# Validación de JWT (BFF)
AZURE_TENANT_ID=<directory-tenant-id>
AZURE_CLIENT_ID=<application-client-id>
AZURE_AUDIENCE=api://<application-client-id>
```

## Desarrollo local

Cada componente es un proyecto Maven independiente:

```bash
# Microservicio de pedidos
cd ms-pedidos && mvn spring-boot:run        # :8081 (requiere BD)

# BFF (requiere AZURE_TENANT_ID/CLIENT_ID/AUDIENCE en el entorno)
cd bff && mvn spring-boot:run               # :8080
```

Pruebas:

```bash
cd ms-pedidos && mvn test
cd bff && mvn test          # 401 sin token, 403 rol insuficiente, 200 rol válido
```

## Docker Compose

Levanta el BFF y `ms-pedidos` en una red interna (la BD es cloud y se referencia
por variables de entorno, SDD §28):

```bash
docker compose up -d --build
docker compose ps
```

Requiere un `.env` en la raíz con las variables de arriba.
