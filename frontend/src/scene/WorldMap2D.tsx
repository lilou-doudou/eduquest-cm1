import { Link } from 'react-router';
import type { WorldSummary } from '../api/types';
import { WORLD_COLORS, islandPosition } from './webgl';

/** Carte 2D équivalente (SVG + liens focalisables) utilisée sans WebGL ou sur matériel lent. */
export function WorldMap2D({ worlds }: { worlds: WorldSummary[] }) {
  return (
    <svg className="map2d" viewBox="-8 -5.5 16 11" role="group" aria-label="Carte des mondes (2D)">
      <rect x="-8" y="-5.5" width="16" height="11" fill="#bfe6ff" rx="0.6" />
      {worlds.map((w, i) => {
        const [x, y] = islandPosition(i, worlds.length);
        const playable = w.availability === 'PLAYABLE';
        const c = WORLD_COLORS[w.assetKey] ?? WORLD_COLORS['world-numbers'];
        const name = w.title.replace(/^(Les |Le |La |L')/, '');
        // Pas d'aria-label : le nom accessible est le texte visible de l'île (WCAG 2.5.3).
        return (
          <Link key={w.slug} to={`/mondes/${w.slug}`}>
            <g className="map2d-island" transform={`translate(${x} ${y})`}>
              <ellipse rx="1.6" ry="0.9" cy="0.25" fill={playable ? c.ground : '#8d8d96'} />
              <ellipse rx="1.6" ry="0.9" fill={playable ? c.top : '#c9c9d2'} />
              <text y="1.75" textAnchor="middle" fontSize="0.42" fill="#1d2433" fontWeight="700">
                {name}
              </text>
              {!playable && (
                <text y="0.12" textAnchor="middle" fontSize="0.3" fill="#3b3b48">
                  Bientôt disponible
                </text>
              )}
              {playable && (
                <text y="0.15" textAnchor="middle" fontSize="0.42" fill="#1d2433">
                  {w.stars}/{w.maxStars} ★
                </text>
              )}
            </g>
          </Link>
        );
      })}
    </svg>
  );
}
