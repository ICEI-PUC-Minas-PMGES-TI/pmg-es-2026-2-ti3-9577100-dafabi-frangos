# Tutoriais do DaFabi Frangos

Material de demonstração e ensino produzido em 27/11/2026 a partir do sistema em execução local, com dados sintéticos. São dois públicos:

| Pacote | Público | Abra |
|---|---|---|
| Demonstração completa | Professores e banca | `pacote-professores/index.html` |
| Guia prático da loja | Fabiana e operadores de caixa | `pacote-loja/index.html` |

Cada pacote funciona offline e traz manual completo, guia rápido com capturas reais, vídeo narrado com legenda e transcrição, prática com perguntas e o manual em Markdown para consulta por IA (`manual-ia.md`). O guia da loja também sai em PDF para imprimir (`Guia-pratico-loja.pdf`).

Os pacotes são **candidatos**: o vídeo e as páginas foram conferidos pela equipe técnica, mas ainda não foram aplicados a uma pessoa do público. Isso não comprova aprendizagem.

## Estrutura

- `fontes/capturar.py`: grava as jornadas com Playwright e Microsoft Edge e registra as marcações em `capturas/marcas.json`.
- `capturas/`: gravações `.webm` originais (1440 x 900).
- `fontes/montar_roteiros.py`: gera os roteiros `roteiro-professores.json` e `roteiro-loja.json`, com o SHA-256 de cada gravação.
- `fontes/extrair_imagens.py`: extrai das gravações as imagens do manual.
- `fontes/EVIDENCIA.md`: ambiente, dados e defeitos observados na gravação.
- `conteudo-*.json` e `projeto-*.yaml`: fonte única de cada pacote.
- `videos/v3-*`: vídeos renderizados, legendas, transcrições e `render-report.json`.

## Regenerar

1. Recrie um banco vazio, suba a API no perfil `dev` e o front (`Codigo/back/README.md` e `Codigo/front/README.md`).
2. Em `fontes/`:

```powershell
python gerar_relatorio_ifood_sintetico.py relatorio-pedidos_demonstracao.xlsx
python capturar.py ../capturas relatorio-pedidos_demonstracao.xlsx
python montar_roteiros.py
python extrair_imagens.py
```

3. Renderize e monte com o renderizador e o montador do pacote AI Showcase (`programa.video_ensino` e `programa.pacote_ensino`), usando `fontes/video.yaml`, os roteiros e os `projeto-*.yaml`. Use sempre uma pasta de saída nova e rode `check` no pacote gerado.

A captura altera o banco: rode sempre num banco descartável recém-criado. A narração usa Edge TTS e envia apenas o texto do roteiro.
