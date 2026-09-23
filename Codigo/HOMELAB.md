# Guia de Deploy no Homelab (TV Box TX9 - ARM64 / 2GB RAM)

Este guia orienta o deploy completo da aplicação **DaFabi Frangos** em um ambiente homelab de baixo custo com hardware modesto (ex: TV Box TX9 com 2GB de RAM e 16GB de armazenamento) e integração com **Cloudflare Tunnels**.

---

## 🏗️ Arquitetura da Solução

```mermaid
graph TD
    User([Usuário na Internet]) -->|HTTPS: dafabi.seudominio.com| CF[Cloudflare Edge / Tunnel]
    CF -->|HTTP localhost:3000| Nginx[Frontend: Nginx Alpine<br/>Porta 3000]
    
    subgraph TV Box TX9 (Homelab)
        Nginx -->|Arquivos estáticos /| Angular[Angular SPA]
        Nginx -->|Proxy /api/| Spring[Backend: Spring Boot 3<br/>Porta 8080<br/>Java 21 -Xmx256m]
        Spring -->|JDBC Porta 5432| Postgres[(PostgreSQL 16 Alpine<br/>shared_buffers=64MB)]
    end
```

### Otimizações Chave para Baixo Consumo de RAM:
- **Nginx Alpine no Frontend:** Consome apenas ~8MB a 15MB de RAM (contra ~80MB se usado Node.js).
- **Proxy Reverso Unificado:** O Nginx atende na porta `3000` e encaminha chamadas `/api/` diretamente ao backend, eliminando a necessidade de expor duas portas e evitando problemas de CORS.
- **JVM Ajustada:** Backend configurado com `-Xms128m -Xmx256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m` (consumo total ~250MB).
- **Postgres Enxuto:** Buffers configurados para baixo uso de memória (`shared_buffers=64MB`, limite de 160MB).
- **Consumo Total do Stack:** Menos de **500MB de RAM**, deixando mais de 1.5GB livres para o SO e o Cloudflare Tunnel.

---

## ⚙️ 1. Preparação da TV Box (Armbian / Linux)

Antes de rodar qualquer container em uma máquina com 2GB de RAM, certifique-se de que há **Swap** configurado:

```bash
# Verifique a memória e se o swap já existe:
free -h

# Se o swap for 0B, crie um arquivo de 1.5GB:
sudo fallocate -l 1.5G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

---

## 🛠️ 2. CI/CD e Compilação das Imagens

> ⚠️ **Atenção:** Nunca tente rodar `docker build` ou `mvn package` direto na TV Box. O processador fraco e a pouca memória vão travar o sistema.

### Opção A: GitHub Actions (Totalmente Automatizado - Recomendado)
Toda vez que você atualizar a branch `homelab` (por exemplo, sincronizando com a `main`) e der `push`, o GitHub Actions:
1. Detecta as alterações na pasta `Codigo/`.
2. Compila as imagens Docker multi-arquitetura (`linux/arm64` e `linux/amd64`).
3. Publica no **GitHub Container Registry (GHCR)**:
   - `ghcr.io/icei-puc-minas-pmges-ti/dafabi-backend:homelab`
   - `ghcr.io/icei-puc-minas-pmges-ti/dafabi-frontend:homelab`
4. O **Watchtower** na TV Box detecta a nova imagem automaticamente a cada 2 minutos e reinicia os containers sem você precisar rodar nenhum comando manual!

### Opção B: Build Local via Script PowerShell (Fallback)
Se preferir compilar na sua máquina Windows local:
No PowerShell dentro de `Codigo`:
```powershell
# Envia para Docker Hub/Registry próprio:
.\build-homelab.ps1 -Registry "seu_usuario"

# Ou gera arquivos .tar locais na pasta dist-homelab:
.\build-homelab.ps1
```
Se você não quiser criar conta ou subir imagens para a nuvem:

```powershell
# Compila para ARM64 e exporta para a pasta ./dist-homelab/
.\build-homelab.ps1
```
Esse comando gerará os arquivos `backend-arm64.tar` e `frontend-arm64.tar`. Basta copiá-los para a TV Box usando SCP, WinSCP ou pendrive:
```bash
scp -r ./dist-homelab user@ip-da-tvbox:/home/user/
```

---

## 🚀 3. Subindo a Aplicação na TV Box

1. Copie o arquivo `docker-compose.homelab.yml` e o `.env.homelab.example` para uma pasta na TV Box (ex: `~/dafabi`):
   ```bash
   mkdir -p ~/dafabi && cd ~/dafabi
   cp .env.homelab.example .env
   ```

2. Ajuste o `.env` com suas senhas e tags das imagens:
   ```bash
   nano .env
   ```

3. Se você usou a **Opção B (arquivos .tar)**, carregue as imagens no Docker da TV Box:
   ```bash
   docker load -i backend-arm64.tar
   docker load -i frontend-arm64.tar
   ```

4. Suba os containers em segundo plano:
   ```bash
   docker compose -f docker-compose.homelab.yml up -d
   ```

5. Verifique o status e os logs:
   ```bash
   docker compose -f docker-compose.homelab.yml ps
   docker compose -f docker-compose.homelab.yml logs -f
   ```

---

## ☁️ 4. Configurar no Cloudflare Tunnel

Como você já possui o Cloudflare Tunnel (`cloudflared`) rodando na TV Box, basta mapear o serviço HTTP:

### Pelo Dashboard do Cloudflare Zero Trust:
1. Acesse **Networks** > **Tunnels** > Selecione seu túnel.
2. Na aba **Public Hostnames**, adicione uma nova rota:
   - **Subdomain:** `dafabi` (ou o nome que desejar)
   - **Domain:** `seudominio.com`
   - **Service:** `HTTP`
   - **URL:** `localhost:3000` (ou o IP local da TV Box, ex: `192.168.1.100:3000`)
3. Salve.

### Por arquivo local (`config.yml` do cloudflared):
Se seu túnel for gerenciado por arquivo de configuração:
```yaml
ingress:
  - hostname: dafabi.seudominio.com
    service: http://localhost:3000
  - service: http_status:404
```

Pronto! Acesse `https://dafabi.seudominio.com` no seu navegador. O site carregará e todas as chamadas de API funcionarão de forma transparente.

---

## 📊 5. Monitoramento e Dicas de Armazenamento (16GB)

Como a TV Box tem apenas 16GB de armazenamento flash (eMMC):

1. **Monitore o uso de memória em tempo real:**
   ```bash
   docker stats
   ```
   Você verá que o Postgres consumirá ~70MB, o Backend ~220MB e o Frontend ~10MB.

2. **Limpeza periódica de imagens antigas:**
   Ao atualizar a aplicação, imagens antigas ocupam espaço precioso. Remova resíduos com:
   ```bash
   docker system prune -a --volumes -f
   ```
   *(Atenção: o volume `postgres_data` é preservado pois está nomeado no docker compose).*

3. **Verificar espaço em disco:**
   ```bash
   df -h
   ```
