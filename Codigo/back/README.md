# DaFabi Frangos — Backend

API Spring Boot da primeira versão do sistema. Nesta etapa, disponibiliza os domínios de **Produtos** e **Estoque**, persistidos em PostgreSQL e versionados com Flyway.

## Pré-requisitos

- Java 21 ou superior;
- Docker Desktop (recomendado para o PostgreSQL);
- Maven Wrapper incluído no projeto.

## Banco local

Na pasta `Codigo/back`, inicie o PostgreSQL:

```bash
docker compose -f src/main/resources/db/docker-compose.yml up -d
```

As migrations do Flyway criam o esquema e inserem as categorias iniciais quando a aplicação sobe.

## Executar a API

Ainda em `Codigo/back`:

```bash
./mvnw spring-boot:run
```

No PowerShell, use:

```powershell
.\mvnw.cmd spring-boot:run
```

A API fica em `http://localhost:8080/api/v1`.

## Endpoints disponíveis

- `GET /api/v1/categories`
- `GET /api/v1/products?query=&categoryId=&status=&includeInactive=`
- `GET /api/v1/products/{id}`
- `GET /api/v1/products/by-barcode/{barcode}`
- `POST /api/v1/products`
- `PUT /api/v1/products/{id}`
- `PATCH /api/v1/products/{id}/status`
- `GET /api/v1/inventory?query=`
- `GET /api/v1/inventory/movements?productId=`
- `POST /api/v1/inventory/adjustments`
- `GET /api/v1/inventory/lots`
- `POST /api/v1/inventory/lots`

## Estoque

O saldo atual fica em `product_stock`; toda alteração manual gera um registro em
`stock_movement`. O registro de um lote perecível também cria uma movimentação de
entrada e soma a quantidade ao saldo do produto. As operações são transacionais e
usam bloqueio pessimista do saldo durante a atualização, evitando perda de atualização
em acessos concorrentes.

## Testes

```bash
./mvnw test
```

## Observação de segurança

Autenticação e autorização ainda não fazem parte desta primeira API. Ela deve ficar protegida antes de qualquer publicação pública.
