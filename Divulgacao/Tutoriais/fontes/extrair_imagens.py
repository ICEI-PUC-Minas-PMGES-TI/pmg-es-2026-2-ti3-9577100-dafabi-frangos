"""Extrai das gravações as capturas usadas no manual e nos guias (WebP em imagens/).

Uso: python extrair_imagens.py   (requer ffmpeg e Pillow)
Cada captura é um quadro da gravação real, escolhido por marca + deslocamento.
"""
import json
import subprocess
import tempfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
CAP = HERE.parent / "capturas"
MARKS = json.loads((CAP / "marcas.json").read_text(encoding="utf-8"))
OUT = HERE / "imagens"
OUT.mkdir(exist_ok=True)

SHOTS = {
    "login": ("c01-entrada-admin", "login-visivel", 1.0),
    "dashboard-vazio": ("c01-entrada-admin", "menu", 1.0),
    "menu-operador": ("c06-abrir-caixa", "menu-operador", 1.5),
    "abrir-caixa": ("c06-abrir-caixa", "abrir", -0.3),
    "resumo-caixa-aberto": ("c06-abrir-caixa", "resumo-caixa", 2.5),
    "pdv-carrinho": ("c07-venda", "total", 1.0),
    "pagamento-troco": ("c07-venda", "troco", 1.2),
    "venda-sucesso": ("c07-venda", "sucesso", 1.5),
    "produto-form": ("c02-produto", "salvar", -0.5),
    "produto-lista": ("c02-produto", "produto-na-lista", 2.0),
    "fornecedor-lista": ("c03-fornecedor", "fornecedor-na-lista", 2.0),
    "compra-form": ("c04-compra", "registrar", -0.6),
    "compra-paga": ("c04-compra", "compra-paga", 2.5),
    "estoque": ("c05-estoque", "tabela-estoque", 2.0),
    "pereciveis": ("c05-estoque", "lote", 2.0),
    "historico": ("c08-estorno", "tabela-vendas", 1.5),
    "estorno-form": ("c08-estorno", "aviso-reversao", 1.0),
    "estorno-concluido": ("c08-estorno", "estorno-concluido", 2.0),
    "despesa-form": ("c09-despesa", "aviso-resultado", 1.5),
    "despesa-pendente": ("c09-despesa", "despesa-pendente", 1.5),
    "despesa-paga": ("c09-despesa", "total-pago", 2.5),
    "ifood-previa": ("c10-ifood", "totais", 2.0),
    "ifood-linhas": ("c10-ifood", "linhas", 1.5),
    "ifood-importado": ("c10-ifood", "importado", 2.0),
    "fechar-diferenca": ("c11-fechar-caixa", "confirmar", -0.6),
    "caixa-fechado": ("c11-fechar-caixa", "fechado", 2.0),
    "dashboard": ("c12-dashboard", "cards", 2.0),
    "dashboard-composicao": ("c12-dashboard", "recentes", 2.5),
    "acesso-negado": ("c13-acesso-operador", "acesso-negado", 2.0),
}

with tempfile.TemporaryDirectory() as tmp:
    for name, (clip, mark, offset) in SHOTS.items():
        frame = Path(tmp) / f"{name}.png"
        t = MARKS[clip][mark]["time"] + offset
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-ss", f"{t:.2f}", "-i", str(CAP / f"{clip}.webm"),
                        "-frames:v", "1", str(frame)], check=True)
        Image.open(frame).convert("RGB").save(OUT / f"{name}.webp", quality=82)
print(len(SHOTS), "imagens")
