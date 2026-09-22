# DaFabi Frangos — Frontend

Aplicação Angular do sistema de gestão DaFabi Frangos. O projeto organiza a interface por domínio funcional e está preparado para evolução gradual da API REST.

## Tecnologias

- Angular 20, TypeScript e componentes standalone;
- Angular Material e CDK;
- Angular Router com lazy loading, `authGuard` e `roleGuard`;
- formulários reativos, signals e interceptor HTTP;
- arquitetura por features.

## O que já usa a API

O módulo **Produtos** está integrado ao backend Spring Boot em `../back`:

- listagem, pesquisa e filtro de produtos;
- categorias ativas;
- inclusão, edição e inativação;
- consulta por código de barras;
- mensagens de carregamento, vazio, erro e sucesso.

Os demais módulos continuam em modo de demonstração com dados locais enquanto suas respectivas APIs não existirem. O login também permanece mockado nesta primeira versão, porque o backend ainda não possui autenticação. Isso não deve ser usado como mecanismo de segurança em produção.

## Pré-requisitos

- Node.js 20.19+ ou 22.12+;
- pnpm 10+;
- Java 21+ e PostgreSQL, para executar a API de Produtos.

## Executar localmente

Em um terminal, inicie o backend conforme as instruções de [`../back/README.md`](../back/README.md).

Em outro terminal, dentro de `Codigo/front`:

```bash
pnpm install
pnpm start
```

Abra `http://localhost:4200`. O proxy de desenvolvimento encaminha chamadas iniciadas por `/api` para `http://localhost:8080`; portanto, não é necessário configurar CORS no navegador.

Para usar a porta 4300:

```bash
pnpm start -- --port 4300
```

## Compilar

```bash
pnpm build
```

O resultado é gerado em `../../dist/front`.

## Acessos de demonstração

| Perfil | Usuário | Senha |
|---|---|---|
| Administradora | `admin` | `admin123` |
| Operador de caixa | `caixa` | `caixa123` |

As credenciais são exclusivamente do mock de interface; não são enviadas ao backend.

## Arquitetura

O código está organizado por domínio em `src/app/features`. Cada feature mantém páginas, componentes, modelos, serviços e rotas próprios. Recursos globais ficam em `core`; componentes reutilizáveis ficam em `shared`.

```text
src/app/
├── core/       # autenticação mock, guards, layout e infraestrutura HTTP
├── shared/     # componentes reutilizáveis e estados de interface
└── features/   # auth, dashboard, sales, cash, products, inventory, etc.
```

## Contrato de Produtos usado pelo frontend

O frontend consome a base `/api/v1`:

- `GET /products`
- `GET /products/{id}`
- `GET /products/by-barcode/{barcode}`
- `POST /products`
- `PUT /products/{id}`
- `PATCH /products/{id}/status`
- `GET /categories`
