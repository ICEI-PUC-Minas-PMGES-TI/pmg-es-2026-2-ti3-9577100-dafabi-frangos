# Política financeira do MVP

## Objetivo

Manter compras, despesas, caixa e resultado financeiro coerentes sem contar a mesma saída duas vezes. Os valores do dashboard são gerenciais e não substituem escrituração contábil.

## Regras adotadas

### Venda e receita

- Somente vendas concluídas compõem faturamento.
- Cancelamentos não compõem receita.
- Estorno remove a receita da venda e devolve o estoque; o custo associado à venda também é revertido.
- Taxas e demais custos de marketplace reduzem o resultado, mas repasse/liquidação não é uma nova receita.

### Compra de mercadoria e estoque

- Compra de mercadoria aumenta o estoque e atualiza o custo médio ponderado do produto.
- A compra não é uma despesa operacional e não reduz o resultado estimado no momento do recebimento.
- O pagamento de uma compra gera uma saída de caixa apenas quando efetivamente pago e somente quando o meio de pagamento é dinheiro. Pagamentos por Pix/cartão não alteram o saldo físico do caixa.
- Custo da mercadoria vendida (CMV) é reconhecido na venda usando o custo médio vigente no momento da venda, gravado no item da venda. Isso preserva o custo histórico mesmo após novas compras.
- Estorno reverte o CMV gravado nos itens da venda.
- Compra a prazo permanece pendente de pagamento; o MVP registra o estado, sem criar agenda de parcelas/contas a pagar.

### Despesas operacionais

- Nova despesa começa pendente e ainda não reduz o resultado nem o caixa.
- Ao marcar como paga/consolidada, seu valor reduz o resultado na data da despesa.
- Despesa consolidada em dinheiro gera uma saída no caixa aberto; outras formas de pagamento não alteram o saldo físico do caixa.
- Despesa consolidada não pode ser editada nem cancelada nesta versão. A correção posterior exige um lançamento compensatório/histórico próprio.
- Despesa pendente pode ser editada ou cancelada sem gerar movimentação de caixa.

## Fórmula do resultado estimado

`faturamento de vendas concluídas - CMV das vendas - despesas consolidadas - taxas e custos importados de marketplace`.

Compras não entram diretamente nessa fórmula. Seu custo passa a compor o CMV quando o produto é vendido. O pagamento da compra afeta caixa, mas não altera o resultado novamente.

O CMV de vendas presenciais usa o custo histórico guardado no item da venda. Pedidos importados não possuem associação confiável de itens aos produtos do estoque no fluxo atual; para eles o dashboard informa resultado parcial e não inventa CMV.

## Exemplos

1. Compra de 10 unidades a R$ 5,00: estoque aumenta em 10 e custo médio é atualizado. Resultado não muda. Se paga em dinheiro, o caixa diminui R$ 50,00.
2. Venda de 2 unidades com custo médio de R$ 5,00 e receita de R$ 20,00: faturamento aumenta R$ 20,00 e CMV aumenta R$ 10,00; resultado antes de outras despesas/taxas aumenta R$ 10,00.
3. Despesa de R$ 30,00 cadastrada como pendente: ainda não altera resultado nem caixa. Ao consolidar em dinheiro, resultado e caixa diminuem R$ 30,00.
4. Estorno integral da venda do exemplo 2: receita e CMV da venda são revertidos, o estoque retorna e o caixa devolve apenas o valor que havia sido recebido em dinheiro.

## Limites desta versão

- Não há parcelas, contas a pagar, conciliação bancária ou escrituração fiscal.
- Compras pagas por meios diferentes de dinheiro ficam registradas como pagamento, mas não alteram o caixa físico.
- CMV de pedidos de marketplace permanece indisponível enquanto os itens importados não forem associados aos produtos do estoque.
