/**
 * Copia o texto de cada cabeçalho (`th`) das tabelas `.data-table` para o atributo
 * `data-label` das células. Em telas estreitas o CSS global mostra cada linha como um
 * cartão e usa esse atributo como rótulo do valor (ver `styles.scss`).
 *
 * Um único observador cobre todas as telas, sem exigir mudança em cada componente.
 */
export function startResponsiveTables(root: HTMLElement = document.body): () => void {
  let scheduled = false;
  const label = () => {
    scheduled = false;
    root.querySelectorAll<HTMLTableElement>('table.data-table').forEach(table => {
      const headers = Array.from(table.querySelectorAll('thead th')).map(th => (th.textContent ?? '').trim());
      table.querySelectorAll('tbody tr').forEach(row => {
        Array.from(row.children).forEach((cell, index) => {
          const text = headers[index] ?? '';
          if (cell.getAttribute('data-label') !== text) cell.setAttribute('data-label', text);
        });
      });
    });
  };
  const observer = new MutationObserver(() => {
    if (scheduled) return;
    scheduled = true;
    requestAnimationFrame(label);
  });
  observer.observe(root, { childList: true, subtree: true });
  label();
  return () => observer.disconnect();
}
