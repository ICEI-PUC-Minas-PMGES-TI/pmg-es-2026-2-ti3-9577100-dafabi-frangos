# Ata de Reunião — Validação da Interface da Sprint 1

**Data:** 15/09/2026
**Projeto:** DaFabi Frangos
**Pauta:** validação do mapa de telas e dos protótipos prioritários.

## Participantes

- Equipe de desenvolvimento do projeto DaFabi Frangos;
- Product Owner: Fabiana Nogueira.

## Decisões registradas

- Priorizar autenticação, gestão de produtos e inclusão de itens no PDV.
- Manter a operação simples, sem recursos de ERP que não sejam necessários ao comércio.
- Separar visualmente e por permissão as funções administrativas e de caixa.
- Permitir produtos com e sem código de barras.
- Usar vermelho nas ações principais e amarelo somente em destaques, totais e avisos.
- Consolidar iFood e 99Food de forma periódica, sem gestão de pedidos em tempo real.
- Não incluir sangria, suprimento, alertas automáticos de validade ou estoque baixo nesta versão.

## Itens apresentados

- Fluxo de login com dois perfis.
- Shell com sidebar recolhível, topbar e breadcrumbs.
- Lista, cadastro, edição, detalhes e inativação de produtos.
- PDV com código de barras, pesquisa, produtos frequentes, carrinho e pagamento.
- Mapa das telas secundárias para dashboard, caixa, estoque e administração.

## Resultado da validação

Os três casos de uso priorizados estão cobertos pela primeira versão navegável. Os demais módulos foram mantidos coerentes com o mesmo sistema visual e com services mockados, permitindo evolução futura para uma API REST.

## Próximos passos

- Substituir os mocks por contratos de API quando o backend estiver disponível.
- Executar validação com usuários em desktop e tablet.
- Refinar regras de integração a partir dos formatos reais fornecidos por iFood e 99Food.
