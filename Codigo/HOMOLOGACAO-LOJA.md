# Pendências para homologação na loja

Não liberar o sistema como controle principal até concluir as verificações abaixo.

- Validar com Fabiana a contagem inicial, os saldos sem lote e as datas reais de validade; regularizar sem duplicar a entrada de mercadoria.
- Confirmar os relatórios reais de iFood e 99Food, suas colunas, cancelamentos, taxas e repasses. Para perecíveis em relatórios de dias anteriores, a atribuição de lotes está bloqueada para conferência: o saldo atual não comprova quais lotes estavam disponíveis na data da venda.
- Validar no PostgreSQL descartável as migrations em banco vazio e em cópia do esquema anterior, inclusive a V24. Testar pagamentos simultâneos da mesma despesa, pagamento contra cancelamento e lançamentos contra fechamento de caixa.
- Percorrer no navegador com administradora e operador: compra de perecível, abertura, venda com dois lotes, estorno, pagamento de despesa, importação repetida, conferência de repasse e fechamento.
- Confirmar o cálculo do painel com exemplos reais de descontos, taxas, cancelamentos parciais e repasses. O custo de mercadorias de pedidos importados ainda não é reconhecido no resultado; o painel sinaliza cobertura incompleta.
- Testar o leitor físico e medir a meta original: concluir uma venda de três itens em até 60 segundos.
- Confirmar se a loja dispõe de conexão para carregar as fontes e ícones externos; o build não depende mais do Google, mas o navegador ainda depende.
