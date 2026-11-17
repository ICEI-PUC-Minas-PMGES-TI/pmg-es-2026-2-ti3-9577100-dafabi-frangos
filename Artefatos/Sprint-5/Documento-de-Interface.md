# Documento de Interface — Sprint 5

> Atualização do [Documento de Interface da Sprint 1](../Sprint-1/Documento-de-Interface.md). A versão anterior descrevia um protótipo navegável sem backend, com credenciais e dados simulados. Esta versão descreve a interface **implementada e integrada à API** (Spring Boot + PostgreSQL), como está no código do front-end Angular em `Codigo/front`.

## 1. Objetivo

Registrar a navegação, os padrões visuais, as telas e os fluxos da versão atual do Sistema de Gestão da DaFabi Frangos. A interface foi pensada para uma operação pequena: poucos cliques no atendimento, leitura rápida dos números e uma separação clara entre o que a **Administradora** e o **Operador de caixa** podem fazer.

## 2. O que mudou desde a Sprint 1

| Área | Sprint 1 (protótipo) | Sprint 5 (implementado) |
|---|---|---|
| Autenticação | Credenciais simuladas e sessão local | Login pela API com JWT, renovação automática da sessão (refresh token) e logout que invalida o token |
| Dados | Banco simulado no navegador | Todas as telas leem e gravam na API |
| Dashboard | Indicadores fixos | Filtros de período (hoje, 7 dias, mês, personalizado), gráfico de faturamento, vendas por origem e resultado estimado com CMV |
| Estornos | Não existia | Lista, registro e detalhe de estornos, com reversão de caixa e estoque |
| Configurações | Tela estática | Dados da loja salvos no banco, gestão de usuários e matriz de permissões por perfil |
| Integrações | Placeholder | Importação de relatórios do iFood e da 99Food, prévia, vínculo de itens com o estoque e registro de repasses |
| Compras | Cadastro simples | Itens com lote e validade para perecíveis, registro de pagamento e cancelamento |
| Despesas | Cadastro simples | Status Pendente / Paga / Cancelada; a despesa só entra no resultado e no caixa quando é paga |
| Estoque | Posição e ajuste | Lotes de perecíveis com validade e status (válido, vencido, utilizado) |

## 3. Mapa de navegação

Rotas reais do front-end. O símbolo 🔒 marca as rotas exclusivas da Administradora.

```text
/login
└── /app (área autenticada)
    ├── /dashboard 🔒 ............................ Dashboard financeiro
    ├── /vendas
    │   ├── /nova ................................ PDV (nova venda)
    │   ├── /historico ........................... Histórico de vendas e pedidos de marketplace
    │   └── /:id ................................. Detalhes da venda
    ├── /caixa
    │   ├── /abrir ............................... Abrir caixa
    │   ├── /resumo .............................. Resumo do caixa
    │   └── /fechar .............................. Fechar caixa
    ├── /produtos
    │   ├── (lista) .............................. Catálogo
    │   ├── /novo 🔒 ............................. Cadastrar produto
    │   ├── /:id ................................. Detalhes do produto
    │   └── /:id/editar 🔒 ....................... Editar produto
    ├── /estoque
    │   ├── (lista) .............................. Posição de estoque
    │   ├── /ajustes 🔒 .......................... Ajuste manual
    │   └── /pereciveis 🔒 ....................... Lotes de perecíveis
    ├── /compras 🔒 ............................... Lista e /nova
    ├── /fornecedores 🔒 .......................... Lista, /novo e /:id/editar
    ├── /despesas 🔒 .............................. Lista, /nova e /:id/editar
    ├── /integracoes 🔒 ........................... Visão geral, /ifood e /99food
    ├── /estornos 🔒 .............................. Lista, /novo e /:id
    ├── /configuracoes 🔒 ......................... Dados da loja, /usuarios e /permissoes
    └── /acesso-negado ........................... Tela de acesso negado
```

## 4. Perfis e visibilidade

