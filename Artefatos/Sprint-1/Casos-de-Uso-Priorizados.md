# Casos de Uso Priorizados — Sprint 1

## UC01 — Autenticar e controlar acesso

**Atores:** Administradora e Operador de caixa.

**Pré-condição:** usuário ativo.

**Fluxo principal:**

1. Usuário informa login e senha.
2. Sistema valida os campos e as credenciais mockadas.
3. Sistema cria a sessão local e identifica o perfil.
4. Administradora segue para o Dashboard; Operador segue para o PDV.
5. Menu e rotas exibem somente as ações permitidas.

**Alternativas:** campos inválidos, credenciais inválidas, sessão expirada e acesso a rota não autorizada.

## UC02 — Gerenciar produtos

**Ator:** Administradora; Operador possui consulta.

**Fluxo principal:**

1. Usuário consulta e filtra o catálogo.
2. Administradora cadastra nome, categoria, preço, custo, estoque, unidade e código opcional.
3. Para perecíveis, pode informar lote e validade.
4. Sistema valida, salva no mock e confirma o resultado.
5. Administradora pode editar ou inativar o produto após confirmação.

**Alternativas:** dados obrigatórios ausentes, valores inválidos, erro simulado ao salvar e lista sem resultados.

## UC03 — Adicionar produtos à venda no PDV

**Ator:** Operador de caixa ou Administradora.

**Pré-condição:** usuário autenticado e caixa aberto.

**Fluxo principal:**

1. Usuário adiciona produto por código de barras, pesquisa ou botão visual.
2. Sistema inclui o item no carrinho e recalcula subtotal e total.
3. Usuário aumenta, diminui ou remove quantidades.
4. Usuário escolhe dinheiro, Pix, débito ou crédito.
5. Em dinheiro, sistema calcula e valida o troco.
6. Sistema conclui a venda, gera o número e atualiza o estoque mockado.

**Alternativas:** produto não encontrado, sem estoque, quantidade inválida, carrinho vazio, valor recebido insuficiente e cancelamento antes da conclusão.

## Critérios de aceite da Sprint 1

- Os três fluxos são navegáveis sem backend.
- Uma venda de até três itens pode ser concluída com poucos cliques.
- O Operador não visualiza nem acessa rotas administrativas.
- Alterações de venda e produto aparecem imediatamente na interface mockada.
- Todos os formulários prioritários apresentam validação e feedback.
