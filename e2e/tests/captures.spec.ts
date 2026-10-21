import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { answer, chooseChild, createVerifiedFamily, missionExercises } from './support';

// Captures des écrans principaux pour la documentation : `make screenshots` (CAPTURE=1).
const OUT = join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'docs', 'screenshots');
test.skip(!process.env.CAPTURE, 'captures générées uniquement avec CAPTURE=1');

test('captures', async ({ page }, info) => {
  mkdirSync(OUT, { recursive: true });
  const shot = (name: string) => page.screenshot({ path: join(OUT, `${info.project.name}-${name}.png`), fullPage: true });
  if (info.project.name === 'no-webgl') {
    await page.addInitScript(() => {
      const original = HTMLCanvasElement.prototype.getContext as (this: HTMLCanvasElement, type: string, ...rest: unknown[]) => unknown;
      Object.defineProperty(HTMLCanvasElement.prototype, 'getContext', {
        value(this: HTMLCanvasElement, type: string, ...rest: unknown[]) {
          return type.includes('webgl') ? null : original.call(this, type, ...rest);
        },
      });
    });
  }
  await createVerifiedFamily(page, 'captures', ['Nova', 'Tiago']);
  await page.goto('/parent/enfants');
  await expect(page.getByRole('heading', { name: 'Ma famille' })).toBeVisible();
  await shot('01-famille');
  await page.goto('/profils');
  await shot('02-qui-joue');
  await chooseChild(page, 'Nova');
  if (info.project.name !== 'no-webgl') await expect(page.locator('.map-wrap canvas')).toBeVisible();
  await page.waitForTimeout(700);
  await shot('03-carte');
  await page.locator('.world-list').getByRole('link', { name: 'Les Îles des Nombres' }).click();
  await shot('04-monde');
  await page.getByRole('link', { name: 'Jouer' }).first().click();
  const exercises = missionExercises('maths/maths-1-1.json');
  await expect(page.getByRole('heading', { name: 'Question 1 sur 5' })).toBeVisible();
  await shot('05-question');
  for (let i = 1; i <= 5; i++) {
    await answer(page, exercises, i, i !== 4);
    if (i === 4) await shot('06-correction');
    await page.getByRole('button', { name: i < 5 ? 'Question suivante' : 'Voir mon bilan' }).click();
  }
  await expect(page.getByText('4 bonnes réponses sur 5')).toBeVisible();
  await page.waitForTimeout(800);
  await shot('07-bilan');
  await page.goto('/mon-aventurier');
  await shot('08-aventurier');
});