| Área | Administradora | Operador de caixa |
|---|:---:|:---:|
| Dashboard | ✅ | — |
| Nova venda (PDV) e histórico de vendas | ✅ | ✅ |
| Caixa (abrir, resumo, fechar) | ✅ | ✅ |
| Produtos | ✅ completo | ✅ só consulta |
| Estoque | ✅ completo | ✅ só consulta |
| Compras, fornecedores e despesas | ✅ | — |
| Integrações iFood / 99Food | ✅ | — |
| Estornos | ✅ | — |
| Configurações, usuários e permissões | ✅ | — |

Regras aplicadas na interface:

- O menu lateral mostra só os itens do perfil logado.
- Cada rota administrativa é protegida por guard (`roleGuard`). Se o Operador digitar a URL, cai na tela de **acesso negado**.
- A API repete a verificação de perfil. A proteção não depende só do front.
- Após o login, a Administradora vai para o Dashboard e o Operador vai para o PDV.

## 5. Padrões de interface

- **Estrutura:** menu lateral recolhível, barra superior com usuário, perfil e botão "Sair do sistema", e cabeçalho de página com trilha (breadcrumb), título e subtítulo.
- **Cores:** base branca e cinza; vermelho nas ações principais; amarelo para destaques e totais; verde para estados positivos (ex.: "Caixa aberto").
- **Componentes:** Angular Material com componentes compartilhados próprios (tabela de dados, chip de status, campo de busca, estados de carregamento/vazio/erro e diálogo de confirmação).
- **Tabelas:** filtros no topo, rolagem horizontal em telas menores e opção de modo compacto (definida em Configurações).
- **Formulários:** rótulo em todos os campos, mensagem de validação junto ao campo e ações (Cancelar / Salvar) no final.
- **Confirmações:** inativar produto, cancelar venda, cancelar compra, cancelar despesa, fechar caixa e registrar estorno sempre pedem confirmação.
- **Feedback:** avisos fixos no contexto da tela (ex.: "Caixa fechado — abra o caixa para concluir") e notificações temporárias (snackbar) após salvar.

## 6. Telas e fluxos

### 6.1 Login e sessão

- Campos "Usuário ou e-mail" e "Senha", com validação.
- Mensagens: "Usuário ou senha inválidos" e "Sua sessão expirou. Entre novamente para continuar."
- O login devolve um token de acesso (JWT) e um token de renovação. Quando o token de acesso vence, o front renova a sessão sozinho, sem o usuário perceber. Se a renovação falhar, o usuário volta ao login com o aviso de sessão expirada.
- A sessão fica no `sessionStorage`: fechar a aba encerra a sessão.
- "Sair do sistema" invalida o token de renovação na API.

### 6.2 Dashboard (Administradora)

- **Filtros de período:** Hoje, Últimos 7 dias, Este mês e Personalizado (data inicial e final, com validação).
- **Indicadores:** faturamento, entradas e saídas e **resultado estimado**. O resultado desconta CMV (custo das mercadorias vendidas), despesas pagas, taxas e custos de marketplace.
- **Gráfico de faturamento:** agrupa por dia ou por mês conforme o tamanho do período.
- **Vendas por origem:** Balcão, iFood e 99Food, com quantidade e percentual.
- **Vendas recentes:** últimas vendas, com atalho para o histórico.
- **Aviso:** se houver pedidos importados sem todas as taxas, a tela avisa que o resultado pode estar incompleto.

### 6.3 PDV — Nova venda

