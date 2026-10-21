import { describe, expect, it } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AppRoutes } from '../src/app/App';
import { CHILD_ME, PARENT_ME, mockApi, renderAt } from './helpers';

const worlds = [
  { slug: 'iles-des-nombres', title: 'Les Îles des Nombres', subjectCode: 'MATHS', displayOrder: 1, availability: 'PLAYABLE', assetKey: 'world-numbers', description: 'Maths', missionCount: 6, completedMissions: 1, stars: 2, maxStars: 18, completed: false },
  { slug: 'vallee-des-formes', title: 'La Vallée des Formes', subjectCode: 'GEOMETRY', displayOrder: 3, availability: 'COMING_SOON', assetKey: 'world-shapes', description: 'Géométrie', missionCount: 0, completedMissions: 0, stars: 0, maxStars: 0, completed: false },
];
const progress = { childId: 'c1', nickname: 'Nova', avatarCode: 'fox', xpTotal: 60, adventurerLevel: 1, xpIntoLevel: 60, xpForNextLevel: 100, totalStars: 2, badges: [], worlds };

describe('carte des mondes', () => {
  it('sans WebGL, affiche la carte 2D et la liste HTML avec les mondes futurs', async () => {
    mockApi({
      'GET /api/v1/auth/me': () => ({ status: 200, body: CHILD_ME }),
      'GET /api/v1/worlds': () => ({ status: 200, body: worlds }),
      'GET /api/v1/me/progress': () => ({ status: 200, body: progress }),
    });
    renderAt(<AppRoutes />, '/aventure');
    expect(await screen.findByRole('heading', { name: 'Bonjour Nova !' })).toBeInTheDocument();
    const map = await screen.findByRole('group', { name: 'Carte des mondes (2D)' });
    expect(within(map).getByRole('link', { name: /Vallée des Formes\s*Bientôt disponible/i })).toHaveAttribute('href', '/mondes/vallee-des-formes');
    expect(screen.getAllByText('Bientôt disponible')).toHaveLength(2); // île de la carte 2D et carte HTML
    expect(within(map).getByRole('link', { name: /Îles des Nombres\s*2\/18/i })).toHaveAttribute('href', '/mondes/iles-des-nombres');
    expect(screen.getByRole('link', { name: 'Les Îles des Nombres' })).toHaveAttribute('href', '/mondes/iles-des-nombres');
  });
});

describe('gardes et session', () => {
  it('redirige vers la connexion quand aucune session', async () => {
    mockApi({ 'GET /api/v1/auth/me': () => ({ status: 401, body: { code: 'SESSION_REQUIRED', detail: 'x' } }) });
    renderAt(<AppRoutes />, '/aventure');
    expect(await screen.findByRole('heading', { name: 'Connexion adulte' })).toBeInTheDocument();
  });

  it("un parent qui ouvre l'espace enfant est renvoyé vers le choix du profil", async () => {
    mockApi({
      'GET /api/v1/auth/me': () => ({ status: 200, body: PARENT_ME }),
      'GET /api/v1/children': () => ({ status: 200, body: [] }),
    });
    renderAt(<AppRoutes />, '/aventure');
    expect(await screen.findByRole('heading', { name: 'Qui joue ?' })).toBeInTheDocument();
  });

  it('affiche la session expirée quand le serveur répond 401 en cours de route', async () => {
    let expired = false;
    mockApi({
      'GET /api/v1/auth/me': () => (expired ? { status: 401, body: { code: 'SESSION_REQUIRED', detail: 'x' } } : { status: 200, body: CHILD_ME }),
      'GET /api/v1/worlds/iles-des-nombres': () => {
        expired = true;
        return { status: 401, body: { code: 'SESSION_REQUIRED', detail: 'Session absente ou expirée.' } };
      },
    });
    renderAt(<AppRoutes />, '/mondes/iles-des-nombres');
    expect(await screen.findByText('La session a expiré. Reconnecte-toi pour continuer.')).toBeInTheDocument();
  });
});

describe('formulaires adultes', () => {
  it("l'inscription exige 12 caractères et la lecture de la notice avant tout envoi", async () => {
    const calls = mockApi({
      'GET /api/v1/auth/me': () => ({ status: 401, body: { code: 'SESSION_REQUIRED' } }),
      'GET /api/v1/privacy/notice': () => ({ status: 200, body: { noticeVersion: '2026-10-adulte-v1' } }),
      'POST /api/v1/auth/register': () => ({ status: 202 }),
    });
    const user = userEvent.setup();
    renderAt(<AppRoutes />, '/inscription');
    await user.type(await screen.findByLabelText('Adresse e-mail'), 'adulte@exemple.test');
    await user.type(screen.getByLabelText('Mot de passe'), 'court');
    await user.click(screen.getByRole('button', { name: 'Créer le compte' }));
    expect(screen.getByRole('alert')).toHaveTextContent('au moins 12 caractères');
    await user.type(screen.getByLabelText('Mot de passe'), ' mais assez long');
    await user.click(screen.getByRole('button', { name: 'Créer le compte' }));
    expect(screen.getByRole('alert')).toHaveTextContent('notice');
    await user.click(screen.getByRole('checkbox'));
    await user.click(screen.getByRole('button', { name: 'Créer le compte' }));
    expect(await screen.findByRole('heading', { name: 'Vérifie ta boîte e-mail' })).toBeInTheDocument();
    const register = calls.filter((c) => c.path === '/api/v1/auth/register');
    expect(register).toHaveLength(1);
    expect(register[0].body).toEqual({ email: 'adulte@exemple.test', password: 'court mais assez long', noticeVersion: '2026-10-adulte-v1' });
  });
});

describe('aventurier', () => {
  it("équipe un accessoire possédé et propose de l'enlever", async () => {
    const rewards = {
      rewards: [
        { code: 'CAPTAIN_CAP', type: 'COSMETIC', title: 'Casquette de capitaine', description: 'Niveau 1', assetKey: 'x', slot: 'HEAD', owned: true, grantedAt: '2026-10-21T10:00:00Z' },
        { code: 'SPACE_HELMET', type: 'COSMETIC', title: "Casque d'astronaute", description: 'Rang 8', assetKey: 'y', slot: 'HEAD', owned: false, grantedAt: null },
      ],
      equipment: {},
    };
    const calls = mockApi({
      'GET /api/v1/auth/me': () => ({ status: 200, body: CHILD_ME }),
      'GET /api/v1/me/progress': () => ({ status: 200, body: progress }),
      'GET /api/v1/me/rewards': () => ({ status: 200, body: rewards }),
      'PUT /api/v1/me/equipment/HEAD': () => ({ status: 200, body: { ...rewards, equipment: { HEAD: 'CAPTAIN_CAP' } } }),
    });
    const user = userEvent.setup();
    renderAt(<AppRoutes />, '/mon-aventurier');
    await user.click(await screen.findByRole('button', { name: 'Équiper' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Retirer' })).toHaveAttribute('aria-pressed', 'true'));
    expect(calls.find((c) => c.method === 'PUT')!.body).toEqual({ rewardCode: 'CAPTAIN_CAP' });
    expect(screen.getAllByText('À gagner')).toHaveLength(1);
  });
});
