"""Grava as jornadas do DaFabi Frangos em execução local, com dados sintéticos.

Pré-requisitos: API no perfil dev em http://localhost:8080 (banco recém-criado),
front em http://localhost:4200 e Microsoft Edge instalado.

Uso: python capturar.py <pasta_capturas> <relatorio_ifood.xlsx>
Cada clipe vira um .webm na pasta e as marcações (tempo e retângulo dos
elementos) ficam em marcas.json, para alinhar os destaques do roteiro.
As requisições para fora de localhost são bloqueadas durante a gravação, exceto as
fontes do Google que a interface carrega.
"""
from __future__ import annotations

import json
import os
import re
import shutil
import sys
from pathlib import Path
from time import monotonic
from urllib.parse import urlparse

from playwright.sync_api import expect, sync_playwright

VIEW = {"width": 1440, "height": 900}
ORIGIN = "http://localhost:4200"
CURSOR = """
document.addEventListener('DOMContentLoaded', () => {
  const c = document.createElement('div');
  c.style.cssText = 'position:fixed;left:-40px;top:-40px;width:18px;height:18px;' +
    'border:2px solid white;border-radius:50%;background:#b42318;box-shadow:0 0 0 1px #8f1d14,0 2px 6px rgba(0,0,0,.35);' +
    'pointer-events:none;z-index:2147483647;transform:translate(-50%,-50%);transition:scale 100ms, background 100ms';
  document.body.append(c);
  const pos = sessionStorage.getItem('__cursor');
  if (pos) { const [x, y] = JSON.parse(pos); c.style.left = x + 'px'; c.style.top = y + 'px'; }
  document.addEventListener('mousemove', e => {
    c.style.left = e.clientX + 'px'; c.style.top = e.clientY + 'px';
    sessionStorage.setItem('__cursor', JSON.stringify([e.clientX, e.clientY]));
  }, true);
  document.addEventListener('mousedown', () => { c.style.scale = '1.7'; c.style.background = '#f4c542'; }, true);
  document.addEventListener('mouseup', () => setTimeout(() => { c.style.scale = '1'; c.style.background = '#b42318'; }, 140), true);
});
"""