1. Um indicador mostra se o caixa está aberto ou fechado. Sem caixa aberto não é possível concluir a venda.
2. Para adicionar produtos: leitor ou digitação do **código de barras**, **pesquisa** por nome, ou **grade visual** com os produtos frequentes. A grade mostra o estoque e marca "Sem estoque".
3. No carrinho: aumentar, diminuir, remover e cancelar a venda. O total é recalculado a cada mudança.
4. "Continuar para pagamento" abre o diálogo de pagamento: Dinheiro, Pix, Débito ou Crédito. A forma sugerida vem das Configurações da loja. Em dinheiro, o sistema calcula o troco e bloqueia valor recebido insuficiente.
5. Ao confirmar, aparece o aviso "Venda realizada com sucesso", com as opções "Visualizar venda" e "Nova venda". O estoque baixa na hora, e para perecíveis o lote mais próximo do vencimento é usado primeiro.

### 6.4 Histórico e detalhes de venda

- **Vendas de balcão:** filtros por origem, forma de pagamento e status (Concluída, Cancelada, Estornada). Colunas de valor bruto, descontos, taxas e líquido.
- **Pedidos de marketplace:** tabela separada com os pedidos importados, os valores como informados pelo portal e uma nota de que o líquido **não** representa entrada no caixa físico.
- **Detalhe da venda:** itens, valores, origem, pagamento, operador, troco e histórico. A Administradora tem o botão "Registrar estorno".

### 6.5 Caixa

- **Abrir:** informa o operador responsável e o saldo inicial. Bloqueia se já houver caixa aberto.
- **Resumo:** saldo do caixa e movimentações em dinheiro (horário, descrição, origem, tipo e valor).
- **Fechar:** mostra o saldo esperado, o valor contado e a diferença. **Justificativa obrigatória** quando há diferença. O resumo final separa vendas, despesas, compras e estornos em dinheiro.

### 6.6 Produtos

- **Lista:** busca, filtros por categoria e status, preço, custo, estoque e código de barras, além das ações Visualizar, Editar e Inativar.
- **Cadastro e edição:** nome, categoria, unidade, preço de venda, custo, estoque inicial, código de barras (opcional), **perecível**, **frequente no PDV** e status. Depois do cadastro, o estoque só muda pelo módulo de ajustes, para manter o histórico.
- **Detalhe:** dados completos e situação de estoque.

### 6.7 Estoque e perecíveis

- **Posição:** estoque atual, custo, última movimentação e origem de cada produto.
- **Ajuste manual:** mostra a quantidade atual, a nova quantidade e a diferença calculada. Exige motivo (mínimo de 5 caracteres) e registra usuário e data.
- **Perecíveis:** registro de lote (produto, código do lote, data de entrada, validade e quantidade) e lista de lotes com status **Válido / Vencido / Utilizado**. Registrar um lote também atualiza o estoque do produto.

### 6.8 Compras e fornecedores

- **Fornecedores:** lista com telefone e e-mail, cadastro e edição (nome, telefone, e-mail, observações e status).
- **Nova compra:** fornecedor, data e itens (produto, quantidade e custo unitário). Para perecíveis, a tela pede **lote e validade**, e o lote é criado no estoque.
- **Lista de compras:** número, fornecedor, data, quantidade de itens, valor total, status da compra e status do pagamento.
- **Registrar pagamento:** forma de pagamento (Dinheiro, Pix, Débito, Crédito ou Boleto/transferência). Pagamento em dinheiro sai do caixa aberto.
- **Cancelar compra:** pede confirmação e reverte o estoque dos itens.

### 6.9 Despesas

- **Lista:** total de despesas pagas no período, filtros por data e status (Pendente, Paga, Cancelada) e ações Marcar como paga, Editar e Cancelar.
- **Formulário:** descrição, categoria (Operacional, Materiais, Serviços, Outros), valor, data, forma de pagamento e observação.
- **Regra de consolidação:** a despesa só entra no resultado estimado quando é **paga**. Se for paga em dinheiro, também sai do caixa.

### 6.10 Integrações iFood e 99Food

- **Visão geral:** um cartão por plataforma, com a última importação, os pedidos importados e as linhas de itens agregados.
- **Importação manual de relatórios:** envio de um ou mais arquivos (até 10 MB cada), análise e **prévia**:
  - período encontrado;
  - pedidos lidos, concluídos e cancelados;
  - valor dos itens, valor pago pelo cliente, líquido informado pelo portal, promoções, taxas e comissões;
  - linhas repetidas e inválidas.

  A importação só vale depois que o usuário confirma.
