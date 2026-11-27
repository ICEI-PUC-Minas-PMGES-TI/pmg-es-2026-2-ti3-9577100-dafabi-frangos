'use strict';
(() => {
  const captions = JSON.parse(document.getElementById('legendas').textContent);
  document.querySelectorAll('video track').forEach((track, index) => {
    if (captions[index]) track.src = URL.createObjectURL(new Blob([captions[index]], { type: 'text/vtt' }));
  });
})();
