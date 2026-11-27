"""Gera os roteiros (media_plan) dos dois vídeos a partir das capturas e marcações.

Uso: python montar_roteiros.py
Lê ../capturas/marcas.json e os .webm, calcula o SHA-256 de cada gravação e
escreve roteiro-professores.json e roteiro-loja.json nesta pasta.
Os destaques usam o retângulo e o instante registrados por capturar.py.
"""
import hashlib
import json
import re
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
CAP = HERE.parent / "capturas"
MARKS = json.loads((CAP / "marcas.json").read_text(encoding="utf-8"))
REL = "../capturas/"


def duration(clip):
    out = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0",
                          str(CAP / f"{clip}.webm")], capture_output=True, text=True, check=True)
    return float(out.stdout.strip())


DUR = {c: duration(c) for c in MARKS}


def anchor(clip, value):
    """Número em segundos, 'end' ou 'marca+/-deslocamento' relativo a marcas.json."""
    if isinstance(value, (int, float)):
        return float(value)
    if value == "end":
        return DUR[clip] - 0.05
    name, sign, offset = re.fullmatch(r"(.+?)([+-])([0-9.]+)", value).groups()
    return MARKS[clip][name]["time"] + (1 if sign == "+" else -1) * float(offset)


def rec(clip, start, end, title, text, highlights=()):
    start = anchor(clip, start)
    end = min(anchor(clip, end), DUR[clip] - 0.05)
    hls = []
    for mark, length in highlights:
        m = MARKS[clip][mark]
        begin = max(0.0, m["time"] - start - 0.3)
        finish = min(end - start, begin + length)
        if "rect" not in m or finish - begin < 0.6:
            raise SystemExit(f"Destaque inválido: {clip}/{mark}")
        hls.append({"start": round(begin, 2), "end": round(finish, 2), "rect": m["rect"]})
    return {"title": title, "text": text,
            "clip": {"path": REL + clip + ".webm", "start": round(start, 2), "end": round(end, 2)},
            "highlights": hls}


def diagram(title, text, boxes, note):
    return {"title": title, "text": text, "diagram": boxes, "note": note}


def interface(scenes):
    paths = sorted({s["clip"]["path"] for s in scenes if "clip" in s})
    return {
        "environment": "local",
        "reference": "Gravação de 27/11/2026 do DaFabi Frangos em execução local (branch fix/qa-25-11, perfil dev, "
                     "PostgreSQL descartável) com dados sintéticos e relatório iFood sintético",
        "authorized_by": "Luca Mazala, integrante da equipe, para a demonstração do projeto",
        "clips": {p: hashlib.sha256((HERE / p).resolve().read_bytes()).hexdigest() for p in paths},
    }


NOTE = "DaFabi Frangos · Demonstração com dados sintéticos"

