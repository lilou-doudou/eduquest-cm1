import { expect, test } from '@playwright/test';
import { answer, chooseChild, createVerifiedFamily, missionExercises } from './support';

// Projet « mobile » (Pixel 7) : mission d'anglais complète au tactile, sans défilement horizontal.
test('mission d’anglais sur mobile', async ({ page }) => {
  await createVerifiedFamily(page, 'mobile', ['Tiago']);
  await chooseChild(page, 'Tiago');

  const noHorizontalScroll = async () =>
    expect(await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)).toBeLessThanOrEqual(1);

  await noHorizontalScroll();
  await page.locator('.world-list').getByRole('link', { name: 'Le Royaume des Mots' }).click();
  await expect(page.getByRole('heading', { name: 'Le Royaume des Mots', level: 1 })).toBeVisible();
  await noHorizontalScroll();

  await page.getByRole('link', { name: 'Jouer' }).first().click();
  const exercises = missionExercises('english/english-1-1.json');
  for (let i = 1; i <= 5; i++) {
    await answer(page, exercises, i, true);
    await noHorizontalScroll();
    await page.getByRole('button', { name: i < 5 ? 'Question suivante' : 'Voir mon bilan' }).click();
  }
  await expect(page.getByText('5 bonnes réponses sur 5')).toBeVisible();
  await expect(page.getByRole('img', { name: '3 étoiles sur 3' })).toBeVisible();
  await noHorizontalScroll();

  await page.getByRole('link', { name: 'Voir mon aventurier' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  await noHorizontalScroll();
});
