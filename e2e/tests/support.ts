import { expect, type APIRequestContext, type Page } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

export const PASSWORD = 'une phrase de passe assez longue';
const MAILPIT = process.env.MAILPIT_URL ?? 'http://127.0.0.1:8025';
const CONTENT = join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'content');

export function uniqueEmail(tag: string): string {
  return `e2e-${tag}-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.test`;
}

/** Lit dans Mailpit le dernier lien envoyé à cette adresse (chemin + requête). */
export async function linkFromMail(request: APIRequestContext, to: string, path: string): Promise<string> {
  let link: string | undefined;
  await expect
    .poll(
      async () => {
        const search = await request.get(`${MAILPIT}/api/v1/search`, { params: { query: `to:"${to}"` } });
        const found = (await search.json()) as { messages: { ID: string }[] };
        if (found.messages.length === 0) return undefined;
        const message = await request.get(`${MAILPIT}/api/v1/message/${found.messages[0].ID}`);
        const text = ((await message.json()) as { Text: string }).Text;
        link = text.match(new RegExp(`https?://[^\\s]+(${path}\\?token=[A-Za-z0-9_-]+)`))?.[1];
        return link;
      },
      { message: `e-mail ${path} pour ${to}`, timeout: 15_000 },
    )
    .toBeTruthy();
  return link!;
}

/** Appels API avec le cookie de session du navigateur et le jeton CSRF (crée un compte vérifié sans passer par l'UI). */
export async function api(page: Page, method: 'GET' | 'POST', path: string, data?: unknown) {
  const csrf = (await (await page.request.get('/api/v1/auth/csrf')).json()) as { headerName: string; token: string };
  const res = await page.request.fetch(`/api/v1${path}`, {
    method,
    data,
    headers: method === 'GET' ? {} : { [csrf.headerName]: csrf.token },
  });
  expect(res.ok(), `${method} ${path} → ${res.status()} ${await res.text()}`).toBeTruthy();
  const body = await res.text();
  return body ? JSON.parse(body) : null;
}

export async function createVerifiedFamily(page: Page, tag: string, children: string[] = ['Nova']) {
  const email = uniqueEmail(tag);
  const notice = (await api(page, 'GET', '/privacy/notice')) as { noticeVersion: string };
  await api(page, 'POST', '/auth/register', { email, password: PASSWORD, noticeVersion: notice.noticeVersion });
  const link = await linkFromMail(page.request, email, '/verifier-email');
  await api(page, 'POST', '/auth/verify-email', { token: new URL(link, 'http://x').searchParams.get('token') });
  await api(page, 'POST', '/auth/login', { email, password: PASSWORD });
  for (const nickname of children) await api(page, 'POST', '/children', { nickname, avatarCode: 'owl', grade: 'CM1' });
  return { email };
}

type Exercise = { type: 'MCQ' | 'NUMBER' | 'TEXT'; prompt: string; answer: { optionId?: string; number?: string; text?: string; accepted?: string[] }; options?: { id: string; label: string }[] };

export function missionExercises(file: string): Exercise[] {
  return (JSON.parse(readFileSync(join(CONTENT, file), 'utf8')) as { exercises: Exercise[] }).exercises;
}

/** Répond à la question attendue (juste ou faux) puis attend la correction du serveur. */
export async function answer(page: Page, exercises: Exercise[], position: number, correct: boolean) {
  await expect(page.getByRole('heading', { name: `Question ${position} sur 5` })).toBeVisible();
  const ex = exercises[position - 1];
  if (ex.type === 'MCQ') {
    const option = correct ? ex.options!.find((o) => o.id === ex.answer.optionId)! : ex.options!.find((o) => o.id !== ex.answer.optionId)!;
    await page.getByRole('radio', { name: option.label, exact: true }).check();
  } else {
    const good = ex.answer.number ?? ex.answer.text ?? ex.answer.accepted![0];
    await page.getByRole('textbox').fill(correct ? good : ex.type === 'NUMBER' ? '999999' : 'zzz');
  }
  await page.getByRole('button', { name: 'Valider' }).click();
  await expect(page.getByText(correct ? 'Bravo, c’est juste !' : 'Pas tout à fait…')).toBeVisible();
}

export async function chooseChild(page: Page, nickname: string) {
  await page.goto('/profils');
  await page.getByRole('button', { name: nickname }).click();
  await expect(page.getByRole('heading', { name: `Bonjour ${nickname} !` })).toBeVisible();
}
