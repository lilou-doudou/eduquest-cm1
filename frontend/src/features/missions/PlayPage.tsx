import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError, get, post, put } from '../../api/client';
import type { Answer, AttemptView, Correction, Summary } from '../../api/types';
import { playFeedback } from '../../app/preferences';
import { ErrorMessage, Loading } from '../../components/States';
import { ProgressBar } from '../../components/ProgressBar';
import { expectedLabel, QuestionForm } from './QuestionForm';

const attemptKey = (id: string) => ['attempt', id];

export function PlayPage() {
  const { id: missionId } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [attemptId, setAttemptId] = useState<string | null>(null);
  const started = useRef(false);

  // Démarre une partie ou reprend celle en cours : le serveur décide (201 nouvelle, 200 existante).
  const start = useMutation({
    mutationFn: () => post<AttemptView>(`/missions/${missionId}/attempts`),
    onSuccess: (view) => {
      qc.setQueryData(attemptKey(view.attemptId), view);
      setAttemptId(view.attemptId);
    },
  });
  useEffect(() => {
    if (!started.current) {
      started.current = true;
      start.mutate();
    }
  }, [start]);

  if (start.isPending || (!attemptId && !start.error)) return <Loading label="Préparation de la mission…" />;
  if (start.error) {
    const locked = start.error instanceof ApiError && (start.error.code === 'MISSION_LOCKED' || start.error.code === 'WORLD_NOT_AVAILABLE');
    return (
      <section>
        <h1>Mission indisponible</h1>
        <ErrorMessage error={start.error} onRetry={locked ? undefined : () => start.mutate()} />
        <Link to="/aventure">Retour à la carte</Link>
      </section>
    );
  }
  return <Attempt attemptId={attemptId!} onFinished={(id) => navigate(`/parties/${id}/bilan`, { replace: true })} />;
}

