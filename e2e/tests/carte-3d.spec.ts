import { expect, test } from '@playwright/test';
import { chooseChild, createVerifiedFamily } from './support';

// Avec WebGL : la scène 3D s'affiche, la liste accessible reste présente et la bascule 2D fonctionne.
test('carte 3D et bascule vers la 2D', async ({ page }) => {
  await createVerifiedFamily(page, 'carte3d', ['Nova']);
  await chooseChild(page, 'Nova');

  await expect(page.locator('.map-wrap canvas')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Les mondes' })).toBeVisible();
  await expect(page.locator('.world-list > li')).toHaveCount(6);

  await page.getByRole('button', { name: 'Afficher la carte 2D' }).click();
  await expect(page.locator('.map-wrap canvas')).toHaveCount(0);
  await expect(page.getByRole('group', { name: 'Carte des mondes (2D)' })).toBeVisible();

  await page.reload();
  await expect(page.getByRole('group', { name: 'Carte des mondes (2D)' })).toBeVisible();
  await page.getByRole('button', { name: 'Afficher la carte 3D' }).click();
  await expect(page.locator('.map-wrap canvas')).toBeVisible();
});