professores = [
    diagram("Sistema de Gestão da DaFabi Frangos",
            "Este é o sistema web de ponto de venda e gestão financeira da DaFabi Frangos. "
            "Ele reúne vendas no balcão, caixa, compras e estoque com lotes, despesas, estornos, "
            "relatórios do iFood e da 99Food e um painel com o resultado do período.",
            ["PDV e controle de caixa", "Compras, estoque e lotes perecíveis",
             "Despesas e estornos auditáveis", "iFood, 99Food e painel financeiro"], NOTE),
    rec("c01-entrada-admin", 1.0, "end", "Entrada e perfis de acesso",
        "Na tela de entrada, o ambiente de demonstração oferece um acesso por perfil. "
        "A administradora, Fabiana, entra e vê o painel do dia. O menu lateral mostra todos os módulos: "
        "vendas, caixa, produtos, estoque, compras, fornecedores, despesas, integrações, estornos e configurações.",
        [("card-admin", 2.8), ("menu", 3.2), ("periodo", 3.0)]),
    rec("c02-produto", "menu-produtos-0.4", "end", "Cadastro de produto",
        "Em Produtos, a administradora cadastra uma Marmita de Frango de quinhentos gramas: categoria, "
        "preço de venda, custo, quantidade inicial e código de barras. Ela marca a opção de exibir nos produtos "
        "frequentes do PDV e salva. O produto aparece no catálogo com preço, custo e estoque.",
        [("novo-produto", 2.0), ("nome", 2.6), ("preco", 1.8), ("custo", 1.8), ("codigo", 2.0),
         ("frequente", 2.6), ("produto-na-lista", 4.0)]),
    rec("c03-fornecedor", "menu-fornecedores-0.4", "end", "Cadastro de fornecedor",
        "Antes de registrar compras, ela cadastra o fornecedor Granja Boa Vista, com telefone e e-mail. "
        "Na lista, telefone e e-mail viram contatos clicáveis.",
        [("novo-fornecedor", 2.0), ("nome", 2.6), ("salvar", 1.8), ("fornecedor-na-lista", 3.8)]),
    rec("c04-compra", "menu-compras-0.3", "compra-na-lista+0.3", "Compra de perecível com lote e validade",
        "Em Compras, a administradora registra a entrada de vinte frangos assados a vinte e quatro reais e cinquenta. "
        "Como o produto é perecível, o sistema pede o lote recebido e a validade. O valor total é calculado, "
        "e ao registrar, o estoque e o custo médio são atualizados e o lote é criado.",
        [("nova-compra", 2.0), ("fornecedor", 2.4), ("produto", 2.4), ("lote", 2.0), ("validade", 2.0),
         ("total", 2.0), ("registrar", 2.0)]),
    rec("c04-compra", "pagar-1.3", "end", "Pagamento da compra",
        "A compra fica com pagamento pendente até ser paga. Aqui ela registra o pagamento por Pix. "
        "Somente pagamentos em dinheiro saem do caixa.",
        [("pagar", 2.0), ("registrar-pagamento", 2.0)]),
    rec("c05-estoque", "menu-estoque-0.6", "end", "Estoque e lotes perecíveis",
        "Em Estoque, cada produto mostra o saldo, o custo e a origem da última movimentação. "
        "A aba Perecíveis lista o lote L2711 com a validade. O aviso amarelo mostra um saldo antigo sem lote: "
        "ele não pode ser vendido até ser vinculado a um lote depois da contagem.",
        [("menu-estoque", 2.0), ("pereciveis", 2.0), ("alerta-sem-lote", 3.5), ("lote", 4.0)]),
    rec("c06-abrir-caixa", "card-operador-1.5", "end", "Perfil do operador e abertura do caixa",
        "Agora entra o operador de caixa, Carlos. O menu dele é restrito: nova venda, histórico, caixa, "
        "produtos e estoque. O PDV avisa que o caixa está fechado. Ele informa o saldo inicial de cento e "
        "cinquenta reais, confirma, e o resumo do caixa passa a mostrar o atendimento aberto.",
        [("card-operador", 2.0), ("menu-operador", 3.2), ("caixa-fechado", 2.6), ("saldo-inicial", 2.2),
         ("confirmar", 2.0), ("resumo-caixa", 3.5)]),
    rec("c07-venda", "codigo-barras-1.8", "nova-venda-0.3", "Venda no PDV com troco",
        "No PDV, o frango entra pela leitura do código de barras. A marmita e o refrigerante entram pelos botões "
        "de produtos frequentes, e a quantidade é ajustada no carrinho. No pagamento em dinheiro, o atalho de "
        "duzentos reais calcula o troco. Ao confirmar, a venda é registrada, o estoque é baixado e o dinheiro "
        "entra no caixa.",
        [("codigo-barras", 2.6), ("card-marmita", 1.8), ("card-refri", 1.8), ("mais-refri", 1.6),
         ("dinheiro", 1.8), ("troco", 2.4), ("sucesso", 4.5)]),
    rec("c07-venda", "venda2-farofa-0.9", "end", "Segunda venda com valor exato",
        "Uma segunda venda, de uma farofa, usa o atalho de valor exato. O PDV fica pronto para o próximo "
        "cliente.",
        [("venda2-farofa", 1.8), ("venda2-sucesso", 3.0)]),
    rec("c08-estorno", "menu-historico-0.5", "end", "Histórico e estorno auditável",
        "No histórico, as vendas do balcão aparecem com origem, pagamento e status. O estorno é exclusivo "
        "da administradora. Ela busca a venda número dois, confere os itens e informa o motivo, que é obrigatório. "
        "O sistema avisa quanto será revertido no caixa. Depois da confirmação, o estorno fica registrado "
        "com o responsável e o motivo, e os itens voltam ao estoque.",
        [("tabela-vendas", 3.0), ("novo-estorno", 2.0), ("venda-encontrada", 3.0), ("motivo", 3.0),
         ("aviso-reversao", 2.8), ("dialogo-estorno", 2.0), ("estorno-concluido", 4.0)]),
    rec("c09-despesa", "menu-despesas-0.4", "end", "Despesa e pagamento",
        "Em Despesas, ela registra o botijão de gás da cozinha, de cento e vinte reais, pago em dinheiro. "
        "A despesa entra como pendente: ela só afeta o resultado e o caixa quando é marcada como paga. "
        "Ao confirmar o pagamento, o valor aparece nas despesas pagas do período e sai do caixa aberto.",
        [("nova-despesa", 2.0), ("descricao", 2.4), ("valor", 2.0), ("forma", 2.0),
         ("despesa-pendente", 3.2), ("marcar-paga", 2.0), ("dialogo-paga", 2.2), ("total-pago", 4.0)]),
    rec("c10-ifood", "menu-integracoes-0.4", "end", "Importação do relatório do iFood",
        "Em Integrações, a loja importa o relatório exportado do portal do iFood. Neste exemplo o arquivo é "
        "sintético. O sistema analisa a planilha e mostra uma prévia: pedidos lidos, concluídos, cancelados, "
        "valores e taxas, e a validação de cada linha. Só depois da confirmação os pedidos são gravados. "
        "A importação não movimenta o caixa nem o estoque.",
        [("cards-integracoes", 2.5), ("area-arquivo", 2.4), ("analisar", 2.0), ("totais", 5.0),
         ("linhas", 3.0), ("confirmar", 2.4), ("importado", 3.0)]),
    rec("c11-fechar-caixa", "menu-caixa-0.4", "end", "Fechamento do caixa com conferência",
        "No fim do dia, o operador abre o resumo do caixa: saldo inicial, vendas, despesas e estornos em dinheiro, "
        "e o saldo esperado. Ao fechar, ele informa o valor contado. Como faltaram dois reais, o sistema calcula "
        "a diferença e exige uma justificativa. Depois da confirmação, o caixa fica fechado para novas vendas, "
        "com a diferença registrada.",
        [("resumo", 3.5), ("fechar-link", 2.0), ("esperado", 2.6), ("contado", 2.4),
         ("diferenca", 2.4), ("justificativa", 3.0), ("dialogo-fechar", 2.0), ("fechado", 3.5)]),
    rec("c12-dashboard", "cards-1.6", "end", "Painel financeiro do dia",
        "De volta ao painel, a administradora vê o faturamento do dia com o balcão e o iFood, as despesas pagas, "
        "as taxas e o resultado estimado. O painel avisa quando o resultado está incompleto, por exemplo pelo custo "
        "das mercadorias dos pedidos importados. Abaixo, ficam as vendas por origem, as vendas recentes e a "
        "composição do resultado.",
        [("cards", 4.5), ("origem", 3.5), ("recentes", 4.5)]),
    rec("c13-acesso-operador", "menu-operador-1.4", "end", "Controle de acesso por perfil",
        "As permissões também valem na API. Se o operador tentar abrir o painel financeiro pelo endereço, "
        "o sistema mostra acesso não permitido.",
        [("menu-operador", 3.0), ("acesso-negado", 3.5)]),
    diagram("Como o sistema foi construído",
            "Por trás das telas, o front em Angular conversa com uma API Spring Boot. O login usa tokens JWT e "
            "perfis. Os dados ficam no PostgreSQL, versionado com Flyway, e vendas, compras e estornos usam "
            "transações com bloqueio do estoque e do caixa, para evitar venda acima do disponível.",
            ["Front Angular 20 com Angular Material", "API Spring Boot com JWT e perfis",
             "PostgreSQL versionado com Flyway", "Transações com bloqueio de estoque e caixa"],
            "Fonte: README do projeto, do front e do back"),
]

