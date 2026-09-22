# Documento de Interface — Sprint 1

## Objetivo

Definir a navegação, os padrões visuais e os fluxos prioritários da primeira versão navegável da DaFabi Frangos. A interface foi desenhada para uma operação pequena, com poucos cliques, leitura rápida e distinção clara entre Administradora e Operador de caixa.

## Mapa de navegação

```text
Login
└── Área autenticada
    ├── Dashboard
    ├── Vendas
    │   ├── Nova venda / PDV
    │   ├── Histórico
    │   └── Detalhes
    ├── Caixa
    │   ├── Abrir
    │   ├── Resumo
    │   └── Fechar
    ├── Produtos
    │   ├── Lista
    │   ├── Novo
    │   ├── Detalhes
    │   └── Editar
    ├── Estoque
    │   ├── Posição
    │   ├── Ajuste manual
    │   └── Perecíveis
    ├── Compras e fornecedores
    ├── Despesas
    ├── Integrações iFood e 99Food
    ├── Estornos
    └── Configurações, usuários e permissões
```

## Perfis e visibilidade

- Administradora: vê todas as seções e ações administrativas.
- Operador de caixa: acessa PDV, histórico, caixa, consulta de produtos e estoque.
- Rotas administrativas são protegidas por guard, além de ficarem ocultas no menu do Operador.
- Tentativas de acesso sem autorização direcionam para uma tela de acesso negado.

## Padrões de interface

- Sidebar recolhível, topbar com usuário e perfil e breadcrumbs.
- Base neutra branca e cinza; vermelho nas ações primárias; amarelo nos destaques e totais.
- Tabelas responsivas com rolagem horizontal em telas menores.
- Formulários com labels, validação associada ao campo e ações alinhadas no final.
- Confirmação antes de inativação, cancelamento e estorno.
- Feedback por mensagens persistentes no contexto e notificações temporárias.

## Fluxos prioritários validados

1. Autenticação e controle de acesso: login por dois perfis, redirecionamento específico, menus filtrados e guards.
2. Gestão de produtos: listar, pesquisar, filtrar, consultar, cadastrar, editar e inativar com confirmação.
3. PDV: adicionar por código, pesquisa ou botão visual; alterar quantidade; remover; cancelar; pagar e atualizar estoque.

## Estados previstos

Carregamento com skeleton, vazio, erro, sucesso, validação, confirmação, sessão expirada, acesso negado, produto não encontrado, estoque insuficiente, carrinho vazio, valor insuficiente, integração indisponível, arquivo inválido e nenhuma venda encontrada.

## Responsividade e acessibilidade

O layout prioriza desktop e tablet. A sidebar vira drawer em larguras menores; formulários e grids passam a uma coluna quando necessário. Há foco visível, navegação por teclado, labels, regiões nomeadas, textos associados aos estados e ícones acompanhados por texto.
