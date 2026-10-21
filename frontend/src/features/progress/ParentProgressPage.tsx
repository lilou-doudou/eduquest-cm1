import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { get } from '../../api/client';
import type { ParentProgress, SkillStatus } from '../../api/types';
import { Avatar } from '../../components/Avatar';
import { Empty, ErrorMessage, Loading } from '../../components/States';

const STATUS: Record<SkillStatus, string> = {
  DISCOVERY: 'En découverte',
  TO_REVIEW: 'À retravailler',
  IN_PROGRESS: 'En progrès',
  CONSISTENT: 'Réussites régulières',
};

const SUBJECTS: Record<string, string> = { MATHS: 'maths', ENGLISH: 'anglais', SCIENCES: 'sciences', PHYSICS: 'technologie' };

const STATUS_LABELS: Record<string, string> = { COMPLETED: 'Terminée', IN_PROGRESS: 'En cours', ABANDONED: 'Abandonnée' };

const dateFmt = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' });

export function ParentProgressPage() {
  const { childId } = useParams();
  const [page, setPage] = useState(0);
  const progress = useQuery({
    queryKey: ['children', childId, 'progress', page],
    queryFn: () => get<ParentProgress>(`/children/${childId}/progress?page=${page}&size=10`),
    placeholderData: keepPreviousData,
  });
  if (progress.isPending) return <Loading />;
  if (progress.error) return <ErrorMessage error={progress.error} onRetry={() => progress.refetch()} />;
  const p = progress.data;
  const pages = Math.max(1, Math.ceil(p.history.totalElements / p.history.size));
  return (
    <section>
      <p>
        <Link to="/parent/enfants">← Ma famille</Link>
      </p>
      <div className="hud">
        <Avatar code={p.avatarCode} size={64} decorative />
        <div>
          <h1>Suivi de {p.nickname}</h1>
          <p>
            {p.totalStars} étoiles · {p.xpTotal} XP · rang d'aventurier {p.adventurerLevel}
          </p>
        </div>
      </div>

      <h2>Mondes</h2>
      <ul className="card-list">
        {p.worlds
          .filter((w) => w.availability === 'PLAYABLE')
          .map((w) => (
            <li key={w.slug} className="card">
              <strong>{w.title}</strong> : {w.completedMissions}/{w.missionCount} missions avec au moins une étoile, {w.stars}/{w.maxStars} étoiles
            </li>
          ))}
      </ul>

      <h2>Compétences</h2>
      <p className="field-help">
        Indicateurs d'entraînement calculés sur les réponses des cinq dernières parties terminées contenant chaque compétence. Ce
        ne sont pas des évaluations scolaires. Seuils : moins de 60 % « À retravailler », 60 à 79 % « En progrès », 80 % et plus
        « Réussites régulières » ; « En découverte » tant qu'il y a moins de cinq réponses.
      </p>
      <div className="table-wrap">
        <table>
          <caption className="sr-only">Indicateurs par compétence</caption>
          <thead>
            <tr>
              <th scope="col">Compétence</th>
              <th scope="col">Indicateur</th>
              <th scope="col">Réussite</th>
              <th scope="col">Réponses</th>
              <th scope="col">Parties</th>
              <th scope="col">Indices utilisés</th>
            </tr>
          </thead>
          <tbody>
            {p.skills.map((s) => (
              <tr key={s.code}>
                <th scope="row">
                  {s.label} <span className="muted">({SUBJECTS[s.subjectCode] ?? s.subjectCode})</span>
                </th>
                <td>
                  <span className={`status status-${s.status.toLowerCase()}`}>{STATUS[s.status]}</span>
                </td>
                <td>{s.successRate === null ? '–' : `${s.successRate} %`}</td>
                <td>{s.evaluatedCount}</td>
                <td>{s.attemptCount}</td>
                <td>
                  {s.hintsUsed}
                  {s.hintsUsed > 0 && <span className="muted"> ({s.correctWithHint} réussies avec indice)</span>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h2>Pistes de révision</h2>
      {p.suggestions.length === 0 ? (
        <Empty>Rien à signaler pour l'instant.</Empty>
      ) : (
        <ul>
          {p.suggestions.map((s, i) => (
            <li key={i}>
              <strong>{s.label}</strong> : {s.detail}
            </li>
          ))}
        </ul>
      )}

      <h2>Parties récentes</h2>
      {p.history.items.length === 0 ? (
        <Empty>Aucune partie jouée.</Empty>
      ) : (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th scope="col">Date</th>
                  <th scope="col">Mission</th>
                  <th scope="col">État</th>
                  <th scope="col">Score</th>
                  <th scope="col">Étoiles</th>
                  <th scope="col">Indices</th>
                </tr>
              </thead>
              <tbody>
                {p.history.items.map((h) => (
                  <tr key={h.attemptId}>
                    <td>{dateFmt.format(new Date(h.startedAt))}</td>
                    <td>
                      {h.missionTitle} <span className="muted">({h.worldTitle})</span>
                    </td>
                    <td>{STATUS_LABELS[h.status] ?? h.status}</td>
                    <td>{h.correctCount === null ? '–' : `${h.correctCount}/5`}</td>
                    <td>{h.stars ?? '–'}</td>
                    <td>{h.hintsUsed}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav className="pager" aria-label="Pages de l'historique">
            <button type="button" className="btn btn-secondary" disabled={page === 0} onClick={() => setPage((x) => x - 1)}>
              Précédent
            </button>
            <span>
              Page {page + 1} / {pages}
            </span>
            <button type="button" className="btn btn-secondary" disabled={page + 1 >= pages} onClick={() => setPage((x) => x + 1)}>
              Suivant
            </button>
          </nav>
        </>
      )}
    </section>
  );
}
