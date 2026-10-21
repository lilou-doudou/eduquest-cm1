import { expect, test } from '@playwright/test';
import { chooseChild, createVerifiedFamily } from './support';

// Navigateur sans WebGL : la carte 2D équivalente remplace la scène 3D sans perte de fonction.
test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    const original = HTMLCanvasElement.prototype.getContext as (this: HTMLCanvasElement, type: string, ...rest: unknown[]) => unknown;
    // Surcharge volontaire : aucun contexte WebGL n'est disponible.
    Object.defineProperty(HTMLCanvasElement.prototype, 'getContext', {
      value(this: HTMLCanvasElement, type: string, ...rest: unknown[]) {
        return type.includes('webgl') ? null : original.call(this, type, ...rest);
      },
    });
  });
});

test('carte 2D de repli et verrouillage des mondes', async ({ page }) => {
  await createVerifiedFamily(page, 'nowebgl', ['Nova']);
  await chooseChild(page, 'Nova');

  await expect(page.getByText(/^Carte 2D/)).toBeVisible();
  await expect(page.locator('canvas')).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Afficher la carte 3D' })).toHaveCount(0);
  await expect(page.locator('.world-list').getByText('Bientôt disponible', { exact: true })).toHaveCount(4);

  // Les îles de la carte 2D sont des liens : on y entre au clavier.
  const island = page.locator('.map-wrap').getByRole('link', { name: /Îles des Nombres/ });
  await island.focus();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: 'Les Îles des Nombres', level: 1 })).toBeVisible();
  await expect(page.getByText('Verrouillée : obtiens au moins une étoile à la mission précédente.').first()).toBeVisible();
  await expect(page.getByRole('link', { name: 'Jouer' })).toHaveCount(1);

  // Un monde non jouable reste consultable mais n'offre aucune mission.
  await page.getByRole('link', { name: '← Retour à la carte' }).click();
  await page.locator('.world-list .is-locked').first().getByRole('link').click();
  await expect(page.getByText('Ce monde est en préparation.', { exact: false })).toBeVisible();
});