loja = [
    diagram("O dia na loja com o sistema",
            "Este vídeo mostra a rotina da loja no sistema DaFabi: abrir o caixa, vender no balcão, receber "
            "mercadoria, lançar despesas e fechar o caixa no fim do dia.",
            ["Abrir o caixa", "Vender no balcão", "Receber mercadoria e lançar despesas", "Fechar o caixa"],
            NOTE),
    rec("c06-abrir-caixa", "card-operador-1.5", "end", "Abrir o caixa",
        "Toque em Operador de caixa, ou entre com seu usuário. O PDV avisa que o caixa está fechado. "
        "Clique em Abrir caixa, conte o dinheiro da gaveta e digite o saldo inicial. Confirme. "
        "O resumo mostra o caixa aberto.",
        [("card-operador", 2.0), ("caixa-fechado", 2.6), ("abrir-link", 2.0), ("saldo-inicial", 2.2),
         ("confirmar", 2.0), ("resumo-caixa", 3.5)]),
    rec("c07-venda", "codigo-barras-1.8", "nova-venda-0.3", "Vender no balcão",
        "Leia o código de barras ou toque no produto. Use o mais e o menos para a quantidade. "
        "Confira o total e clique em Continuar para pagamento. Em dinheiro, digite o valor recebido "
        "ou use um atalho. O troco aparece na tela. Confirme, e devolva o troco indicado.",
        [("codigo-barras", 2.6), ("card-marmita", 1.8), ("mais-refri", 1.6),
         ("continuar", 1.8), ("dinheiro", 1.8), ("troco", 2.4), ("sucesso", 4.5)]),
    rec("c07-venda", "venda2-farofa-0.9", "end", "Venda rápida com valor exato",
        "Quando o cliente paga o valor certo, use o botão Valor exato. Para Pix e cartão, só escolha a forma "
        "de pagamento e confirme.",
        [("venda2-farofa", 1.8), ("venda2-sucesso", 3.0)]),
    rec("c04-compra", "menu-compras-0.3", "compra-na-lista+0.3", "Receber frango do fornecedor",
        "Na chegada da mercadoria, a Fabiana abre Compras e Nova compra. Escolha o fornecedor e o produto, "
        "digite a quantidade e o custo. Para frango e outros perecíveis, digite o lote e a validade da etiqueta. "
        "Registre: o estoque já fica disponível para venda.",
        [("fornecedor", 2.4), ("produto", 2.4), ("quantidade", 1.6), ("lote", 2.0), ("validade", 2.0),
         ("registrar", 2.0)]),
    rec("c05-estoque", "pereciveis-2.2", "end", "Conferir lotes e validade",
        "Em Estoque e Perecíveis, confira os lotes e as validades. Se aparecer o aviso amarelo de saldo sem lote, "
        "esse saldo não pode ser vendido: faça a contagem e use Vincular saldo.",
        [("pereciveis", 2.0), ("alerta-sem-lote", 3.5), ("lote", 4.0)]),
    rec("c09-despesa", "menu-despesas-0.4", "end", "Lançar uma despesa",
        "Em Despesas, clique em Nova despesa. Escreva a descrição, a categoria, o valor e a forma de pagamento. "
        "A despesa fica pendente. Quando pagar, clique em Marcar como paga e confirme. "
        "Só então ela conta no resultado e, se foi em dinheiro, sai do caixa.",
        [("nova-despesa", 2.0), ("descricao", 2.4), ("valor", 2.0), ("forma", 2.0),
         ("despesa-pendente", 3.2), ("marcar-paga", 2.0), ("total-pago", 4.0)]),
    rec("c08-estorno", "menu-estornos-0.3", "end", "Desfazer uma venda com estorno",
        "Se o cliente desistir, só a administradora faz o estorno. Em Estornos, clique em Novo estorno, "
        "digite o número da venda e busque. Confira os itens e escreva o motivo. "
        "Confirme: o dinheiro sai do caixa e os produtos voltam ao estoque.",
        [("novo-estorno", 2.0), ("numero", 2.0), ("venda-encontrada", 3.0), ("motivo", 3.0),
         ("aviso-reversao", 2.8), ("estorno-concluido", 4.0)]),
    rec("c11-fechar-caixa", "menu-caixa-0.4", "end", "Fechar o caixa",
        "No fim do dia, abra Caixa e veja o saldo esperado. Clique em Fechar caixa, conte o dinheiro e digite "
        "o valor contado. Se houver diferença, escreva a justificativa. Confirme o fechamento: o caixa não aceita "
        "mais vendas até ser aberto de novo.",
        [("resumo", 3.5), ("fechar-link", 2.0), ("esperado", 2.6), ("contado", 2.4),
         ("justificativa", 3.0), ("dialogo-fechar", 2.0), ("fechado", 3.5)]),
    rec("c12-dashboard", "cards-1.6", "end", "Ver o resultado do dia",
        "No Dashboard, a Fabiana acompanha o faturamento, as despesas pagas, as taxas do iFood e da 99Food "
        "e o resultado estimado. Use Hoje, Últimos sete dias ou Este mês para trocar o período.",
        [("cards", 4.5), ("origem", 3.5)]),
    diagram("Cuidados que evitam erro no caixa",
            "Para fechar o dia sem surpresas: confira lote e validade na entrada do frango, marque a despesa como "
            "paga quando pagar, justifique qualquer diferença no fechamento e deixe o estorno com a administradora.",
            ["Lote e validade na entrada do frango", "Despesa só conta quando marcada como paga",
             "Diferença no fechamento exige justificativa", "Estorno é feito pela administradora"],
            NOTE),
]

for name, title, scenes in (("professores", "demonstracao-completa-dafabi", professores),
                            ("loja", "rotina-da-loja-dafabi", loja)):
    plan = {"product": "DaFabi Frangos", "videos": [{"id": title, "scenes": scenes, "interface": interface(scenes)}]}
    (HERE / f"roteiro-{name}.json").write_text(json.dumps(plan, ensure_ascii=False, indent=2), encoding="utf-8")
    print(name, len(scenes), "cenas")