function Attempt({ attemptId, onFinished }: { attemptId: string; onFinished: (id: string) => void }) {
  const qc = useQueryClient();
  const navigate = useNavigate();
  const attempt = useQuery({ queryKey: attemptKey(attemptId), queryFn: () => get<AttemptView>(`/attempts/${attemptId}`) });
  const [correction, setCorrection] = useState<Correction | null>(null);
  const [hint, setHint] = useState<string | null>(null);
  const [quitOpen, setQuitOpen] = useState(false);
  const continueRef = useRef<HTMLButtonElement>(null);

  const question = attempt.data?.currentQuestion;

  const answer = useMutation({
    mutationFn: (a: Answer) => put<Correction>(`/attempts/${attemptId}/answers/${question!.id}`, { answer: a }),
    onSuccess: (c) => {
      setCorrection(c);
      playFeedback(c.correct);
    },
  });
  const askHint = useMutation({
    mutationFn: () => post<{ hint: string }>(`/attempts/${attemptId}/answers/${question!.id}/hint`),
    onSuccess: (h) => setHint(h.hint),
  });
  const complete = useMutation({
    mutationFn: () => post<Summary>(`/attempts/${attemptId}/complete`),
    onSuccess: (summary) => {
      qc.invalidateQueries({ queryKey: ['worlds'] });
      qc.invalidateQueries({ queryKey: ['me'] });
      qc.setQueryData(['summary', attemptId], summary);
      onFinished(attemptId);
    },
  });
  const abandon = useMutation({
    mutationFn: () => post(`/attempts/${attemptId}/abandon`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['worlds'] });
      navigate(attempt.data ? `/mondes/${attempt.data.worldSlug}` : '/aventure');
    },
  });

  useEffect(() => {
    if (correction) continueRef.current?.focus();
  }, [correction]);

  useEffect(() => {
    if (attempt.data?.status === 'COMPLETED') onFinished(attemptId);
  }, [attempt.data?.status, attemptId, onFinished]);

  const next = async () => {
    const result = await attempt.refetch();
    setCorrection(null);
    setHint(null);
    answer.reset();
    askHint.reset();
    if (result.data && result.data.readyToComplete && !complete.isPending) complete.mutate();
  };

  if (attempt.isPending) return <Loading />;
  if (!attempt.data) return <ErrorMessage error={attempt.error} onRetry={() => attempt.refetch()} />;
  const view = attempt.data;
  if (view.status === 'ABANDONED') {
    return (
      <section>
        <h1>{view.missionTitle}</h1>
        <p>Cette partie a été abandonnée.</p>
        <Link to={`/mondes/${view.worldSlug}`}>Retour au monde</Link>
      </section>
    );
  }
  const shownHint = hint ?? question?.hint ?? null;

  return (
    <section className="play">
      <div className="play-head">
        <h1>{view.missionTitle}</h1>
        <button type="button" className="btn btn-ghost" onClick={() => setQuitOpen(true)}>
          Quitter
        </button>
      </div>
      <ProgressBar value={correction?.answeredCount ?? view.answeredCount} max={view.total} label="Questions corrigées" />

      {quitOpen && (
        <div className="panel" role="dialog" aria-modal="false" aria-labelledby="quit-title">
          <h2 id="quit-title">Faire une pause ?</h2>
          <p>Tes réponses déjà validées sont enregistrées. Tu pourras reprendre la mission plus tard.</p>
          <div className="actions">
            <button type="button" className="btn btn-primary" onClick={() => navigate(`/mondes/${view.worldSlug}`)}>
              Enregistrer et revenir à la carte
            </button>
            <button type="button" className="btn btn-secondary" onClick={() => setQuitOpen(false)}>
              Continuer la mission
            </button>
            <button type="button" className="btn btn-ghost" onClick={() => abandon.mutate()} disabled={abandon.isPending}>
              Abandonner cette partie
            </button>
          </div>
          {abandon.error && <ErrorMessage error={abandon.error} />}
        </div>
      )}

      {view.readyToComplete && !question && (
        <div className="panel">
          <p>Tu as répondu aux cinq questions.</p>
          <button type="button" className="btn btn-primary" onClick={() => complete.mutate()} disabled={complete.isPending}>
            Voir mon bilan
          </button>
          {complete.error && <ErrorMessage error={complete.error} onRetry={() => complete.mutate()} />}
        </div>
      )}

      {question && (
        <article className="question" aria-labelledby={`qtitle-${question.id}`}>
          <h2 id={`qtitle-${question.id}`} className="question-position">
            Question {question.position} sur {question.total}
          </h2>
          <p className="instruction">{question.instruction}</p>
          <QuestionForm key={question.id} question={question} correction={correction} pending={answer.isPending} onSubmit={(a) => answer.mutate(a)} />

          {!correction && (
            <div className="hint-zone">
              {shownHint ? (
                <p className="hint" role="note">
                  <strong>Indice :</strong> {shownHint}
                </p>
              ) : (
                <button type="button" className="btn btn-ghost" onClick={() => askHint.mutate()} disabled={askHint.isPending}>
                  Besoin d'un indice ?
                </button>
              )}
              {askHint.error && <ErrorMessage error={askHint.error} />}
            </div>
          )}

          {answer.error && <ErrorMessage error={answer.error} onRetry={answer.variables ? () => answer.mutate(answer.variables!) : undefined} />}

          <div aria-live="polite" role="status" className="feedback-live">
            {correction && (
              <div className={`feedback ${correction.correct ? 'feedback-ok' : 'feedback-ko'}`}>
                <p className="feedback-title">{correction.correct ? 'Bravo, c’est juste !' : 'Pas tout à fait…'}</p>
                {!correction.correct && (
                  <p>
                    La bonne réponse était : <strong>{expectedLabel(question, correction)}</strong>
                  </p>
                )}
                <p>{correction.explanation}</p>
              </div>
            )}
          </div>
          {correction && (
            <button ref={continueRef} type="button" className="btn btn-primary" onClick={next} disabled={attempt.isFetching || complete.isPending}>
              {correction.readyToComplete ? 'Voir mon bilan' : 'Question suivante'}
            </button>
          )}
          {complete.error && <ErrorMessage error={complete.error} onRetry={() => complete.mutate()} />}
        </article>
      )}
    </section>
  );
}
