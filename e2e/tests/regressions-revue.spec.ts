import { expect, test } from '@playwright/test';
import { PASSWORD, answer, api, chooseChild, createVerifiedFamily, linkFromMail, missionExercises, uniqueEmail } from './support';

test('un lien de confirmation ne peut être consommé qu’une fois, même en concurrence', async ({ page }) => {
  const email = uniqueEmail('verify-concurrent');
  const notice = await api(page, 'GET', '/privacy/notice');
  await api(page, 'POST', '/auth/register', { email, password: PASSWORD, noticeVersion: notice.noticeVersion });
  const link = await linkFromMail(page.request, email, '/verifier-email');
  const token = new URL(link, 'http://test').searchParams.get('token');
  const csrf = await (await page.request.get('/api/v1/auth/csrf')).json();
  const responses = await Promise.all([0, 1].map(() => page.request.post('/api/v1/auth/verify-email', {
    data: { token }, headers: { [csrf.headerName]: csrf.token },
  })));
  expect(responses.map((response) => response.status()).sort()).toEqual([204, 400]);
  const rejected = responses.find((response) => response.status() === 400)!;
  expect(await rejected.json()).toMatchObject({ code: 'INVALID_TOKEN' });
  await api(page, 'POST', '/auth/login', { email, password: PASSWORD });
});

test('deux réinitialisations concurrentes ne changent le mot de passe qu’une fois', async ({ page }) => {
  const { email } = await createVerifiedFamily(page, 'reset-concurrent', []);
  await api(page, 'POST', '/auth/logout');
  await api(page, 'POST', '/auth/forgot-password', { email });
  const link = await linkFromMail(page.request, email, '/reinitialiser-mot-de-passe');
  const token = new URL(link, 'http://test').searchParams.get('token');
  const csrf = await (await page.request.get('/api/v1/auth/csrf')).json();
  const passwords = ['nouvelle phrase secrète numéro un', 'nouvelle phrase secrète numéro deux'];
  const responses = await Promise.all(passwords.map((newPassword) => page.request.post('/api/v1/auth/reset-password', {
    data: { token, newPassword }, headers: { [csrf.headerName]: csrf.token },
  })));
  expect(responses.map((response) => response.status()).sort()).toEqual([204, 400]);
  const winner = responses.findIndex((response) => response.status() === 204);
  expect(await responses[1 - winner].json()).toMatchObject({ code: 'INVALID_TOKEN' });
  await api(page, 'POST', '/auth/login', { email, password: passwords[winner] });
});

test('une interruption réseau conserve la correction puis permet de passer à la question suivante', async ({ page }) => {
  await createVerifiedFamily(page, 'retry-correction');
  await chooseChild(page, 'Nova');
  await page.locator('.world-list').getByRole('link', { name: 'Les Îles des Nombres' }).click();
  await page.getByRole('link', { name: 'Jouer', exact: true }).first().click();
  await answer(page, missionExercises('maths/maths-1-1.json'), 1, true);

  // Toutes les relances de React Query échouent jusqu’au clic explicite sur Réessayer.
  await page.route('**/api/v1/attempts/*', (route) => route.request().method() === 'GET' ? route.abort('failed') : route.continue());
  await page.getByRole('button', { name: 'Question suivante' }).click();
  await expect(page.getByRole('alert')).toBeVisible({ timeout: 20_000 });
  await expect(page.getByText('Bravo, c’est juste !')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Question 1 sur 5' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Valider', exact: true })).toHaveCount(0);
  const inputs = page.locator('.question-form input');
  for (const input of await inputs.all()) await expect(input).toBeDisabled();

  await page.unroute('**/api/v1/attempts/*');
  await page.getByRole('button', { name: 'Réessayer', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Question 2 sur 5' })).toBeVisible();
  await answer(page, missionExercises('maths/maths-1-1.json'), 2, true);
});

test('le changement de profil ne réaffiche pas la progression du précédent enfant', async ({ page }) => {
  const { email } = await createVerifiedFamily(page, 'cache-session', ['Nova', 'Orion']);
  await chooseChild(page, 'Nova');
  // Navigation React uniquement : un rechargement du document masquerait une fuite de cache.
  await page.getByRole('link', { name: 'Paramètres', exact: true }).click();
  await page.getByLabel("Adresse e-mail de l'adulte").fill(email);
  await page.getByLabel("Mot de passe de l'adulte").fill(PASSWORD);
  await page.getByRole('button', { name: "Ouvrir l'espace adulte" }).click();
  await expect(page.getByRole('heading', { name: 'Ma famille' })).toBeVisible();
  await page.getByRole('link', { name: 'Jouer', exact: true }).click();

  let release!: () => void;
  const gate = new Promise<void>((resolve) => { release = resolve; });
  let requested!: () => void;
  const requestStarted = new Promise<void>((resolve) => { requested = resolve; });
  await page.route('**/api/v1/me/progress', async (route) => {
    requested();
    await gate;
    await route.continue();
  });
  try {
    await page.getByRole('button', { name: 'Orion', exact: true }).click();
    await requestStarted;
    await expect(page).toHaveURL(/\/aventure$/);
    await expect(page.getByRole('heading', { name: 'La carte des mondes' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Bonjour Nova !' })).toHaveCount(0);
  } finally {
    release();
  }
  await expect(page.getByRole('heading', { name: 'Bonjour Orion !' })).toBeVisible();
});
