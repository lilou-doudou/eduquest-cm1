// Mesure de charge : N enfants simultanés jouent des missions complètes via l'API (sans navigateur).
// Prépare des familles vérifiées (via Mailpit), puis mesure chaque requête du parcours de jeu.
// Usage : make perf (pile isolée E2E) ; variables API_URL, MAILPIT_URL, CHILDREN, ROUNDS.
import { readFileSync } from 'node:fs';
import { cpus, totalmem } from 'node:os';
import { join } from 'node:path';

const API = process.env.API_URL ?? 'http://127.0.0.1:8081';
const MAILPIT = process.env.MAILPIT_URL ?? 'http://127.0.0.1:8025';
const CHILDREN = Number(process.env.CHILDREN ?? 20);
const ROUNDS = Number(process.env.ROUNDS ?? 3);
const PASSWORD = 'mesure de charge locale uniquement';
const content = JSON.parse(readFileSync(join(import.meta.dirname, '..', 'content', 'maths', 'maths-1-1.json'), 'utf8'));

class Client {
  cookie = '';
  csrf = null;
  async call(method, path, body, timings, label) {
    if (method !== 'GET' && !this.csrf) {
      this.csrf = await this.call('GET', '/api/v1/auth/csrf');
    }
    const headers = { Accept: 'application/json', Cookie: this.cookie };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (method !== 'GET') headers[this.csrf.headerName] = this.csrf.token;
    const t0 = performance.now();
    const res = await fetch(API + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    const text = await res.text();
    if (timings) (timings[label] ??= []).push(performance.now() - t0);
    const set = res.headers.getSetCookie().find((c) => c.startsWith('EQSESSION='));
    if (set) { this.cookie = set.split(';')[0]; this.csrf = null; }
    if (!res.ok) throw new Error(`${method} ${path} → ${res.status} ${text}`);
    return text ? JSON.parse(text) : null;
  }
}

async function verifiedParent(i) {
  const email = `perf-${Date.now()}-${i}@example.test`;
  const c = new Client();
  const { noticeVersion } = await c.call('GET', '/api/v1/privacy/notice');
  await c.call('POST', '/api/v1/auth/register', { email, password: PASSWORD, noticeVersion });
  let token;
  for (let attempt = 0; attempt < 30 && !token; attempt++) {
    const found = await (await fetch(`${MAILPIT}/api/v1/search?query=${encodeURIComponent(`to:"${email}"`)}`)).json();
    if (found.messages.length) {
      const msg = await (await fetch(`${MAILPIT}/api/v1/message/${found.messages[0].ID}`)).json();
      token = msg.Text.match(/verifier-email\?token=([A-Za-z0-9_-]+)/)?.[1];
    }
    if (!token) await new Promise((r) => setTimeout(r, 300));
  }
  await c.call('POST', '/api/v1/auth/verify-email', { token });
  await c.call('POST', '/api/v1/auth/login', { email, password: PASSWORD });
  return { email, client: c };
}

function answerFor(position) {
  const a = content.exercises[position - 1].answer;
  return a.optionId ? { optionId: a.optionId } : a.number ? { number: a.number } : { text: a.accepted[0] };
}

async function play(child, timings) {
  const world = await child.call('GET', '/api/v1/worlds/iles-des-nombres', undefined, timings, 'GET /worlds/{slug}');
  const missionId = world.levels[0].missions[0].id;
  for (let round = 0; round < ROUNDS; round++) {
    let view = await child.call('POST', `/api/v1/missions/${missionId}/attempts`, undefined, timings, 'POST /missions/{id}/attempts');
    while (view.currentQuestion) {
      const q = view.currentQuestion;
      await child.call('PUT', `/api/v1/attempts/${view.attemptId}/answers/${q.id}`, { answer: answerFor(q.position) }, timings, 'PUT /attempts/{id}/answers/{ex}');
      view = await child.call('GET', `/api/v1/attempts/${view.attemptId}`, undefined, timings, 'GET /attempts/{id}');
    }
    await child.call('POST', `/api/v1/attempts/${view.attemptId}/complete`, undefined, timings, 'POST /attempts/{id}/complete');
    await child.call('GET', '/api/v1/me/progress', undefined, timings, 'GET /me/progress');
  }
}

const pct = (sorted, p) => sorted[Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1)];

const families = Math.ceil(CHILDREN / 5);
console.log(`Préparation : ${families} familles, ${CHILDREN} enfants (API ${API})…`);
const parents = await Promise.all(Array.from({ length: families }, (_, i) => verifiedParent(i)));
const children = [];
for (let i = 0; i < CHILDREN; i++) {
  const parent = parents[Math.floor(i / 5)];
  const created = await parent.client.call('POST', '/api/v1/children', { nickname: `Enfant ${i + 1}`, avatarCode: 'fox', grade: 'CM1' });
  const c = new Client();
  await c.call('POST', '/api/v1/auth/login', { email: parent.email, password: PASSWORD });
  await c.call('POST', '/api/v1/auth/child-session', { childId: created.id });
  children.push(c);
}

console.log(`Mesure : ${CHILDREN} enfants simultanés × ${ROUNDS} missions de 5 questions…`);
const timings = {};
const t0 = performance.now();
await Promise.all(children.map((c) => play(c, timings)));
const wall = (performance.now() - t0) / 1000;

const all = Object.values(timings).flat().sort((a, b) => a - b);
const rows = Object.entries(timings).map(([label, values]) => {
  const s = values.sort((a, b) => a - b);
  return { requête: label, n: s.length, p50: pct(s, 50).toFixed(1), p95: pct(s, 95).toFixed(1), max: s[s.length - 1].toFixed(1) };
});
rows.push({ requête: 'TOUTES', n: all.length, p50: pct(all, 50).toFixed(1), p95: pct(all, 95).toFixed(1), max: all[all.length - 1].toFixed(1) });
console.table(rows);
console.log(`Durée ${wall.toFixed(1)} s, ${(all.length / wall).toFixed(0)} requêtes/s. Machine : ${cpus().length} vCPU ${cpus()[0]?.model ?? ''}, ${(totalmem() / 2 ** 30).toFixed(1)} Go, Node ${process.version}. Temps en ms.`);
const p95 = pct(all, 95);
console.log(p95 < 500 ? `Objectif p95 < 500 ms : atteint (${p95.toFixed(1)} ms).` : `Objectif p95 < 500 ms : NON atteint (${p95.toFixed(1)} ms).`);
process.exit(p95 < 500 ? 0 : 1);
