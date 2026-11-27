# Evidência da gravação

Gravação de 27/11/2026, feita com o sistema DaFabi Frangos em execução local:

- código da branch `fix/qa-25-11` (commit b05b67c), sem alterações locais;
- API Spring Boot no perfil `dev`, com PostgreSQL 16 descartável criado só para a gravação;
- front Angular servido por `ng serve` com o proxy do projeto;
- navegador Microsoft Edge controlado por Playwright (`capturar.py`), 1440 x 900, pt-BR, fuso America/Sao_Paulo.

Dados: somente os dados demonstrativos criados pelo perfil `dev` (usuários `admin` e `caixa`, quatro categorias, três produtos) e os cadastros feitos durante a gravação (Marmita de Frango 500g, fornecedor Granja Boa Vista, compra de 20 frangos com lote L2711, duas vendas, um estorno, uma despesa). O relatório do iFood é sintético (`relatorio-pedidos_demonstracao.xlsx`, gerado por `gerar_relatorio_ifood_sintetico.py` com as mesmas colunas usadas nos testes do back). Nenhum dado de cliente ou relatório real foi usado.

Rede: durante a gravação, o navegador só acessou localhost e as fontes do Google que a interface carrega.

## Comportamentos observados

- O resultado de cada ação aparece na tela gravada: cadastro na lista, compra registrada e paga, lote em Perecíveis, caixa aberto, venda com troco, estorno concluído, despesa pendente e depois paga, importação concluída, caixa fechado com diferença, painel atualizado e acesso negado ao operador.
- Defeito observado: logo após o login, a consulta do caixa atual é feita antes do token e falha com 401. O front guarda esse estado até recarregar a página. Efeitos vistos: aviso "Sua sessão expirou" na tela de login aberta pela primeira vez; PDV mostrando "Caixa fechado" com o caixa aberto; resumo do caixa com "Caixa indisponível". Na gravação, a página é recarregada após o login para contornar o defeito.
