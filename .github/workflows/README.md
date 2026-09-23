# CI/CD da DaFabi Frangos

## Frontend

O projeto `dafabi-front` está conectado ao repositório no Vercel. Cada push na
`main` que altera `Codigo/front/**` dispara automaticamente um novo deployment
de produção no Vercel. O workflow `frontend-vercel.yml` executa apenas a
validação do build Angular no GitHub Actions.

## Backend

O workflow `backend-azure.yml` valida o backend e publica a imagem no GHCR.
O Container App da Azure é atualizado manualmente, pois a conta atual não tem
permissão para criar uma identidade de deploy.

Para atualizar manualmente, abra o Container App `ca-dafabi-api-dev` no grupo
`rg-dafabi-dev`, acesse **Revisões e réplicas** ou **Contêineres**, altere a
imagem para `ghcr.io/icei-puc-minas-pmges-ti/dafabi-api:sha-<SHA_DO_COMMIT>` e
crie uma nova revisão.
