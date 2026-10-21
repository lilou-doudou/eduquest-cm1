import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { get } from '../../api/client';
import type { AttemptView, RewardsResponse } from '../../api/types';
import { useReducedMotion } from '../../app/preferences';
import { ErrorMessage, Loading } from '../../components/States';
import { Stars } from '../../components/Stars';

/** La célébration n'est montrée qu'une fois par partie (mémorisée localement). */
function useCelebrateOnce(attemptId: string) {
  const [celebrate] = useState(() => {
    try {
      const key = `eduquest.celebrated.${attemptId}`;
      if (localStorage.getItem(key)) return false;
      localStorage.setItem(key, '1');
      return true;
    } catch {
      return true;
    }
  });
  return celebrate;
}

export function SummaryPage() {
  const { id } = useParams();
  const attempt = useQuery({ queryKey: ['attempt', id], queryFn: () => get<AttemptView>(`/attempts/${id}`) });
  const rewards = useQuery({ queryKey: ['me', 'rewards'], queryFn: () => get<RewardsResponse>('/me/rewards') });
  const celebrate = useCelebrateOnce(id!);
  const reducedMotion = useReducedMotion();
  const [showConfetti, setShowConfetti] = useState(false);
  useEffect(() => {
    if (celebrate && !reducedMotion) {
      setShowConfetti(true);
      const t = setTimeout(() => setShowConfetti(false), 2500);
      return () => clearTimeout(t);
    }
  }, [celebrate, reducedMotion]);

  if (attempt.isPending) return <Loading />;
  if (attempt.error) return <ErrorMessage error={attempt.error} onRetry={() => attempt.refetch()} />;
  const s = attempt.data.summary;
  if (!s) {
    return (
      <section>
        <h1>Bilan indisponible</h1>
        <p>Cette mission n'est pas encore terminée.</p>
        <Link to={`/missions/${attempt.data.missionId}/jouer`}>Reprendre la mission</Link>
      </section>
    );
  }
  const titles = new Map(rewards.data?.rewards.map((r) => [r.code, r.title]));
  return (
    <section className="summary">
      {showConfetti && <div className="confetti" aria-hidden="true" />}
      <h1>Bilan : {attempt.data.missionTitle}</h1>
      <Stars count={s.stars} size="lg" />
      <p className="summary-score">
        {s.correctCount} bonne{s.correctCount > 1 ? 's' : ''} réponse{s.correctCount > 1 ? 's' : ''} sur {s.questionCount}
        {s.hintsUsed > 0 && ` (dont ${s.hintsUsed} question${s.hintsUsed > 1 ? 's' : ''} avec indice)`}
      </p>
      <p>
        {s.xpDelta > 0 ? (
          <>
            <strong>+{s.xpDelta} XP</strong> gagnés ! Total : {s.xpTotal} XP · rang {s.adventurerLevel}
          </>
        ) : (
          <>Pas de nouvel XP cette fois : améliore ton meilleur score pour en gagner.</>
        )}
      </p>
      {s.stars === 0 && <p>Il faut au moins 3 bonnes réponses pour obtenir une étoile et ouvrir la mission suivante. Tu peux réessayer !</p>}
      {s.newRewards.length > 0 && (
        <div className="panel">
          <h2>Nouvelles récompenses</h2>
          <ul>
            {s.newRewards.map((code) => (
              <li key={code}>{titles.get(code) ?? code}</li>
            ))}
          </ul>
          <Link to="/mon-aventurier">Voir mon aventurier</Link>
        </div>
      )}
      <h2>Compétences travaillées</h2>
      <ul className="skill-list">
        {s.skills.map((k) => (
          <li key={k.code}>
            {k.label} : {k.correct}/{k.total}
          </li>
        ))}
      </ul>
      <div className="actions">
        <Link className="btn btn-secondary" to={`/missions/${s.missionId}/jouer`}>
          Rejouer
        </Link>
        {s.nextMissionId && (
          <Link className="btn btn-primary" to={`/missions/${s.nextMissionId}/jouer`}>
            Mission suivante
          </Link>
        )}
        <Link className="btn btn-ghost" to={`/mondes/${attempt.data.worldSlug}`}>
          Retour au monde
        </Link>
      </div>
    </section>
  );
}
