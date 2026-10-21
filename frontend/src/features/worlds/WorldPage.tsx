import { Link, useParams } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { get } from '../../api/client';
import type { MissionSummary, WorldDetail } from '../../api/types';
import { ErrorMessage, Loading } from '../../components/States';
import { Stars } from '../../components/Stars';

function MissionCard({ mission }: { mission: MissionSummary }) {
  const locked = mission.state === 'LOCKED';
  return (
    <li className={`card mission-card ${locked ? 'is-locked' : ''}`}>
      <div className="grow">
        <h4>
          Mission {mission.ordinal} · {mission.title}
        </h4>
        {locked ? (
          <p>Verrouillée : obtiens au moins une étoile à la mission précédente.</p>
        ) : (
          <p>
            <Stars count={mission.bestStars} size="sm" />{' '}
            {mission.completedAttempts > 0 ? `Meilleur score ${mission.bestCorrectCount}/5` : 'Pas encore jouée'}
          </p>
        )}
      </div>
      {!locked && (
        <Link className="btn btn-primary" to={`/missions/${mission.id}/jouer`}>
          {mission.inProgressAttemptId ? 'Reprendre' : mission.completedAttempts > 0 ? 'Rejouer' : 'Jouer'}
        </Link>
      )}
    </li>
  );
}

export function WorldPage() {
  const { slug } = useParams();
  const world = useQuery({ queryKey: ['worlds', slug], queryFn: () => get<WorldDetail>(`/worlds/${slug}`) });
  if (world.isPending) return <Loading />;
  if (world.error) return <ErrorMessage error={world.error} onRetry={() => world.refetch()} />;
  const { world: w, levels } = world.data;
  return (
    <section>
      <p>
        <Link to="/aventure">← Retour à la carte</Link>
      </p>
      <h1>{w.title}</h1>
      <p>{w.description}</p>
      {w.availability !== 'PLAYABLE' ? (
        <div className="panel">
          <p className="tag tag-soon">Bientôt disponible</p>
          <p>Ce monde est en préparation. En attendant, les Îles des Nombres et le Royaume des Mots t'attendent !</p>
        </div>
      ) : (
        <>
          <p>
            {w.completedMissions}/{w.missionCount} missions réussies · {w.stars}/{w.maxStars} étoiles
          </p>
          {levels.map((level) => (
            <section key={level.ordinal} className="level" aria-labelledby={`level-${level.ordinal}`}>
              <h2 id={`level-${level.ordinal}`}>
                Niveau {level.ordinal} · {level.title} {!level.unlocked && <span className="tag">Verrouillé</span>}
              </h2>
              <ul className="card-list">
                {level.missions.map((m) => (
                  <MissionCard key={m.id} mission={m} />
                ))}
              </ul>
            </section>
          ))}
        </>
      )}
    </section>
  );
}
