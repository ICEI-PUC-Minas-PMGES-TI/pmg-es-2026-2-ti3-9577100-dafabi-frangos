# Sistema de Gestão da DaFabi Frangos

Projeto desenvolvido para a unidade curricular **Trabalho Interdisciplinar: Aplicações para Cenários Reais**, do curso de Engenharia de Software da PUC Minas.

A solução proposta é um sistema web de ponto de venda e gestão financeira para a DaFabi Frangos. O objetivo é centralizar o registro de vendas, o controle de caixa, estoque, compras e despesas, tornando o atendimento mais rápido e oferecendo uma visão clara dos resultados diários e mensais do negócio.

Entre as funcionalidades estão o cadastro de produtos, vendas por código de barras ou seleção visual, diferentes formas de pagamento, abertura e fechamento de caixa, controle de estoque e acompanhamento de produtos perecíveis. O protótipo também consolida vendas do iFood e da 99Food por sincronização ou importação simulada de relatórios.

## Alunos integrantes da equipe

- André Leôncio Jales
- Davi Vinicius Barbosa de Oliveira
- Gustavo Alvarenga Ribeiro Carvalho
- Gustavo Pereira Felix
- Luca Moreira Ribeiro Mazala de Araujo

## Professores responsáveis

- Ian Guelman
- Rafael Henriques Nogueira Diniz

## Cliente

- **Empresa:** DaFabi Frangos
- **Product Owner:** Fabiana Nogueira

## Organização do repositório

- [`Artefatos`](./Artefatos): documentação do projeto e entregas das sprints.
- [`Codigo/front`](./Codigo/front): aplicação Angular.
- [`Codigo/back`](./Codigo/back): API Spring Boot.
- [`Divulgacao`](./Divulgacao): apresentação, vídeo e outros materiais de divulgação.

## Documentação

Os documentos da Sprint 0 estão disponíveis em [`Artefatos/Sprint-0`](./Artefatos/Sprint-0), incluindo o Documento de Visão, a ata de reunião com a cliente e os termos assinados. O planejamento da Sprint 1, com a divisão de tarefas e responsabilidades, está registrado no board do projeto.

## Instruções de utilização

A primeira versão navegável está separada em frontend e backend. A interface Angular está em [`Codigo/front`](./Codigo/front) e a API Spring Boot em [`Codigo/back`](./Codigo/back). Nesta entrega, Produtos já utiliza a API e PostgreSQL; os demais domínios ainda usam dados de demonstração até que suas APIs sejam implementadas. A autenticação da interface também continua mockada, pois a API ainda não oferece autenticação.

```bash
cd Codigo/front
pnpm install
pnpm start
```

Em outro terminal, inicie o backend conforme o [`README do backend`](./Codigo/back/README.md). Consulte o [`README do frontend`](./Codigo/front/README.md) para os acessos de demonstração, arquitetura e detalhes de compilação. A documentação da Sprint 1 está em [`Artefatos/Sprint-1`](./Artefatos/Sprint-1).
