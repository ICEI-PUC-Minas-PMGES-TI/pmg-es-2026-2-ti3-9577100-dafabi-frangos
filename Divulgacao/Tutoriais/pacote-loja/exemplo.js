'use strict';
(() => {
  function create(config) {
    return { config, reviewed: false, quantity: config.suggested_units, reason: '', saved: false, approved: false, error: '' };
  }
  function validate(state) {
    if (!state.reviewed) return 'Confira os dados antes de salvar a revisão.';
    if (!Number.isInteger(state.quantity) || state.quantity < 0 || state.quantity % state.config.pack !== 0) return 'Informe zero ou uma quantidade inteira compatível com o múltiplo indicado.';
    if (state.quantity !== state.config.suggested_units && !state.reason.trim()) return 'Explique o motivo da quantidade diferente da sugestão.';
    return '';
  }
  function transition(state, action) {
    const next = { ...state, error: '' };
    if (action.type === 'reset') return create(state.config);
    if (action.type === 'review') next.reviewed = true;
    else if (action.type === 'edit') {
      if (Object.hasOwn(action, 'quantity')) next.quantity = action.quantity;
      if (Object.hasOwn(action, 'reason')) next.reason = action.reason;
      next.saved = false; next.approved = false;
    } else if (action.type === 'save') {
      next.error = validate(next); next.saved = !next.error; next.approved = false;
    } else if (action.type === 'approve') {
      next.error = next.saved ? validate(next) : 'Salve uma revisão válida antes de aprovar a simulação.';
      next.approved = !next.error;
    }
    return next;
  }
  const api = { create, validate, transition };
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else globalThis.ExercicioShowcase = api;
})();
