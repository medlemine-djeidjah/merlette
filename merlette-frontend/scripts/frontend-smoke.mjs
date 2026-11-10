// Browser integration checks with isolated HTTP fixtures. No production API writes.
// Start the QA Vite server with all module flags enabled and Chrome with CDP on 9223.
import assert from 'node:assert/strict'
import { mkdir, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const origin = process.env.PREVIEW_URL || 'http://127.0.0.1:5178'
const debugUrl = process.env.CHROME_DEBUG_URL || 'http://127.0.0.1:9223'
const community = process.env.COMMUNITY_ENABLED !== 'false'
const artifacts = `test-artifacts/${community ? 'complete' : 'core'}`
const target = await (await fetch(`${debugUrl}/json/new?${encodeURIComponent('about:blank')}`, { method: 'PUT' })).json()
const ws = new WebSocket(target.webSocketDebuggerUrl)
await new Promise((resolve, reject) => { ws.onopen = resolve; ws.onerror = reject })
let sequence = 0
const pending = new Map()
const exceptions = []
const requests = []
function call(method, params = {}) {
  return new Promise((resolve, reject) => {
    const id = ++sequence
    pending.set(id, { resolve, reject })
    ws.send(JSON.stringify({ id, method, params }))
  })
}
ws.onmessage = event => {
  const message = JSON.parse(event.data)
  if (message.id) {
    const task = pending.get(message.id)
    if (!task) return
    pending.delete(message.id)
    if (message.error) task.reject(new Error(JSON.stringify(message.error))); else task.resolve(message.result)
  } else if (message.method === 'Fetch.requestPaused') {
    respond(message.params).catch(error => { exceptions.push(error.message); call('Fetch.failRequest', { requestId: message.params.requestId, errorReason: 'Failed' }).catch(() => {}) })
  } else if (message.method === 'Runtime.exceptionThrown') exceptions.push(message.params.exceptionDetails.exception?.description || message.params.exceptionDetails.text)
}
const categories = [{ id: 1, name: 'Oiseaux', slug: 'oiseaux' }, { id: 2, name: 'Plantes', slug: 'plantes' }, { id: 3, name: 'Insectes', slug: 'insectes' }]
const user = { id: 1, username: 'lea_martin', email: 'lea@example.org', role: 'ADMIN', bio: 'Observer les petits détails et apprendre à reconnaître le vivant.', avatarUrl: null, createdAt: '2026-10-13T08:00:00Z' }
const species = { id: 1, commonName: 'Mésange bleue', scientificName: 'Cyanistes caeruleus', category: categories[0] }
const observations = Array.from({ length: 6 }, (_, i) => ({
  id: i + 1, title: ['Une visite au jardin', 'Petite fleur des champs', 'Sur une feuille', 'Au bord du sentier', 'La pause du matin', 'Un coin de verdure'][i],
  description: 'Une rencontre au détour du chemin. Quelques détails à observer ensemble.', status: i === 1 ? 'IDENTIFIED' : 'NEEDS_ID',
  category: categories[i % 3], author: { id: 1, username: user.username }, observedAt: '2026-11-09', locationName: ['Le jardin', 'Parc des Cévennes', 'Au bord du sentier'][i % 3],
  coverUrl: `/uploads/test-${i % 3}.svg`, photos: [{ id: i + 1, url: `/uploads/test-${i % 3}.svg`, position: 0 }],
  createdAt: '2026-11-09T10:00:00Z', updatedAt: '2026-11-09T10:00:00Z', proposalCount: 1, commentCount: 1, identifiedSpecies: i === 1 ? species : null,
}))
const comments = [{ id: 1, content: 'Les joues blanches sont un bel indice.', author: { id: 2, username: 'karim' }, createdAt: '2026-11-09T10:00:00Z', updatedAt: '2026-11-09T10:00:00Z', edited: false }]
const notices = [{ id: 1, type: 'NEW_COMMENT', message: 'Karim a commenté votre observation « Une visite au jardin ».', observationId: 1, actor: { id: 2, username: 'karim' }, isRead: false, createdAt: '2026-11-09T10:00:00Z' }]
let voted = false
let failFeed = false
let paginationFixture = false
let multipart = ''
let nextComment = 2
function paginated(data, page = 1, limit = 12) { return { data: data.slice((page - 1) * limit, page * limit), pagination: { page, limit, total: data.length, totalPages: Math.ceil(data.length / limit) } } }
// Match the checked backend DTOs: summaries have a cover, details have photos instead.
function summaryOf(o) { return { id: o.id, title: o.title, status: o.status, category: o.category, author: o.author, coverUrl: o.coverUrl, observedAt: o.observedAt, createdAt: o.createdAt, proposalCount: o.proposalCount, commentCount: o.commentCount, identifiedSpecies: o.identifiedSpecies } }
function detailOf(o) { const { coverUrl: _coverUrl, proposalCount: _proposalCount, ...detail } = o; return { ...detail, proposals: [], myVoteProposalId: null } }
function illustration(index) {
  const bg = ['#cfdbbd', '#eee2c3', '#bfd0b9'][index]
  const drawing = index === 0 ? '<path d="M60 280Q180 230 420 280" stroke="#657950" stroke-width="9" fill="none"/><ellipse cx="245" cy="174" rx="68" ry="51" fill="#738e65"/><circle cx="299" cy="133" r="39" fill="#375d58"/><circle cx="309" cy="131" r="4" fill="#152c28"/><path d="m336 135 28 8-30 7M185 176l-64 48 88-13" fill="#486750"/><path d="m239 215-5 40m24-40 4 36" stroke="#635938" stroke-width="5"/>' : index === 1 ? '<path d="M247 326V170m0 80q-92-2-80-52 63-6 80 52m0 30q73-9 67-51-56 3-67 51" stroke="#69824d" stroke-width="5" fill="#8b9b68"/><g fill="#f8f4df"><ellipse cx="247" cy="131" rx="22" ry="49"/><ellipse cx="247" cy="131" rx="22" ry="49" transform="rotate(60 247 160)"/><ellipse cx="247" cy="131" rx="22" ry="49" transform="rotate(120 247 160)"/><ellipse cx="247" cy="131" rx="22" ry="49" transform="rotate(180 247 160)"/><ellipse cx="247" cy="131" rx="22" ry="49" transform="rotate(240 247 160)"/><ellipse cx="247" cy="131" rx="22" ry="49" transform="rotate(300 247 160)"/></g><circle cx="247" cy="160" r="24" fill="#c59c45"/>' : '<path d="M120 315q100-230 280-240-5 210-280 240" fill="#6c8a57"/><path d="m120 315 245-205m-140 122 5-85m-5 85 95 5" stroke="#c8d6b5" stroke-width="3"/><ellipse cx="272" cy="184" rx="25" ry="32" fill="#a35b3f"/><path d="M272 155v58" stroke="#533b2b" stroke-width="3"/><g fill="#3f3425"><circle cx="259" cy="177" r="4"/><circle cx="285" cy="177" r="4"/><circle cx="259" cy="193" r="4"/><circle cx="285" cy="193" r="4"/></g>'
  return `<svg xmlns="http://www.w3.org/2000/svg" width="500" height="360"><rect width="500" height="360" fill="${bg}"/><circle cx="407" cy="60" r="110" fill="#ffffff" opacity=".15"/>${drawing}</svg>`
}
async function respond({ requestId, request }) {
  const url = new URL(request.url)
  const proxied = url.origin === origin && (url.pathname.startsWith('/api/') || url.pathname.startsWith('/uploads/'))
  if (url.port !== '8080' && !proxied) { await call('Fetch.continueRequest', { requestId }); return }
  requests.push({ path: url.pathname, method: request.method, query: url.search })
  let status = 200; let body = {}; let contentType = 'application/json'
  let input = {}
  if (request.headers['Content-Type']?.includes('application/json') || request.headers['content-type']?.includes('application/json')) { try { input = JSON.parse(request.postData || '{}') } catch {} }
  const path = url.pathname.replace(/^\/api/, '')
  const method = request.method
  if (method === 'OPTIONS') body = ''
  else if (url.pathname.startsWith('/uploads/')) { body = illustration(Number(url.pathname.match(/test-(\d)/)?.[1] || 0)); contentType = 'image/svg+xml' }
  else if (path === '/auth/me') body = user
  else if (path === '/auth/register' || path === '/auth/login') { if (input.username === 'already_used') { status = 409; body = { error: { code: 'CONFLICT', message: 'Cet email ou ce pseudo est déjà utilisé.', details: [] } } } else if (input.password === 'incorrect') { status = 401; body = { error: { code: 'INVALID_CREDENTIALS', message: 'Email ou mot de passe incorrect.', details: [] } } } else { status = path.endsWith('register') ? 201 : 200; body = { token: 'browser-test-token', user } } }
  else if (path === '/categories') { if (method === 'POST') { const category = { id: categories.length + 1, ...input }; categories.push(category); body = category; status = 201 } else body = categories }
  else if (path === '/species') body = method === 'POST' ? { id: 9, ...input } : [species]
  else if (/\/proposals$/.test(path)) { if (method === 'POST') { status = 201; voted = true; body = { proposal: { id: 1, speciesId: 1, votes: 1 }, observationStatus: 'NEEDS_ID' } } else body = { totalVotes: voted ? 2 : 1, consensus: { reached: false, proposalId: null, speciesId: null }, proposals: [{ id: 1, species, proposedBy: { id: 2, username: 'karim' }, votes: voted ? 2 : 1, percent: 100, voters: [{ id: 2, username: 'karim' }, ...(voted ? [{ id: 1, username: user.username }] : [])], createdAt: '2026-11-09T10:00:00Z' }] } }
  else if (/\/vote$/.test(path)) { voted = method === 'PUT'; body = { myVoteProposalId: voted ? 1 : null, observationStatus: 'NEEDS_ID', identifiedSpecies: null } }
  else if (/\/comments$/.test(path)) { if (method === 'POST') { const comment = { id: nextComment++, content: input.content, author: user, createdAt: '2026-11-10T10:00:00Z', updatedAt: '2026-11-10T10:00:00Z', edited: false }; comments.push(comment); body = comment; status = 201 } else body = comments }
  else if (/^\/comments\//.test(path)) { const comment = comments.find(c => c.id === Number(path.split('/').at(-1))); if (method === 'DELETE') { comments.splice(comments.indexOf(comment), 1); status = 204 } else { Object.assign(comment, input, { edited: true }); body = comment } }
  else if (path === '/notifications/unread-count') body = { count: notices.filter(n => !n.isRead).length }
  else if (path === '/notifications/read-all') { notices.forEach(n => { n.isRead = true }); body = { updated: notices.length } }
  else if (/^\/notifications\/\d+\/read$/.test(path)) { notices[0].isRead = true; body = notices[0] }
  else if (path === '/notifications') body = paginated(notices.filter(n => url.searchParams.get('unread') !== 'true' || !n.isRead))
  else if (path === '/users/me/conversations') body = { data: observations.slice(0, 3).map(o => ({ observationId: o.id, title: o.title, coverUrl: o.coverUrl, status: o.status, myRole: ['AUTHOR'], lastActivity: o.createdAt })) }
  else if (path === '/users/me' && method === 'PATCH') { Object.assign(user, input); body = user }
  else if (/^\/users\/\d+$/.test(path)) body = { ...user, ...(path.endsWith('/2') && { id: 2, username: 'karim' }), stats: { observations: observations.length, proposals: 1, identifiedObservations: 1 } }
  else if (path === '/observations' && method === 'POST') { multipart = request.postData || ''; status = 201; const created = { ...observations[0], id: 77, title: 'Une observation de test' }; observations.unshift(created); body = detailOf(created) }
  else if (/^\/observations\/\d+$/.test(path)) { const observation = observations.find(o => o.id === Number(path.split('/').at(-1))); if (!observation) { status = 404; body = { error: { code: 'NOT_FOUND', message: 'Observation introuvable.', details: [] } } } else if (method === 'PATCH') { Object.assign(observation, input); body = detailOf(observation) } else if (method === 'DELETE') { observations.splice(observations.indexOf(observation), 1); status = 204 } else body = detailOf(observation) }
  else if (path === '/observations') {
    if (failFeed) { status = 503; body = { error: { code: 'UNAVAILABLE', message: 'Le carnet est temporairement indisponible.', details: [] } } }
    else { let rows = paginationFixture ? [...observations, ...observations.map(o => ({ ...o, id: o.id + 100 })), ...observations.map(o => ({ ...o, id: o.id + 200 }))] : observations; if (url.searchParams.has('category')) rows = rows.filter(o => o.category.slug === url.searchParams.get('category')); if (url.searchParams.has('status')) rows = rows.filter(o => o.status === url.searchParams.get('status')); body = paginated(rows.map(summaryOf), Number(url.searchParams.get('page') || 1), Number(url.searchParams.get('limit') || 12)) }
  } else { status = 404; body = { error: { code: 'NOT_FOUND', message: 'Endpoint de test non prévu.', details: [] } } }
  await call('Fetch.fulfillRequest', { requestId, responseCode: status, responseHeaders: [{ name: 'Content-Type', value: contentType }, { name: 'Access-Control-Allow-Origin', value: origin }, { name: 'Access-Control-Allow-Methods', value: 'GET,POST,PATCH,PUT,DELETE,OPTIONS' }, { name: 'Access-Control-Allow-Headers', value: 'authorization,content-type' }], body: Buffer.from(status === 204 ? '' : typeof body === 'string' ? body : JSON.stringify(body)).toString('base64') })
}
async function evaluate(expression) {
  const result = await call('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description || result.exceptionDetails.text)
  return result.result.value
}
async function wait(expression, label = expression) {
  const start = Date.now()
  while (Date.now() - start < 15000) { if (await evaluate(expression)) return; await new Promise(resolve => setTimeout(resolve, 100)) }
  throw new Error(`Timeout: ${label}`)
}
async function navigate(path, expected) {
  await call('Page.navigate', { url: origin + path })
  await wait(`document.readyState === 'complete' && !!document.querySelector('h1') && document.body.innerText.includes(${JSON.stringify(expected)})`, `navigate ${path}: ${expected}`)
}
async function click(text) {
  await evaluate(`(() => { const element = [...document.querySelectorAll('button,a')].find(e => e.textContent.trim() === ${JSON.stringify(text)}); if (!element) throw new Error('Missing control: '+${JSON.stringify(text)}); element.click(); })()`)
}
async function fill(selector, value) {
  await evaluate(`(() => { const element=document.querySelector('main')?.querySelector(${JSON.stringify(selector)}); if(!element) throw new Error('Missing field'); const proto=element.tagName==='TEXTAREA'?HTMLTextAreaElement.prototype:element.tagName==='SELECT'?HTMLSelectElement.prototype:HTMLInputElement.prototype; Object.getOwnPropertyDescriptor(proto,'value').set.call(element,${JSON.stringify(value)});element.dispatchEvent(new Event(element.tagName==='SELECT'?'change':'input',{bubbles:true})); })()`)
}
async function width(value) { await call('Emulation.setDeviceMetricsOverride', { width: value, height: 1000, deviceScaleFactor: 1, mobile: value < 760 }); await new Promise(resolve => setTimeout(resolve, 150)) }
async function noOverflow() {
  const dimensions = await evaluate('({ document: document.documentElement.scrollWidth, viewport: window.innerWidth })')
  assert.ok(dimensions.document <= dimensions.viewport + 1, `Horizontal overflow ${JSON.stringify(dimensions)}`)
}
async function screenshot(name) {
  await mkdir(artifacts, { recursive: true })
  const { data } = await call('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true })
  await writeFile(`${artifacts}/${name}.png`, Buffer.from(data, 'base64'))
}
async function check(name, fn) { await fn(); console.log(`PASS ${name}`) }
try {
  await call('Page.enable'); await call('Runtime.enable')
  await call('Fetch.enable', { patterns: [{ urlPattern: '*localhost:8080/*' }, { urlPattern: `${origin}/api/*` }, { urlPattern: `${origin}/uploads/*` }] })
  await call('Page.navigate', { url: origin })
  await wait(`location.origin === ${JSON.stringify(origin)} && document.readyState === 'complete'`)
  await evaluate('localStorage.clear(); sessionStorage.clear()')
  await width(1440)
  await check('Home, live cards, category and status filters', async () => {
    await navigate('/', 'Le vivant est partout.')
    await wait('document.querySelectorAll(".observation-card").length === 6')
    await screenshot('home-desktop'); await noOverflow()
    await click('Oiseaux')
    await wait('location.search.includes("category=oiseaux") && document.querySelectorAll(".observation-card").length === 2')
    await fill('.filter-selects select', 'IDENTIFIED')
    await wait('document.body.innerText.includes("La découverte se fait attendre")')
    await click('Effacer les filtres'); await wait('document.querySelectorAll(".observation-card").length === 6')
    await width(390); await noOverflow(); await screenshot('home-mobile')
    await evaluate('document.querySelector(".mobile-menu-button").click()'); await wait('!!document.querySelector("#mobile-menu")')
  })
  await check('Protected route and registration form', async () => {
    await navigate('/observations/new', 'Ravi de vous revoir.')
    assert.equal(await evaluate('location.pathname'), '/login')
    await navigate('/register', 'Cultivons la curiosité.')
    await noOverflow(); await screenshot('registration-mobile')
    await fill('[name="username"]', 'x'); await fill('[name="password"]', 'short')
    assert.equal(await evaluate('document.querySelector(".auth-form-panel form").checkValidity()'), false, '#54 invalid fields rejected')
    await fill('[name="username"]', 'already_used'); await fill('[name="email"]', 'lea@example.org'); await fill('[name="password"]', 'Secret123!')
    await click('Créer mon compte'); await wait('document.querySelector("[role=alert]")?.textContent.includes("déjà utilisé")')
    await fill('[name="username"]', 'lea_martin')
    await click('Créer mon compte'); await wait('!!localStorage.getItem("merlette.token") && location.pathname === "/"')
    assert.equal(await evaluate('localStorage.getItem("merlette.token")'), 'browser-test-token', '#55 automatic session after registration')
  })
  await check('#41 pagination and filter persistence after reloading the URL', async () => {
    paginationFixture = true
    await navigate('/?status=NEEDS_ID', 'Le vivant est partout.')
    await wait('document.querySelectorAll(".observation-card").length === 12')
    await click('Suivant'); await wait('location.search.includes("page=2") && document.querySelectorAll(".observation-card").length === 3')
    assert.ok(await evaluate('location.search.includes("status=NEEDS_ID")'))
    await navigate('/?status=NEEDS_ID&page=2', 'Le vivant est partout.')
    await wait('document.querySelectorAll(".observation-card").length === 3')
    await click('Précédent'); await wait('location.search.includes("page=1") && document.querySelectorAll(".observation-card").length === 12')
    paginationFixture = false
  })
  await check('Detail gallery, vote, species search and comment', async () => {
    await width(1440); await navigate('/observations/1', 'Une visite au jardin')
    await wait(community ? 'document.body.innerText.includes("Mésange bleue")' : 'document.body.innerText.includes("Les propositions d’espèces et les votes arrivent bientôt")'); await screenshot('detail-desktop'); await noOverflow()
    if (community) {
    await click('Voter'); await wait('document.body.innerText.includes("Retirer mon vote")')
    await click('Retirer mon vote'); await wait('!document.body.innerText.includes("Retirer mon vote")')
    await click('Proposer une espèce')
    const beforeSpeciesSearch = requests.filter(request => request.path === '/api/species').length
    await fill('.species-search input', 'm'); await new Promise(resolve => setTimeout(resolve, 450))
    assert.equal(requests.filter(request => request.path === '/api/species').length, beforeSpeciesSearch, '#74 no request below two characters')
    await fill('.species-search input', 'me'); await wait('!!document.querySelector(".species-result")')
    await evaluate('document.querySelector(".species-search input").focus()')
    await call('Input.dispatchKeyEvent', { type: 'keyDown', key: 'ArrowDown', code: 'ArrowDown' })
    await wait('!!document.querySelector(".species-search input").getAttribute("aria-activedescendant")')
    await call('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Enter', code: 'Enter' })
    await wait('!document.querySelector(".species-search")')
    assert.ok(requests.some(request => request.method === 'POST' && request.path === '/api/observations/1/proposals'), '#74 selecting a suggestion sends its species ID')
    await fill('.comment-form textarea', 'Un très joli détail.'); await click('Partager mon commentaire'); await wait('document.querySelector(".comment-form textarea").value === "" && document.body.innerText.includes("Un très joli détail.")')
    await click('Modifier'); await wait('!!document.querySelector(".modal-dialog[open]")'); await fill('.modal-dialog [name="title"]', 'Une visite au jardin, précisée'); await click('Enregistrer les modifications'); await wait('!document.querySelector(".modal-dialog") && document.querySelector("h1").textContent.includes("précisée")')
    }
    await evaluate('document.querySelector("button.gallery-main").click()'); await wait('document.querySelector("dialog.lightbox").open'); await evaluate('document.querySelector("dialog.lightbox .icon-button").click()')
    await width(390); await noOverflow(); await screenshot('detail-mobile')
    await navigate('/observations/2', 'Petite fleur des champs')
    await wait('document.querySelector(".detail-heading section")?.textContent.includes("Mésange bleue")')
    assert.ok(await evaluate('document.querySelector(".detail-heading section").textContent.includes("Cyanistes caeruleus")'), '#70 retained species common and scientific names')
  })
  await check('Publication: file selection, removal and multipart upload', async () => {
    await navigate('/observations/new', 'Une rencontre à partager.')
    const { root } = await call('DOM.getDocument')
    const { nodeId } = await call('DOM.querySelector', { nodeId: root.nodeId, selector: '#photo-input' })
    await call('DOM.setFileInputFiles', { nodeId, files: [resolve('../postman/fixtures/bird.jpg')] })
    await wait('document.querySelectorAll(".upload-preview").length === 1')
    await evaluate('document.querySelector(".upload-preview button").click()'); await wait('document.querySelectorAll(".upload-preview").length === 0')
    await call('DOM.setFileInputFiles', { nodeId, files: [resolve('../postman/fixtures/bird.jpg')] })
    await wait('document.querySelectorAll(".upload-preview").length === 1')
    await fill('[name="title"]', 'Une observation de test'); await fill('[name="categoryId"]', '1')
    await fill('[name="locationName"]', 'Jardin'); await fill('[name="description"]', 'Petit oiseau')
    await noOverflow(); await screenshot('publication-mobile'); await width(1440); await screenshot('publication-desktop')
    await click('Publier l’observation'); await wait('location.pathname === "/observations/77" && document.body.innerText.includes("Votre rencontre rejoint le carnet.")')
    assert.ok(multipart.includes('name="photos"'), 'Photo sent in multipart')
    for (const field of ['categoryId', 'observedAt', 'title', 'locationName', 'description']) assert.ok(multipart.includes(`name="${field}"`), `#64 ${field} included in publication`)
  })
  await check('Owner deletion works independently of editing module', async () => {
    user.role = 'USER'
    await navigate('/observations/77', 'Une observation de test')
    await wait('!!document.querySelector(".profile-menu")')
    await click('Supprimer'); await wait('!!document.querySelector("dialog[open]")')
    await click('Annuler'); await wait('!document.querySelector("dialog[open]")')
    assert.ok(observations.some(o => o.id === 77), 'Cancelling keeps the observation')
    await click('Supprimer'); await wait('!!document.querySelector("dialog[open]")')
    await click('Confirmer la suppression'); await wait('location.pathname === "/me/observations"')
    assert.ok(!observations.some(o => o.id === 77), 'Confirmed deletion removes the observation')
    const otherObservation = observations.find(o => o.id === 2)
    const originalAuthor = otherObservation.author
    otherObservation.author = { id: 2, username: 'karim' }
    await navigate('/observations/2', 'Petite fleur des champs')
    await wait('!!document.querySelector(".profile-menu")')
    assert.equal(await evaluate('!!document.querySelector(".management-actions")'), false, 'A regular user cannot delete another author’s observation')
    otherObservation.author = originalAuthor
    user.role = 'ADMIN'
  })
  await check('Workspace, discussions, notifications and profile', async () => {
    for (const [path, text, name] of [['/me/observations','Mes observations','my-observations'],['/me/conversations','Mes discussions','conversations'],['/notifications','Notifications','notifications'],['/users/1','lea_martin','profile'],['/admin','Prendre soin du carnet.','administration'],['/comment-ca-marche','Un regard.','how-it-works'],['/especes','Un nom à découvrir.','species']]) {
      await navigate(path, text); await width(390); await noOverflow(); await width(1440); await noOverflow(); await screenshot(name)
    }
    if (community) {
      await navigate('/notifications', 'Notifications'); await wait('document.querySelectorAll(".list-row").length > 0'); await click('Tout marquer comme lu'); await wait('!document.body.innerText.includes("· Non lue")')
      await navigate('/users/1', 'lea_martin'); await click('Modifier mon profil'); await fill('[name="bio"]', 'Une curiosité partagée.'); await click('Enregistrer'); await wait('document.body.innerText.includes("Votre profil a bien été mis à jour.")')
      await navigate('/users/2', 'karim'); await wait('!!document.querySelector(".profile-menu")')
      assert.equal(await evaluate('[...document.querySelectorAll("main button")].some(button => button.textContent.includes("Modifier mon profil"))'), false, '#60 editing is only available on the current user profile')
      await navigate('/me/conversations', 'Mes discussions'); await wait('document.querySelectorAll(".list-row").length === 3'); await click('Mes contributions'); await wait('document.body.innerText.includes("La discussion commence avec vous")')
      await navigate('/admin', 'Prendre soin du carnet.'); await fill('[name="name"]', 'Lichens'); await fill('[name="slug"]', 'lichens'); await click('Ajouter la catégorie'); await wait('document.body.innerText.includes("La catégorie « Lichens » a été ajoutée.")')
      await fill('[name="commonName"]', 'Sittelle torchepot'); await fill('[name="scientificName"]', 'Sitta europaea'); await fill('[name="categoryId"]', '1'); await click('Ajouter l’espèce'); await wait('document.body.innerText.includes("L’espèce « Sittelle torchepot » a été ajoutée.")')
    } else {
      await navigate('/notifications', 'Les nouvelles viendront à vous')
      assert.equal(requests.filter(request => /\/proposals$|\/comments$|\/notifications|\/species|\/conversations|^\/api\/users\/\d+$/.test(request.path)).length, 0, 'Disabled modules do not call missing backend endpoints')
    }
  })
  await check('Error recovery, 404, logout and denied administrator access', async () => {
    failFeed = true; await navigate('/', 'Le vivant est partout.'); await wait('document.body.innerText.includes("Un petit contretemps")'); failFeed = false; await click('Réessayer'); await wait('document.querySelectorAll(".observation-card").length > 0')
    await navigate('/chemin-inexistant','Cette page s’est'); await width(390); await noOverflow(); await screenshot('not-found-mobile')
    user.role = 'USER'; await navigate('/admin', 'Un espace réservé')
    await navigate('/', 'Le vivant est partout.'); await wait('!!document.querySelector(".profile-menu summary")'); await evaluate('document.querySelector(".profile-menu summary").click()'); await click('Se déconnecter'); await wait('!localStorage.getItem("merlette.token")')
    await navigate('/notifications', 'Ravi de vous revoir.')
    await fill('[name="email"]', 'lea@example.org'); await fill('[name="password"]', 'incorrect'); await click('Se connecter'); await wait('document.querySelector("[role=alert]")?.textContent.includes("incorrect")')
    await fill('[name="password"]', 'Secret123!'); await click('Se connecter'); await wait('location.pathname === "/notifications" && !!document.querySelector(".profile-menu")')
    assert.equal(await evaluate('localStorage.getItem("merlette.token")'), 'browser-test-token', '#48 login stores the token')
  })
  assert.deepEqual(exceptions, [], 'No unhandled browser exceptions')
  await writeFile(`${artifacts}/results.json`, JSON.stringify({ passed: true, requests: requests.length, exceptions, note: 'HTTP fixtures isolated to browser QA; no backend writes.' }, null, 2))
  console.log(`PASS browser smoke suite (${requests.length} mocked API requests; no backend writes)`)
} catch (error) {
  console.error(error)
  process.exitCode = 1
} finally {
  const closed = new Promise(resolve => ws.addEventListener('close', resolve, { once: true }))
  ws.close()
  await closed
  await fetch(`${debugUrl}/json/close/${target.id}`).catch(() => {})
}
