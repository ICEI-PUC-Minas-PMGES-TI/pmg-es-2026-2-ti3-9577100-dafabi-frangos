import { createReadStream, existsSync } from 'node:fs';
import { createServer } from 'node:http';
import { extname, join, normalize } from 'node:path';

const port = Number(process.env.PORT ?? 8080);
const root = join(process.cwd(), 'dist');
const contentTypes = {
  '.css': 'text/css; charset=utf-8',
  '.html': 'text/html; charset=utf-8',
  '.ico': 'image/x-icon',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.woff2': 'font/woff2'
};

createServer((request, response) => {
  const pathname = decodeURIComponent(new URL(request.url, `http://${request.headers.host}`).pathname);
  const candidate = normalize(join(root, pathname === '/' ? 'index.html' : pathname));
  const file = candidate.startsWith(root) && existsSync(candidate) ? candidate : join(root, 'index.html');

  response.writeHead(200, {
    'Content-Type': contentTypes[extname(file)] ?? 'application/octet-stream',
    'Cache-Control': file.endsWith('index.html') ? 'no-cache' : 'public, max-age=31536000, immutable'
  });
  createReadStream(file).pipe(response);
}).listen(port, '0.0.0.0', () => console.log(`DaFabi web listening on ${port}`));