class Clip:
    def __init__(self, browser, name, out_dir, marks):
        self.name, self.out_dir, self.marks = name, out_dir, marks
        self.tmp = out_dir / f"_tmp_{name}"
        self.context = browser.new_context(viewport=VIEW, record_video_dir=self.tmp, record_video_size=VIEW,
                                           locale="pt-BR", timezone_id="America/Sao_Paulo", service_workers="block")
        self.denied = []

        def route(request):
            host = urlparse(request.request.url).hostname
            if host in ("localhost", "127.0.0.1", "fonts.googleapis.com", "fonts.gstatic.com"):
                request.continue_()
            else:
                self.denied.append(host)
                request.abort()

        self.context.route("**/*", route)
        self.context.add_init_script(CURSOR)
        self.page = self.context.new_page()
        self.t0 = monotonic()
        self.marks[name] = {}
        self.x, self.y = 720, 450

    # ---- utilidades -------------------------------------------------
    def now(self):
        return round(monotonic() - self.t0, 3)

    def wait(self, ms):
        self.page.wait_for_timeout(ms)

    def mark(self, label, locator=None, pad=6):
        entry = {"time": self.now()}
        if locator is not None:
            box = locator.bounding_box()
            if box:
                x = max(0.0, box["x"] - pad); y = max(0.0, box["y"] - pad)
                w = min(VIEW["width"] - x, box["width"] + 2 * pad); h = min(VIEW["height"] - y, box["height"] + 2 * pad)
                entry["rect"] = [round(x / VIEW["width"], 5), round(y / VIEW["height"], 5),
                                 round(w / VIEW["width"], 5), round(h / VIEW["height"], 5)]
        self.marks[self.name][label] = entry
        return entry

    def point(self, locator, steps=18):
        locator.scroll_into_view_if_needed()
        box = locator.bounding_box()
        assert box, "elemento sem caixa"
        tx, ty = box["x"] + box["width"] / 2, box["y"] + box["height"] / 2
        self.page.mouse.move(tx, ty, steps=steps)
        self.x, self.y = tx, ty

    def click(self, locator, label=None, before=450, after=700):
        self.point(locator)
        if label:
            self.mark(label, locator)
        self.wait(before)
        locator.click()
        self.wait(after)

    def type(self, locator, text, label=None, delay=75):
        self.point(locator)
        if label:
            self.mark(label, locator)
        self.wait(250)
        self.page.mouse.click(self.x, self.y)
        locator.focus()
        self.wait(150)
        locator.press("Control+a")
        self.page.keyboard.type(text, delay=delay)
        self.wait(350)

    def choose(self, name, option, label=None):
        self.click(self.page.get_by_role("combobox", name=name), label, before=300, after=500)
        opt = self.page.get_by_role("option", name=option).first
        self.click(opt, None, before=300, after=500)

    def goto(self, path):
        self.page.goto(ORIGIN + path)
        self.page.wait_for_load_state("networkidle")

    def login(self, who):
        self.goto("/login")
        card = self.page.get_by_role("button", name=who)
        self.click(card, "login-" + who, before=900, after=600)
        self.page.wait_for_url(re.compile(r"/app/"))
        self.page.wait_for_load_state("networkidle")
        # Contorno do defeito observado: o estado do caixa consultado antes do
        # login fica em cache como "não autenticado" até recarregar a página.
        self.page.reload(); self.page.wait_for_load_state("networkidle")

    def close(self):
        video = self.page.video
        self.context.close()
        target = self.out_dir / f"{self.name}.webm"
        os.replace(video.path(), target)
        shutil.rmtree(self.tmp, ignore_errors=True)
        if self.denied:
            print(f"{self.name}: bloqueado fora de localhost: {sorted(set(self.denied))}")
        print(f"{self.name}: {target.name} ({self.now():.1f}s)")


def money(text):
    return float(re.sub(r"[^0-9,]", "", text).replace(",", "."))


