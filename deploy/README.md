# Despliegue en AWS (Parte 3) — Pedidos360

Despliega los 3 componentes en **una sola instancia EC2** con Docker Compose,
con **API Gateway (HTTP API)** delante del BFF y **HTTPS** en el frontend
(vía Caddy + un hostname `sslip.io`, sin comprar dominio). La base de datos es
**H2** dentro del contenedor de `ms-pedidos` (sin RDS).

```
Navegador
   │  https
   ▼
Frontend (Caddy HTTPS)  ── https://<IP>.sslip.io        ┐
   │  fetch  https://<api>.execute-api.../pedidos       │  todo en la
   ▼                                                     │  misma EC2
AWS API Gateway (HTTP API, HTTPS)                        │
   │  http://<IP>:8090/pedidos                           │
   ▼                                                     │
BFF :8090  ──►  ms-pedidos :8081 (H2)                    ┘
```

> Nota de seguridad: MSAL exige Redirect URIs `https://` (excepto `localhost`).
> Por eso el frontend se sirve por HTTPS con Caddy.

---

## Fase 0 — Prerrequisitos
- Cuenta AWS con acceso a la consola.
- Un **key pair** (para SSH) en la región que uses (p.ej. `us-east-1`).
- Tener a mano: `AZURE_TENANT_ID`, `AZURE_CLIENT_ID`.

## Fase 1 — Lanzar la instancia EC2
1. EC2 → **Launch instance**.
2. **AMI**: Amazon Linux 2023. **Tipo**: `t3.small` (2 GB; recomendado para compilar). *(Con `t2.micro`/`t3.micro` de free tier hay que agregar swap, ver Troubleshooting.)*
3. **Key pair**: selecciona el tuyo.
4. **Security group** — reglas de entrada:
   | Tipo | Puerto | Origen | Para qué |
   |------|-------:|--------|----------|
   | SSH | 22 | Mi IP | administración |
   | HTTP | 80 | 0.0.0.0/0 | Caddy (reto Let's Encrypt) |
   | HTTPS | 443 | 0.0.0.0/0 | frontend |
   | TCP personalizado | 8090 | 0.0.0.0/0 | BFF (lo consume el API Gateway) |
5. Almacenamiento: 16 GB. **Launch**.
6. Anota la **IP pública** (IPv4). Con ella arma `PUBLIC_HOST` reemplazando los puntos por guiones:
   `13.58.0.0` → **`13-58-0-0.sslip.io`**

## Fase 2 — Instalar Docker en la EC2
Conéctate por SSH y ejecuta:
```bash
ssh -i tu-llave.pem ec2-user@<IP-PUBLICA>

sudo dnf update -y
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
# Plugin de docker compose
sudo mkdir -p /usr/libexec/docker/cli-plugins
sudo curl -sSL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/libexec/docker/cli-plugins/docker-compose
sudo chmod +x /usr/libexec/docker/cli-plugins/docker-compose
exit    # salir y volver a entrar para que tome el grupo docker
```
Reingresa por SSH y valida: `docker version` y `docker compose version`.

## Fase 3 — Clonar los repos (uno al lado del otro)
```bash
cd ~
git clone https://github.com/Pedidos360/pedidos360-backend.git
git clone https://github.com/Pedidos360/pedidos360-frontend.git
```

## Fase 4 — Arrancar primero BFF + microservicio
Necesitamos el BFF accesible para conectar el API Gateway.
```bash
cd ~/pedidos360-backend/deploy
cp .env.aws.example .env
# Edita .env y completa por ahora SOLO estos (el resto en Fase 6):
#   AZURE_TENANT_ID, AZURE_CLIENT_ID, AZURE_AUDIENCE
#   FRONTEND_ORIGIN=https://<IP-con-guiones>.sslip.io
nano .env

docker compose -f docker-compose.aws.yml up -d --build ms-pedidos bff
```
Prueba (debe dar **401**, señal de que el BFF valida):
```bash
curl -i http://localhost:8090/pedidos
```

## Fase 5 — Crear el API Gateway (HTTP API)
1. API Gateway → **Create API** → **HTTP API** → Build.
2. **Integrations**: no agregues aquí; pulsa Next hasta poder crear rutas.
3. Crea la API (nombre `pedidos360`). Luego:
   - **Routes** → Create → método **ANY**, ruta **/{proxy+}**.
   - **Integrations** → adjunta a esa ruta una integración **HTTP URI**:
     `http://<IP-PUBLICA>:8090/{proxy}`  (tipo HTTP proxy).
4. **Stages**: usa el stage `$default` con *auto-deploy*.
5. Copia la **Invoke URL** (algo como `https://abc123.execute-api.us-east-1.amazonaws.com`). Ese es tu `VITE_API_URL`.

> CORS: **no** lo configures en el Gateway. El BFF ya responde CORS para
> `FRONTEND_ORIGIN`; configurarlo también en el Gateway duplicaría cabeceras.

Prueba el Gateway (debe dar **401** sin token):
```bash
curl -i https://<api-id>.execute-api.<region>.amazonaws.com/pedidos
```

## Fase 6 — Registrar el HTTPS del frontend en Entra ID
En Azure → App `Pedidos360 Frontend` → **Authentication** → plataforma **SPA**,
agrega el Redirect URI:
```
https://<IP-con-guiones>.sslip.io
```
(Deja también `http://localhost:5173` para desarrollo.)

## Fase 7 — Completar el .env y levantar todo
```bash
cd ~/pedidos360-backend/deploy
nano .env
# Completa ahora:
#   PUBLIC_HOST=<IP-con-guiones>.sslip.io
#   FRONTEND_ORIGIN=https://<IP-con-guiones>.sslip.io
#   VITE_REDIRECT_URI=https://<IP-con-guiones>.sslip.io
#   VITE_API_URL=https://<api-id>.execute-api.<region>.amazonaws.com
#   VITE_CLIENT_ID / VITE_TENANT_ID / VITE_API_SCOPE

docker compose -f docker-compose.aws.yml up -d --build
docker compose -f docker-compose.aws.yml ps
```
Caddy tardará ~30 s en emitir el certificado la primera vez.

## Fase 8 — Probar de punta a punta
1. Abre `https://<IP-con-guiones>.sslip.io`
2. Inicia sesión (Admin) → entra a **Pedidos** → debe mostrar la tabla (**200**).
3. DevTools → Network: la llamada va al **API Gateway** (`execute-api`) con
   `Authorization: Bearer …`.
4. Seguridad: `curl -i https://<api>.execute-api.../pedidos` sin token → **401**.

---

## Troubleshooting
- **Compilación se queda sin memoria** (t2.micro/t3.micro, 1 GB): agrega swap
  antes de `docker compose build`:
  ```bash
  sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
  sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
  ```
- **Caddy no obtiene certificado**: revisa que el SG tenga 80 y 443 abiertos y
  que `PUBLIC_HOST` use la IP correcta con guiones. Log: `docker compose -f docker-compose.aws.yml logs caddy`.
- **Login: redirect_uri mismatch**: el Redirect URI en Entra debe ser
  exactamente `https://<IP-con-guiones>.sslip.io` (sin `/` final).
- **CORS en el navegador**: `FRONTEND_ORIGIN` del BFF debe ser idéntico al
  origen del frontend (mismo esquema y host). Reconstruye tras cambiarlo.
- **La IP cambió al reiniciar la EC2**: si no usas Elastic IP, la IP pública
  cambia; habría que actualizar `PUBLIC_HOST`, el Redirect URI y reconstruir.
  Asigna una **Elastic IP** para fijarla.
