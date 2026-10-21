import { describe, expect, it } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import { PlayPage } from '../src/features/missions/PlayPage';
import { CHILD_ME, mockApi, renderAt } from './helpers';

const question = (position: number) => ({
  id: `ex${position}`,
  position,
  total: 5,
  type: 'MCQ',
  instruction: 'Choisis la bonne traduction.',
  prompt: 'Comment dit-on « bleu » en anglais ?',
  options: [
    { id: 'a', label: 'blue' },
    { id: 'b', label: 'red' },
    { id: 'c', label: 'green' },
  ],
  hintUsed: false,
});

const view = (answered: number) => ({
  attemptId: 'att1',
  missionId: 'm1',
  missionTitle: "L'arc-en-ciel",
  worldSlug: 'royaume-des-mots',
  status: 'IN_PROGRESS',
  answeredCount: answered,
  total: 5,
  readyToComplete: false,
  currentQuestion: question(answered + 1),
});

function renderPlay() {
  return renderAt(
    <Routes>
      <Route path="/missions/:id/jouer" element={<PlayPage />} />
      <Route path="/parties/:id/bilan" element={<h1>Bilan</h1>} />
    </Routes>,
    '/missions/m1/jouer',
  );
}

describe('écran de mission', () => {
  it("n'affiche ni corrigé ni explication avant la soumission", async () => {
    mockApi({
      'GET /api/v1/auth/me': () => ({ status: 200, body: CHILD_ME }),
      'POST /api/v1/missions/m1/attempts': () => ({ status: 201, body: view(0) }),
      'GET /api/v1/attempts/att1': () => ({ status: 200, body: view(0) }),
    });
    renderPlay();
    expect(await screen.findByText('Question 1 sur 5')).toBeInTheDocument();
    expect(screen.queryByText(/bonne réponse/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/signifie/)).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Valider' })).toBeDisabled();
  });

  it('affiche la correction serveur et ne soumet qu’une fois malgré un double clic', async () => {
    const calls = mockApi({
      'POST /api/v1/missions/m1/attempts': () => ({ status: 201, body: view(0) }),
      'GET /api/v1/attempts/att1': () => ({ status: 200, body: view(0) }),
      'PUT /api/v1/attempts/att1/answers/ex1': async () => {
        await new Promise((r) => setTimeout(r, 30));
        return {
          status: 200,
          body: { exerciseId: 'ex1', correct: false, hintUsed: false, expectedAnswer: { optionId: 'a' }, explanation: '« Blue » signifie « bleu ».', answeredCount: 1, readyToComplete: false },
        };
      },
    });
    const user = userEvent.setup();
    renderPlay();
    await user.click(await screen.findByLabelText('red'));
    const submit = screen.getByRole('button', { name: 'Valider' });
    await user.dblClick(submit);
    expect(await screen.findByText('Pas tout à fait…')).toBeInTheDocument();
    expect(screen.getByText('« Blue » signifie « bleu ».')).toBeInTheDocument();
    expect(calls.filter((c) => c.method === 'PUT')).toHaveLength(1);
    expect(calls.find((c) => c.method === 'PUT')!.body).toEqual({ answer: { optionId: 'b' } });
    expect(screen.getByRole('button', { name: 'Question suivante' })).toHaveFocus();
  });

  it('reprend une partie interrompue à la question suivante non terminée', async () => {
    mockApi({
      'POST /api/v1/missions/m1/attempts': () => ({ status: 200, body: view(2) }),
      'GET /api/v1/attempts/att1': () => ({ status: 200, body: view(2) }),
    });
    renderPlay();
    expect(await screen.findByText('Question 3 sur 5')).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: 'Questions corrigées' })).toHaveAttribute('aria-valuenow', '2');
  });

  it('explique une mission verrouillée sans proposer de réessayer', async () => {
    mockApi({
      'POST /api/v1/missions/m1/attempts': () => ({ status: 409, body: { code: 'MISSION_LOCKED', detail: "Cette mission n'est pas encore débloquée." } }),
    });
    renderPlay();
    expect(await screen.findByText("Cette mission n'est pas encore débloquée.")).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Réessayer' })).not.toBeInTheDocument();
  });

  it('demande un indice et l’affiche', async () => {
    mockApi({
      'POST /api/v1/missions/m1/attempts': () => ({ status: 201, body: view(0) }),
      'GET /api/v1/attempts/att1': () => ({ status: 200, body: view(0) }),
      'POST /api/v1/attempts/att1/answers/ex1/hint': () => ({ status: 200, body: { exerciseId: 'ex1', hint: 'C’est la couleur du ciel.' } }),
    });
    const user = userEvent.setup();
    renderPlay();
    await user.click(await screen.findByRole('button', { name: "Besoin d'un indice ?" }));
    await waitFor(() => expect(screen.getByText('C’est la couleur du ciel.')).toBeInTheDocument());
  });
});