def main():
    out = Path(sys.argv[1]).resolve(); out.mkdir(parents=True, exist_ok=True)
    report = Path(sys.argv[2]).resolve()
    marks = {}
    with sync_playwright() as p:
        browser = p.chromium.launch(channel="msedge")

        # 01. Entrada da administradora e painel vazio
        c = Clip(browser, "c01-entrada-admin", out, marks); pg = c.page
        c.goto("/login"); c.mark("login-visivel"); c.wait(2500)
        c.click(pg.get_by_role("button", name="Administradora"), "card-admin", before=900, after=400)
        pg.wait_for_url(re.compile(r"/app/dashboard")); pg.wait_for_load_state("networkidle")
        c.mark("dashboard", pg.get_by_role("heading", level=1)); c.wait(1800)
        c.point(pg.get_by_role("navigation", name="Menu principal")); c.mark("menu", pg.get_by_role("navigation", name="Menu principal"), 2)
        c.wait(2500)
        c.point(pg.get_by_role("radio", name="Este mês")); c.mark("periodo", pg.get_by_role("radiogroup").first)
        c.wait(2500)
        c.close()

        # 02. Cadastro de produto
        c = Clip(browser, "c02-produto", out, marks); pg = c.page
        c.login("Administradora")
        c.click(pg.get_by_role("link", name="Produtos", exact=True).first, "menu-produtos", after=1200)
        c.mark("lista-produtos", pg.get_by_role("region", name="Lista de produtos")); c.wait(1500)
        c.click(pg.get_by_role("button", name="Novo produto"), "novo-produto", after=1200)
        c.type(pg.get_by_label("Nome do produto"), "Marmita de Frango 500g", "nome")
        c.choose("Categoria", "Marmitas", "categoria")
        c.type(pg.get_by_label("Preço de venda"), "22.90", "preco")
        c.type(pg.get_by_label("Custo", exact=True), "11.00", "custo")
        c.type(pg.get_by_label("Quantidade inicial em estoque"), "15", "quantidade")
        c.type(pg.get_by_label("Código de barras (opcional)"), "7891000000042", "codigo")
        frequent = pg.get_by_label("Exibir nos produtos frequentes do PDV")
        c.mark("frequente", frequent); c.wait(800)
        if not frequent.is_checked():
            c.click(frequent, None)
        c.click(pg.get_by_role("button", name="Salvar produto"), "salvar", after=1500)
        row = pg.get_by_role("row").filter(has_text="Marmita de Frango 500g")
        expect(row).to_be_visible(); c.mark("produto-na-lista", row); c.wait(3500)
        c.close()

        # 03. Fornecedor
        c = Clip(browser, "c03-fornecedor", out, marks); pg = c.page
        c.login("Administradora")
        c.click(pg.get_by_role("link", name="Fornecedores", exact=True).first, "menu-fornecedores", after=1200)
        c.click(pg.get_by_role("link", name="Novo fornecedor").first, "novo-fornecedor", after=1200)
        c.type(pg.get_by_label("Nome do fornecedor"), "Granja Boa Vista", "nome")
        c.type(pg.get_by_label("Telefone"), "31988887777", "telefone")
        c.type(pg.get_by_label("E-mail (opcional)"), "contato@granjaboavista.test", "email", delay=45)
        c.click(pg.get_by_role("button", name="Salvar fornecedor"), "salvar", after=1200)
        row = pg.get_by_role("row").filter(has_text="Granja Boa Vista")
        expect(row).to_be_visible(); c.mark("fornecedor-na-lista", row); c.wait(3500)
        c.close()

        # 04. Compra de perecível com lote e validade, e pagamento
        c = Clip(browser, "c04-compra", out, marks); pg = c.page
        c.login("Administradora")
        c.click(pg.get_by_role("link", name="Compras", exact=True).first, "menu-compras", after=1200)
        c.click(pg.get_by_role("link", name="Nova compra").first, "nova-compra", after=1200)
        c.choose("Fornecedor", "Granja Boa Vista", "fornecedor")
        c.choose("Produto", "Frango Assado Tradicional", "produto")
        c.type(pg.get_by_label("Quantidade"), "20", "quantidade")
        c.type(pg.get_by_label("Custo unitário"), "24.50", "custo")
        c.type(pg.get_by_label("Lote recebido para produto perecível"), "L2711", "lote")
        validade = pg.get_by_label("Validade do lote")
        c.click(validade, "validade", before=300, after=200)
        validade.fill("2026-11-30"); c.wait(900)
        c.mark("total", pg.get_by_text("Valor total").locator(".."))
        c.wait(1500)
        c.click(pg.get_by_role("button", name="Registrar compra"), "registrar", after=1500)
        row = pg.get_by_role("row").filter(has_text="Granja Boa Vista")
        expect(row).to_be_visible(); c.mark("compra-na-lista", row); c.wait(2500)
        c.click(row.get_by_role("button", name="Pagar", exact=True), "pagar", after=900)
        dialog = pg.get_by_role("dialog")
        c.mark("dialogo-pagamento", dialog)
        c.click(dialog.get_by_role("radio", name="Pix"), "pix", after=600)
        c.click(dialog.get_by_role("button", name="Registrar pagamento"), "registrar-pagamento", after=1500)
        c.mark("compra-paga", pg.get_by_role("row").filter(has_text="Granja Boa Vista")); c.wait(3000)
        c.close()

        # 05. Estoque e perecíveis
        c = Clip(browser, "c05-estoque", out, marks); pg = c.page
        c.login("Administradora")
        c.click(pg.get_by_role("link", name="Estoque", exact=True).first, "menu-estoque", after=1500)
        c.mark("tabela-estoque", pg.get_by_role("table")); c.wait(3500)
        c.click(pg.get_by_role("link", name="Perecíveis"), "pereciveis", after=1500)
        c.mark("alerta-sem-lote", pg.get_by_role("status").first); c.wait(3500)
        lot = pg.get_by_role("row").filter(has_text="L2711")
        c.point(lot); c.mark("lote", lot); c.wait(4000)
        c.close()

        # 06. Entrada do operador, menu restrito e abertura do caixa
        c = Clip(browser, "c06-abrir-caixa", out, marks); pg = c.page
        c.goto("/login"); c.wait(1200)
        c.click(pg.get_by_role("button", name="Operador de caixa"), "card-operador", before=900, after=500)
        pg.wait_for_url(re.compile(r"/app/")); pg.wait_for_load_state("networkidle")
        pg.reload(); pg.wait_for_load_state("networkidle"); c.wait(600)  # contorno do defeito do caixa após login
        c.mark("menu-operador", pg.get_by_role("navigation", name="Menu principal"), 2); c.wait(3500)
        aviso = pg.get_by_text("Caixa fechado").locator("..").locator("..")
        c.point(pg.get_by_text("Caixa fechado")); c.mark("caixa-fechado", aviso); c.wait(2500)
        c.click(pg.get_by_role("link", name="Abrir caixa").first, "abrir-link", after=1300)
        c.type(pg.get_by_label("Saldo inicial"), "150", "saldo-inicial")
        c.wait(800)
        c.click(pg.get_by_role("button", name="Abrir caixa"), "abrir", after=900)
        dialog = pg.get_by_role("dialog"); c.mark("confirmar", dialog); c.wait(1500)
        c.click(dialog.get_by_role("button", name="Abrir caixa"), "confirmar-abrir", after=1500)
        c.mark("resumo-caixa", pg.locator("main")); c.wait(3500)
        c.close()

        # 07. Venda em dinheiro com troco
        c = Clip(browser, "c07-venda", out, marks); pg = c.page
        c.login("Operador de caixa")
        c.goto("/app/vendas/nova"); c.wait(1500)
        c.type(pg.get_by_role("textbox", name="Código de barras"), "7891000000011", "codigo-barras", delay=90)
        pg.keyboard.press("Enter"); c.wait(1200)
        c.mark("item-frango", pg.get_by_role("article").filter(has_text="Frango Assado Tradicional"))
        c.wait(1200)
        c.click(pg.get_by_role("button", name=re.compile("Marmita de Frango 500g")).first, "card-marmita", after=900)
        c.click(pg.get_by_role("button", name=re.compile("Refrigerante Guaraná 2L")).first, "card-refri", after=900)
        c.click(pg.get_by_role("button", name="Aumentar Refrigerante Guaraná 2L"), "mais-refri", after=900)
        total = pg.get_by_text("Total da venda").locator("..")
        c.mark("total", total); c.wait(2000)
        c.click(pg.get_by_role("button", name=re.compile("Continuar para pagamento")), "continuar", after=1000)
        dialog = pg.get_by_role("dialog")
        c.click(dialog.get_by_role("radio", name="Pagar com Dinheiro"), "dinheiro", after=700)
        c.click(dialog.get_by_role("button", name="R$ 200"), "recebido", after=900)
        c.mark("troco", dialog.get_by_text("Troco").locator("..")); c.wait(2200)
        c.click(dialog.get_by_role("button", name="Confirmar pagamento"), "confirmar", after=1500)
        ok = pg.get_by_role("dialog")
        expect(ok.get_by_role("heading", name="Venda realizada com sucesso")).to_be_visible()
        c.mark("sucesso", ok); c.wait(4500)
        c.click(ok.get_by_role("button", name="Nova venda"), "nova-venda", after=800)
        # Segunda venda, rápida, para demonstrar o estorno depois
        c.click(pg.get_by_role("button", name=re.compile("Farofa Caseira Crocante")).first, "venda2-farofa", after=700)
        c.click(pg.get_by_role("button", name=re.compile("Continuar para pagamento")), "venda2-continuar", after=900)
        dialog = pg.get_by_role("dialog")
        c.click(dialog.get_by_role("radio", name="Pagar com Dinheiro"), "venda2-dinheiro", after=500)
        c.click(dialog.get_by_role("button", name="Valor exato"), "venda2-exato", after=500)
        c.click(dialog.get_by_role("button", name="Confirmar pagamento"), "venda2-confirmar", after=1500)
        c.mark("venda2-sucesso", pg.get_by_role("dialog")); c.wait(2500)
        c.close()

        # 08. Histórico e estorno (administradora)
        c = Clip(browser, "c08-estorno", out, marks); pg = c.page
        c.login("Administradora")
        c.click(pg.get_by_role("link", name="Histórico de vendas", exact=True).first, "menu-historico", after=1500)
        c.mark("tabela-vendas", pg.get_by_role("table").first); c.wait(3500)
        c.click(pg.get_by_role("link", name="Estornos", exact=True).first, "menu-estornos", after=1200)
        c.click(pg.get_by_role("link", name="Novo estorno").first, "novo-estorno", after=1200)
        c.type(pg.get_by_label("Número da venda"), "2", "numero")
        c.click(pg.get_by_role("button", name="Buscar venda"), "buscar", after=1500)
        c.mark("venda-encontrada", pg.get_by_role("article").first); c.wait(3000)
        c.type(pg.get_by_label("Motivo do estorno"), "Cliente desistiu da compra no balcão", "motivo", delay=45)
        c.mark("aviso-reversao", pg.get_by_text("Será revertido")); c.wait(2500)
        c.click(pg.get_by_role("button", name="Confirmar estorno"), "confirmar", after=900)
        dialog = pg.get_by_role("dialog")
        if dialog.count():
            c.mark("dialogo-estorno", dialog); c.wait(1500)
            c.click(dialog.get_by_role("button", name=re.compile("estorno|Confirmar", re.I)).last, "confirmar-dialogo", after=1500)
        c.mark("estorno-concluido", pg.locator("main")); c.wait(4000)
        c.close()

        # 09. Despesa paga em dinheiro
        c = Clip(browser, "c09-despesa", out, marks); pg = c.page
        c.login("Administradora")
        c.click(pg.get_by_role("link", name="Despesas", exact=True).first, "menu-despesas", after=1200)
        c.click(pg.get_by_role("link", name="Nova despesa").first, "nova-despesa", after=1200)
        c.type(pg.get_by_label("Descrição"), "Botijão de gás da cozinha", "descricao", delay=55)
        c.choose("Categoria", "Operacional", "categoria")
        c.type(pg.get_by_label("Valor"), "120", "valor")
        c.choose("Forma de pagamento", "Dinheiro", "forma")
        c.mark("aviso-resultado", pg.get_by_text("Esta despesa será considerada", exact=False)); c.wait(2500)
        c.click(pg.get_by_role("button", name="Registrar despesa"), "registrar", after=1500)
        dialog = pg.get_by_role("dialog")
        if dialog.count():
            c.mark("dialogo-despesa", dialog); c.wait(1200)
            c.click(dialog.get_by_role("button").last, "confirmar-dialogo", after=1500)
        row = pg.get_by_role("row").filter(has_text="Botijão de gás da cozinha")
        expect(row).to_be_visible(); c.mark("despesa-pendente", row); c.wait(3500)
        c.click(row.get_by_role("button", name="Marcar como paga"), "marcar-paga", after=900)
        dialog = pg.get_by_role("dialog"); c.mark("dialogo-paga", dialog); c.wait(2000)
        c.click(dialog.get_by_role("button", name="Confirmar pagamento"), "confirmar-paga", after=1500)
        c.mark("despesa-paga", pg.get_by_role("row").filter(has_text="Botijão de gás da cozinha"))
        c.mark("total-pago", pg.get_by_text("Despesas pagas no período").locator("..").locator(".."))
        c.wait(4000)
        c.close()

        # 10. Importação iFood com relatório sintético
        c = Clip(browser, "c10-ifood", out, marks); pg = c.page
        c.login("Administradora")
        c.click(pg.get_by_role("link", name="Integrações", exact=True).first, "menu-integracoes", after=1500)
        c.mark("cards-integracoes", pg.get_by_role("article").first); c.wait(2500)
        c.click(pg.get_by_role("link", name="Abrir integração").first, "abrir-ifood", after=1500)
        drop = pg.get_by_role("button", name=re.compile("Escolha os arquivos"))
        c.point(drop); c.mark("area-arquivo", drop); c.wait(1200)
        pg.locator("input[type=file]").set_input_files(str(report)); c.wait(1200)
        c.click(pg.get_by_role("button", name="Analisar relatório"), "analisar", after=2500)
        c.mark("previa", pg.get_by_role("heading", name="Prévia da importação")); c.wait(1500)
        pg.mouse.wheel(0, 380); c.wait(800)
        c.mark("totais", pg.get_by_text("Pedidos lidos").locator("..").locator("..")); c.wait(4000)
        pg.mouse.wheel(0, 520); c.wait(1500)
        c.mark("linhas", pg.get_by_role("table", name="Linhas do arquivo para revisão")); c.wait(3000)
        c.click(pg.get_by_role("button", name="Confirmar este arquivo"), "confirmar", after=2500)
        c.mark("importado", pg.locator("main")); c.wait(3500)
        c.close()

        # 11. Fechamento do caixa com diferença justificada (operador)
        c = Clip(browser, "c11-fechar-caixa", out, marks); pg = c.page
        c.login("Operador de caixa")
        c.click(pg.get_by_role("link", name="Caixa", exact=True).first, "menu-caixa", after=1500)
        c.mark("resumo", pg.locator("main")); c.wait(4000)
        c.click(pg.get_by_role("link", name="Fechar caixa").first, "fechar-link", after=1500)
        esperado = pg.get_by_text("Saldo esperado").locator("..").locator("strong").first
        valor = money(esperado.inner_text())
        c.mark("esperado", esperado.locator("..")); c.wait(2500)
        contado = f"{valor - 2:.2f}"
        c.type(pg.get_by_label("Valor contado"), contado, "contado")
        c.wait(1200)
        just = pg.get_by_role("textbox", name=re.compile("Justificativa|justifique|Motivo", re.I))
        if just.count():
            c.mark("diferenca", just.first.locator("..").locator("..")); c.wait(2000)
            c.type(just.first, "Faltaram duas moedas de um real na contagem", "justificativa", delay=45)
        c.wait(1200)
        c.click(pg.get_by_role("button", name="Confirmar fechamento"), "confirmar", after=1000)
        dialog = pg.get_by_role("dialog")
        if dialog.count():
            c.mark("dialogo-fechar", dialog); c.wait(1500)
            c.click(dialog.get_by_role("button").last, "confirmar-dialogo", after=1800)
        c.mark("fechado", pg.locator("main")); c.wait(4000)
        c.close()

        # 12. Painel com o resultado do dia e restrição do operador
        c = Clip(browser, "c12-dashboard", out, marks); pg = c.page
        c.login("Administradora")
        c.wait(1500)
        c.mark("cards", pg.locator("main").get_by_text("Faturamento").first.locator("..").locator("..").locator(".."))
        c.wait(5000)
        c.point(pg.get_by_text("Resultado estimado").first); c.mark("resultado", pg.get_by_text("Resultado estimado").first.locator("..").locator(".."))
        c.wait(4000)
        c.point(pg.get_by_text("Vendas por origem")); c.mark("origem", pg.get_by_text("Vendas por origem").locator("..").locator(".."))
        c.wait(4000)
        pg.mouse.wheel(0, 600); c.wait(1500)
        c.mark("recentes", pg.locator("main")); c.wait(4500)
        c.close()

        c = Clip(browser, "c13-acesso-operador", out, marks); pg = c.page
        c.login("Operador de caixa")
        c.mark("menu-operador", pg.get_by_role("navigation", name="Menu principal"), 2); c.wait(2500)
        c.goto("/app/dashboard"); c.wait(1500)
        c.mark("acesso-negado", pg.locator("main")); c.wait(4000)
        c.close()

        browser.close()
    (out / "marcas.json").write_text(json.dumps(marks, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
