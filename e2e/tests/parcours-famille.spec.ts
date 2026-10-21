import { expect, test } from '@playwright/test';
import { readFile } from 'node:fs/promises';
import { PASSWORD, answer, linkFromMail, missionExercises, uniqueEmail } from './support';

// Parcours complet par l'interface : inscription, vérification, profil, mission interrompue puis reprise,
// bilan, retour à l'espace adulte, suivi, export puis suppression du compte.
test('parcours famille de bout en bout', async ({ page }) => {
  const email = uniqueEmail('parcours');
  const exercises = missionExercises('maths/maths-1-1.json');

  await test.step('inscription et vérification de l’e-mail', async () => {
    await page.goto('/inscription');
    await page.getByLabel('Adresse e-mail').fill(email);
    await page.getByLabel('Mot de passe').fill(PASSWORD);
    await page.getByLabel(/J'ai lu la/).check();
    await page.getByRole('button', { name: 'Créer le compte' }).click();
    await expect(page.getByRole('heading', { name: 'Vérifie ta boîte e-mail' })).toBeVisible();

    await page.goto(await linkFromMail(page.request, email, '/verifier-email'));
    await expect(page.getByText('Adresse confirmée !')).toBeVisible();
  });

  await test.step('connexion et création du profil enfant', async () => {
    await page.getByRole('link', { name: 'Se connecter' }).click();
    await page.getByLabel('Adresse e-mail').fill(email);
    await page.getByLabel('Mot de passe').fill(PASSWORD);
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await page.getByRole('link', { name: 'Ma famille' }).click();
    await page.getByRole('button', { name: 'Ajouter un profil' }).click();
    await page.getByLabel('Pseudonyme').fill('Capitaine Lune');
    await page.getByRole('radio', { name: 'Panda' }).check();
    await page.getByRole('button', { name: 'Créer le profil' }).click();
    await expect(page.getByRole('heading', { name: 'Capitaine Lune' })).toBeVisible();
  });

  await test.step('mode enfant : début de mission puis pause', async () => {
    await page.getByRole('link', { name: 'Jouer' }).click();
    await page.getByRole('button', { name: 'Capitaine Lune' }).click();
    await expect(page.getByRole('heading', { name: 'Bonjour Capitaine Lune !' })).toBeVisible();
    await page.locator('.world-list').getByRole('link', { name: 'Les Îles des Nombres' }).click();
    await page.getByRole('link', { name: 'Jouer' }).first().click();
    for (const position of [1, 2]) {
      await answer(page, exercises, position, true);
      await page.getByRole('button', { name: 'Question suivante' }).click();
    }
    await expect(page.getByRole('heading', { name: 'Question 3 sur 5' })).toBeVisible();
    await page.getByRole('button', { name: 'Quitter' }).click();
    await page.getByRole('button', { name: 'Enregistrer et revenir à la carte' }).click();
  });

  await test.step('reprise après rechargement, puis bilan', async () => {
    await page.reload();
    await page.getByRole('link', { name: 'Reprendre' }).click();
    await expect(page.getByRole('heading', { name: 'Question 3 sur 5' })).toBeVisible();
    await answer(page, exercises, 3, true);
    await page.getByRole('button', { name: 'Question suivante' }).click();
    await answer(page, exercises, 4, false);
    await page.getByRole('button', { name: 'Question suivante' }).click();
    await answer(page, exercises, 5, true);
    await page.getByRole('button', { name: 'Voir mon bilan' }).click();

    await expect(page.getByRole('heading', { name: 'Bilan : Les quais du port' })).toBeVisible();
    await expect(page.getByText('4 bonnes réponses sur 5')).toBeVisible();
    await expect(page.getByRole('img', { name: '2 étoiles sur 3' })).toBeVisible();
    await expect(page.getByText('+60 XP')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Nouvelles récompenses' })).toBeVisible();
  });

  await test.step('l’espace adulte est protégé, puis rouvert avec le mot de passe', async () => {
    await page.goto('/parent/enfants');
    await expect(page).toHaveURL(/\/aventure$/);

    await page.goto('/parametres');
    await page.getByLabel("Adresse e-mail de l'adulte").fill(email);
    await page.getByLabel("Mot de passe de l'adulte").fill(PASSWORD);
    await page.getByRole('button', { name: "Ouvrir l'espace adulte" }).click();
    await expect(page.getByRole('heading', { name: 'Ma famille' })).toBeVisible();
  });

  await test.step('suivi parent', async () => {
    await page.getByRole('link', { name: 'Voir le suivi' }).click();
    await expect(page.getByText('Les quais du port')).toBeVisible();
  });

  await test.step('export des données', async () => {
    await page.getByRole('link', { name: 'Données' }).click();
    const downloadPromise = page.waitForEvent('download');
    await page.getByRole('button', { name: "Télécharger l'export" }).click();
    const download = await downloadPromise;
    const exported = JSON.parse(await readFile((await download.path())!, 'utf8'));
    const serialized = JSON.stringify(exported);
    expect(serialized).toContain('Capitaine Lune');
    expect(serialized).not.toMatch(/password|\$argon2/i);
  });

  await test.step('suppression du compte', async () => {
    await page.getByLabel('Écris SUPPRIMER pour confirmer').fill('SUPPRIMER');
    await page.getByRole('button', { name: 'Supprimer définitivement le compte' }).click();
    const reauth = page.getByRole('form', { name: 'Confirmation du mot de passe' });
    if (await reauth.isVisible().catch(() => false)) {
      await reauth.getByLabel('Mot de passe').fill(PASSWORD);
      await reauth.getByRole('button', { name: 'Confirmer' }).click();
    }
    await expect(page).toHaveURL(/\/$|\/connexion/);

    await page.goto('/connexion');
    await page.getByLabel('Adresse e-mail').fill(email);
    await page.getByLabel('Mot de passe').fill(PASSWORD);
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page.getByRole('alert')).toBeVisible();
  });
});