- **Vínculo com o estoque:** cada item do cardápio da plataforma é associado a um produto DaFabi, com a quantidade de unidades por item. Combos podem ter vários componentes, e um item pode ser marcado como "não controlar no estoque".
- **Data inicial do controle:** depois da contagem física, a Administradora define a partir de que dia os relatórios baixam o estoque. Há uma prévia das baixas antes de aplicar.
- **Repasses (conciliação):** registro de cada período de pagamento da plataforma (referência, período e valores). Repasses ficam separados das vendas e não entram de novo no faturamento nem no caixa.

### 6.11 Estornos (Administradora)

- **Lista:** venda, data, usuário, motivo, valor e status, com atalho para "Novo estorno".
- **Novo estorno:** busca pelo número da venda e verifica se ela pode ser estornada. Exige motivo (mínimo de 5 caracteres) e mostra quanto será revertido no caixa, avisando que os itens voltam ao estoque.
- **Detalhe:** confirmação "Estorno concluído" com venda, data e hora, usuário responsável, valor estornado e motivo.

### 6.12 Configurações

- **Dados da loja:** nome, telefone e endereço, além das preferências gerais: forma de pagamento sugerida no PDV e modo compacto nas tabelas. Tudo fica salvo no banco.
- **Usuários:** lista com nome, e-mail, perfil e status; criação de usuário (nome, e-mail, senha inicial com mínimo de 8 caracteres e perfil); ativação e inativação de acesso.
- **Permissões:** matriz somente leitura com o que cada perfil acessa em cada área.

## 7. Estados de tela

| Estado | Onde aparece |
|---|---|
| Carregando | Listas, dashboard, integrações e configurações |
| Vazio | Carrinho vazio, nenhum lote registrado, nenhum item importado |
| Erro com "Tentar novamente" | Todas as telas que buscam dados na API |
| Validação de campo | Todos os formulários |
| Confirmação | Inativar, cancelar, fechar caixa, estornar |
| Sucesso | Venda concluída, estorno concluído, salvar configurações e associações |
| Sessão expirada | Login, após falha na renovação do token |
| Acesso negado | Rota administrativa acessada pelo Operador |
| Regras de negócio | Caixa fechado, sem estoque, valor recebido insuficiente, venda não encontrada ou não estornável, arquivo inválido ou maior que 10 MB |
| Dados incompletos | Dashboard com pedidos importados sem taxas |

## 8. Responsividade e acessibilidade

- O layout prioriza desktop e tablet. Abaixo de ~1080 px, o PDV passa a uma coluna (produtos acima do carrinho). Em telas menores, o menu lateral vira gaveta, e formulários e grades ficam em uma coluna.
- Foco visível e navegação por teclado; rótulos associados aos campos; regiões e menus com `aria-label`; avisos com `role="status"`.
- Os ícones vêm sempre acompanhados de texto ou de rótulo acessível.

## 9. Capturas de tela

As imagens ficam em [`prints/`](./prints).

| Tela | Arquivo |
|---|---|
| Login | `prints/01-login.png` |
| Dashboard | `prints/02-dashboard.png` |
| PDV — Nova venda | `prints/03-pdv.png` |
| Pagamento | `prints/04-pagamento.png` |
| Histórico de vendas | `prints/05-historico-vendas.png` |
| Fechar caixa | `prints/06-fechar-caixa.png` |
| Produtos | `prints/07-produtos.png` |
| Perecíveis | `prints/08-pereciveis.png` |
| Compras | `prints/09-compras.png` |
| Despesas | `prints/10-despesas.png` |
| Integração iFood | `prints/11-integracao-ifood.png` |
| Estornos | `prints/12-estornos.png` |
| Configurações | `prints/13-configuracoes.png` |
