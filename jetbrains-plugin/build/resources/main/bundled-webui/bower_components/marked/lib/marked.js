(function(global) {
  if (typeof global.marked === 'function') {
    return;
  }

  function escapeHtml(input) {
    return input
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function render(markdown) {
    if (markdown == null) {
      return '';
    }

    var source = String(markdown).replace(/\r\n?/g, '\n');
    var blocks = source.split(/\n{2,}/);
    var html = blocks
      .map(function(block) {
        return '<p>' + escapeHtml(block).replace(/\n/g, '<br/>') + '</p>';
      })
      .join('');

    return html;
  }

  render.parse = render;
  global.marked = render;
})(window);
