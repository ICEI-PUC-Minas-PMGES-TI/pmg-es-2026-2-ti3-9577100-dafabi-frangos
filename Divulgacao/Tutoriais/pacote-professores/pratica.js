'use strict';
(() => {
  const data = JSON.parse(document.getElementById('practice-data').textContent);
  const host = document.getElementById('practice');
  function node(tag, text, className) {
    const item = document.createElement(tag);
    if (text !== undefined) item.textContent = text;
    if (className) item.className = className;
    return item;
  }
  if (data.example) {
    const api = globalThis.ExercicioShowcase;
    let state = api.create(data.example);
    const example = node('section', undefined, 'example');
    example.append(node('h2', data.example.title), node('p', data.example.description), node('p', 'Exercício ilustrativo; não reproduz o motor de cálculo nem envia pedido. Todos os dados são sintéticos e ficam nesta página.', 'notice'));
    const step1 = node('section', undefined, 'chapter');
    step1.append(node('h3', '1. Confira os dados'), node('p', 'Produto fictício: ' + data.example.product), node('p', 'Múltiplo: ' + data.example.pack + ' unidades · Sugestão: ' + data.example.suggested_units + ' unidades'));
    const review = node('button', 'Conferi os dados'); review.type = 'button'; step1.append(review);
    const step2 = node('section', undefined, 'chapter'); step2.append(node('h3', '2. Edite a quantidade e explique o motivo'));
    const qtyLabel = node('label', 'Quantidade em unidades'); qtyLabel.htmlFor = 'example-quantity';
    const quantity = node('input'); quantity.id = 'example-quantity'; quantity.type = 'number'; quantity.min = '0'; quantity.step = String(data.example.pack); quantity.value = state.quantity;
    const reasonLabel = node('label', 'Motivo da revisão'); reasonLabel.htmlFor = 'example-reason';
    const reason = node('textarea'); reason.id = 'example-reason'; reason.rows = 3;
    step2.append(qtyLabel, quantity, reasonLabel, reason);
    const step3 = node('section', undefined, 'chapter'); step3.append(node('h3', '3. Salve a revisão no exercício'));
    const save = node('button', 'Salvar revisão fictícia'); save.type = 'button'; const saved = node('p'); step3.append(save, saved);
    const step4 = node('section', undefined, 'chapter'); step4.append(node('h3', '4. Aprove a simulação'));
    const approve = node('button', 'Aprovar simulação'); approve.type = 'button'; const status = node('p'); status.setAttribute('role', 'status');
    const error = node('p', '', 'feedback'); error.setAttribute('role', 'alert'); error.hidden = true;
    step4.append(approve, status, error);
    const reset = node('button', 'Recomeçar somente este exercício'); reset.type = 'button';
    const download = node('button', 'Exportar relato fictício'); download.type = 'button';
    function refresh() {
      review.disabled = state.reviewed; quantity.disabled = !state.reviewed; reason.disabled = !state.reviewed; save.disabled = !state.reviewed; approve.disabled = !state.saved || state.approved;
      saved.textContent = state.saved ? 'Revisão salva nesta página. Qualquer nova edição exige salvar novamente.' : 'A revisão ainda precisa ser salva.';
      status.textContent = state.approved ? 'Simulação aprovada. Nenhum pedido foi criado ou enviado.' : 'A simulação ainda não foi aprovada.';
      error.hidden = !state.error; error.textContent = state.error;
    }
    function act(action) { state = api.transition(state, action); refresh(); }
    review.addEventListener('click', () => act({ type: 'review' }));
    quantity.addEventListener('input', () => act({ type: 'edit', quantity: quantity.value.trim() === '' ? NaN : Number(quantity.value) }));
    reason.addEventListener('input', () => act({ type: 'edit', reason: reason.value }));
    save.addEventListener('click', () => act({ type: 'save' })); approve.addEventListener('click', () => act({ type: 'approve' }));
    reset.addEventListener('click', () => { act({ type: 'reset' }); quantity.value = state.quantity; reason.value = ''; });
    download.addEventListener('click', () => {
      const payload = { type: 'exercicio-sintetico', product: data.product, version: data.version, example_product: state.config.product, quantity: Number.isFinite(state.quantity) ? state.quantity : null, reason: state.reason, saved: state.saved, approved: state.approved, note: 'Relato local de exercício. Nenhum pedido enviado.' };
      const url = URL.createObjectURL(new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' }));
      const anchor = node('a'); anchor.href = url; anchor.download = 'exercicio-sintetico.json'; document.body.append(anchor); anchor.click(); anchor.remove(); setTimeout(() => URL.revokeObjectURL(url), 1000);
    });
    example.append(step1, step2, step3, step4, reset, download); host.append(example); refresh();
  }
  const key = 'showcase-practice:' + data.product + ':' + data.version;
  let answers = {};
  try { const saved = JSON.parse(localStorage.getItem(key) || '{}'); if (saved && typeof saved === 'object' && !Array.isArray(saved)) answers = saved; } catch (_) {}
  const score = document.createElement('p'); score.className = 'score'; score.setAttribute('role', 'status'); host.append(score);
  function update() { score.textContent = data.questions.filter(q => Object.hasOwn(answers, q.id) && answers[q.id] === q.answer).length + ' de ' + data.questions.length + ' respostas corretas'; }
  data.questions.forEach(question => {
    const box = document.createElement('section'); box.className = 'chapter';
    const field = document.createElement('fieldset'); const legend = document.createElement('legend'); legend.textContent = question.prompt; field.append(legend);
    const feedback = document.createElement('p'); feedback.className = 'feedback'; feedback.setAttribute('role', 'status'); feedback.hidden = true;
    function evaluate(index, save) {
      answers[question.id] = index; feedback.hidden = false;
      feedback.classList.toggle('correct', index === question.answer);
      feedback.textContent = (index === question.answer ? 'Correto. ' : 'Revise esta escolha. ') + question.explanation;
      if (save) { try { localStorage.setItem(key, JSON.stringify(answers)); } catch (_) {} }
      update();
    }
    question.options.forEach((text, index) => {
      const label = document.createElement('label'); label.className = 'option';
      const radio = document.createElement('input'); radio.type = 'radio'; radio.name = question.id; radio.value = index; radio.checked = answers[question.id] === index;
      radio.addEventListener('change', () => evaluate(index, true));
      const span = document.createElement('span'); span.textContent = text; label.append(radio, span); field.append(label);
    });
    box.append(field, feedback); host.append(box);
    if (Object.hasOwn(answers, question.id)) evaluate(answers[question.id], false);
  });
  update();
})();
