import { lazy, Suspense, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { get } from '../../api/client';
import type { ChildProgress, WorldSummary } from '../../api/types';
import { setPreferences, usePreferences, useReducedMotion } from '../../app/preferences';
import { Avatar } from '../../components/Avatar';
import { ErrorBoundary } from '../../components/ErrorBoundary';
import { ProgressBar } from '../../components/ProgressBar';
import { ErrorMessage, Loading } from '../../components/States';
import { Stars } from '../../components/Stars';
import { WorldMap2D } from '../../scene/WorldMap2D';
import { hasWebGL } from '../../scene/webgl';

// La scène 3D est chargée à la demande : l'apprentissage ne dépend jamais de son chargement.
const WorldScene3D = lazy(() => import('../../scene/WorldScene3D'));

export const WORLDS_KEY = ['worlds'];
export const MY_PROGRESS_KEY = ['me', 'progress'];

export function AdventurePage() {
  const worlds = useQuery({ queryKey: WORLDS_KEY, queryFn: () => get<WorldSummary[]>('/worlds') });
  const progress = useQuery({ queryKey: MY_PROGRESS_KEY, queryFn: () => get<ChildProgress>('/me/progress') });
  const prefs = usePreferences();
  const reducedMotion = useReducedMotion();
  const navigate = useNavigate();
  const webgl = useMemo(() => hasWebGL(), []);
  const [failed3d, setFailed3d] = useState(false);
  const use3d = webgl && prefs.mapMode === 'auto' && !failed3d;

  return (
    <section>
      {progress.data && (
        <div className="hud">
          <Avatar code={progress.data.avatarCode} size={64} decorative />
          <div className="grow">
            <h1>Bonjour {progress.data.nickname} !</h1>
            <p>
              Aventurier de rang <strong>{progress.data.adventurerLevel}</strong> · {progress.data.totalStars} étoiles
            </p>
            <ProgressBar value={progress.data.xpIntoLevel} max={progress.data.xpForNextLevel} label="XP jusqu'au prochain rang" />
          </div>
        </div>
      )}
      {!progress.data && <h1>La carte des mondes</h1>}

      {worlds.isPending && <Loading label="Chargement de la carte…" />}
      {worlds.error && <ErrorMessage error={worlds.error} onRetry={() => worlds.refetch()} />}
      {worlds.data && (
        <>
          <div className="map-wrap">
            {use3d ? (
              <ErrorBoundary fallback={<WorldMap2D worlds={worlds.data} />} onError={() => setFailed3d(true)}>
                <Suspense fallback={<WorldMap2D worlds={worlds.data} />}>
                  <WorldScene3D
                    worlds={worlds.data}
                    reducedMotion={reducedMotion}
                    onSelect={(slug) => navigate(`/mondes/${slug}`)}
                    onSlow={() => setPreferences({ mapMode: '2d' })}
                  />
                </Suspense>
              </ErrorBoundary>
            ) : (
              <WorldMap2D worlds={worlds.data} />
            )}
            <p className="map-caption">
              {use3d ? 'Carte 3D : clique sur une île, ou utilise la liste ci-dessous.' : 'Carte 2D : choisis une île ou un monde dans la liste.'}{' '}
              {webgl && (
                <button type="button" className="link-button" onClick={() => setPreferences({ mapMode: prefs.mapMode === '2d' ? 'auto' : '2d' })}>
                  {prefs.mapMode === '2d' ? 'Afficher la carte 3D' : 'Afficher la carte 2D'}
                </button>
              )}
            </p>
          </div>

          <h2>Les mondes</h2>
          <ul className="world-list">
            {worlds.data.map((w) => (
              <li key={w.slug} className={`card world-card ${w.availability === 'PLAYABLE' ? '' : 'is-locked'}`}>
                <h3>
                  <Link to={`/mondes/${w.slug}`}>{w.title}</Link>
                </h3>
                <p>{w.description}</p>
                {w.availability === 'PLAYABLE' ? (
                  <p className="world-meta">
                    <Stars count={Math.min(3, Math.round((w.stars / Math.max(1, w.maxStars)) * 3))} size="sm" /> {w.completedMissions}/
                    {w.missionCount} missions réussies {w.completed && <strong className="tag">Monde terminé</strong>}
                  </p>
                ) : (
                  <p className="tag tag-soon">Bientôt disponible</p>
                )}
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}
