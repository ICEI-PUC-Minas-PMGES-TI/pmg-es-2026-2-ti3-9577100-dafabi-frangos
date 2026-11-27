import zipfile, sys
from xml.sax.saxutils import escape
H=["ID COMPLETO DO PEDIDO","ID DA LOJA","NOME DA LOJA","DATA E HORA DO PEDIDO","STATUS FINAL DO PEDIDO","VALOR DOS ITENS (R$)","TOTAL PAGO PELO CLIENTE (R$)","TAXAS E COMISSÕES (R$)","VALOR LÍQUIDO (R$)"]
rows=[["DEMO-IF-0001","0000","DaFabi Frangos (demonstração)","27/11/2026 11:05:00","CONCLUIDO","89,80","89,80","-10,78","79,02"],
      ["DEMO-IF-0002","0000","DaFabi Frangos (demonstração)","27/11/2026 11:40:00","CONCLUIDO","56,90","56,90","-6,83","50,07"],
      ["DEMO-IF-0003","0000","DaFabi Frangos (demonstração)","27/11/2026 12:10:00","CANCELADO","44,90","0,00","0,00","0,00"]]
def col(i): return chr(65+i)
def row(r,vals): return f'<row r="{r}">'+''.join(f'<c r="{col(i)}{r}" t="inlineStr"><is><t>{escape(v)}</t></is></c>' for i,v in enumerate(vals))+'</row>'
sheet='<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>'+row(1,H)+''.join(row(i+2,r) for i,r in enumerate(rows))+'</sheetData></worksheet>'
z=zipfile.ZipFile(sys.argv[1],'w',zipfile.ZIP_DEFLATED)
z.writestr('[Content_Types].xml','<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>')
z.writestr('_rels/.rels','<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>')
z.writestr('xl/workbook.xml','<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Pedidos" sheetId="1" r:id="rId1"/></sheets></workbook>')
z.writestr('xl/_rels/workbook.xml.rels','<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>')
z.writestr('xl/worksheets/sheet1.xml',sheet); z.close()
