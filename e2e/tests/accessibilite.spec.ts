import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { answer, chooseChild, createVerifiedFamily, missionExercises } from './support';

// Contrôle automatisé axe-core (WCAG 2.1 A/AA) des écrans principaux, complété par des vérifications
// clavier et « animations réduites ». Les vérifications manuelles (lecteur d'écran, zoom 200 %) restent à faire.
async function expectNoViolations(page: Page, screen: string) {
  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
  const summary = results.violations.map((v) => `${v.id} (${v.impact}) : ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`);
  expect(summary, `violations axe sur ${screen}`).toEqual([]);
}

test('écrans publics', async ({ page }) => {
  for (const path of ['/', '/connexion', '/inscription', '/confidentialite']) {
    await page.goto(path);
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await expectNoViolations(page, path);
  }
});

test('parcours enfant et espace parent', async ({ page }) => {
  await createVerifiedFamily(page, 'a11y', ['Nova']);
  await page.goto('/parent/enfants');
  await expect(page.getByRole('heading', { name: 'Ma famille' })).toBeVisible();
  await expectNoViolations(page, 'Ma famille');
  await page.goto('/parent/confidentialite');
  await expect(page.getByRole('heading', { name: 'Données de la famille' })).toBeVisible();
  await expectNoViolations(page, 'Données');

  await chooseChild(page, 'Nova');
  await expectNoViolations(page, 'carte');
  await page.locator('.world-list').getByRole('link', { name: 'Les Îles des Nombres' }).click();
  await expect(page.getByRole('heading', { name: 'Les Îles des Nombres', level: 1 })).toBeVisible();
  await expectNoViolations(page, 'monde');

  await page.getByRole('link', { name: 'Jouer' }).first().click();
  const exercises = missionExercises('maths/maths-1-1.json');
  await expect(page.getByRole('heading', { name: 'Question 1 sur 5' })).toBeVisible();
  await expectNoViolations(page, 'question');
  for (let i = 1; i <= 5; i++) {
    await answer(page, exercises, i, i !== 2);
    if (i === 2) await expectNoViolations(page, 'correction erronée');
    await page.getByRole('button', { name: i < 5 ? 'Question suivante' : 'Voir mon bilan' }).click();
  }
  await expect(page.getByText('4 bonnes réponses sur 5')).toBeVisible();
  await expectNoViolations(page, 'bilan');
  await page.goto('/mon-aventurier');
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  await expectNoViolations(page, 'aventurier');
});

test('mission au clavier seul, focus visible après correction', async ({ page }) => {
  await createVerifiedFamily(page, 'clavier', ['Nova']);
  await chooseChild(page, 'Nova');
  await page.locator('.world-list').getByRole('link', { name: 'Les Îles des Nombres' }).focus();
  await page.keyboard.press('Enter');
  await page.getByRole('link', { name: 'Jouer' }).first().focus();
  await page.keyboard.press('Enter');

  await expect(page.getByRole('heading', { name: 'Question 1 sur 5' })).toBeVisible();
  await page.getByRole('textbox').focus();
  await page.keyboard.type('4205');
  await page.keyboard.press('Enter');
  await expect(page.getByText('Bravo, c’est juste !')).toBeVisible();
  // Le focus passe sur « Question suivante » : Entrée suffit pour continuer.
  await expect(page.getByRole('button', { name: 'Question suivante' })).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: 'Question 2 sur 5' })).toBeVisible();

  // QCM : flèches pour choisir, Entrée pour valider.
  await page.getByRole('radio').first().focus();
  await page.keyboard.press('ArrowDown');
  await expect(page.getByRole('radio', { name: '3 908', exact: true })).toBeChecked();
  await page.keyboard.press('Enter');
  await expect(page.getByText('Bravo, c’est juste !')).toBeVisible();
});

test.describe('animations réduites', () => {
  test.use({ contextOptions: { reducedMotion: 'reduce' } });
  test('la préférence système désactive les animations', async ({ page }) => {
    await page.goto('/');
    const duration = await page.evaluate(() => {
      const probe = document.createElement('div');
      probe.className = 'btn';
      document.body.appendChild(probe);
      return getComputedStyle(probe).transitionDuration;
    });
    expect(duration.split(',').every((d) => parseFloat(d) <= 0.01)).toBeTruthy();
  });
});
