// Check Vite's real HTTP forwarding against a local disposable backend.
import assert from 'node:assert/strict'
import { createServer } from 'node:http'
import { spawn } from 'node:child_process'

const requests = []
const backend = createServer(async (request, response) => {
  let body = ''
  for await (const chunk of request) body += chunk
  requests.push({ url: request.url, method: request.method, body })
  if (request.url.startsWith('/uploads/')) {
    response.writeHead(200, { 'Content-Type': 'image/png' })
    response.end(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))
  } else {
    response.writeHead(200, { 'Content-Type': 'application/json' })
    response.end(JSON.stringify({ path: request.url, method: request.method, body }))
  }
})
await new Promise(resolve => backend.listen(0, '127.0.0.1', resolve))
const target = `http://127.0.0.1:${backend.address().port}`
const port = Number(process.env.PROXY_TEST_PORT || 5180)
const origin = `http://127.0.0.1:${port}`
const vite = spawn(process.execPath, ['node_modules/vite/bin/vite.js', '--host', '127.0.0.1', '--port', String(port), '--strictPort'], {
  env: { ...process.env, MERLETTE_API_TARGET: target }, stdio: ['ignore', 'pipe', 'pipe'], windowsHide: true,
})
let output = ''
vite.stdout.on('data', data => { output += data })
vite.stderr.on('data', data => { output += data })
try {
  const start = Date.now()
  let ready = false
  while (Date.now() - start < 15000) {
    if (vite.exitCode !== null) throw new Error(`Vite stopped: ${output}`)
    try { const response = await fetch(`${origin}/api/health`, { signal: AbortSignal.timeout(1000) }); if (response.ok && (await response.json()).path === '/api/health') { ready = true; break } } catch {}
    await new Promise(resolve => setTimeout(resolve, 100))
  }
  assert.ok(ready, `Vite did not start: ${output}`)
  const feed = await (await fetch(`${origin}/api/observations?category=oiseaux&limit=12`)).json()
  assert.equal(feed.path, '/api/observations?category=oiseaux&limit=12')
  const registration = await (await fetch(`${origin}/api/auth/register`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: 'proxy-test' }) })).json()
  assert.equal(registration.method, 'POST')
  assert.deepEqual(JSON.parse(registration.body), { username: 'proxy-test' })
  const photo = await fetch(`${origin}/uploads/test.png`)
  assert.equal(photo.headers.get('content-type'), 'image/png')
  assert.equal((await photo.arrayBuffer()).byteLength, 8)
  assert.ok(requests.every(request => request.url.startsWith('/api/') || request.url.startsWith('/uploads/')))
  console.log('PASS real Vite proxy: API, query parameters, POST body and uploaded images')
  console.log('No request was sent to the shared backend.')
} finally {
  vite.kill()
  backend.closeAllConnections()
  await new Promise(resolve => backend.close(resolve))
}
