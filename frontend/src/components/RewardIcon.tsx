import type { ReactElement } from 'react';
import type { Reward } from '../api/types';

const COLORS: Record<string, string> = {
  FIRST_STEP: '#3fa65b',
  PERFECT_MISSION: '#f2b632',
  NUMBERS_EXPLORER: '#2f7dd1',
  WORDS_FRIEND: '#c2569b',
  FOREST_GUARDIAN: '#1f6b3a',
  INVENTOR_GENIUS: '#c96b2a',
  SHAPES_MASTER: '#5a7fa8',
  GREAT_EXPLORER: '#c9a23a',
};

const SLOT_SHAPES: Record<string, ReactElement> = {
  HEAD: <path d="M10 26 Q24 6 38 26 Z" />,
  BACK: <path d="M12 12 Q24 4 36 12 L40 40 H8 Z" />,
  COMPANION: <circle cx="24" cy="26" r="13" />,
};

/** Icône de badge (médaille) ou d'accessoire. Grisée et marquée « à gagner » si non possédée. */
export function RewardIcon({ reward }: { reward: Pick<Reward, 'code' | 'type' | 'slot' | 'owned'> }) {
  const color = reward.owned ? (COLORS[reward.code] ?? '#7a5cc9') : '#b8b8c2';
  return (
    <svg viewBox="0 0 48 48" width="48" height="48" aria-hidden="true" className="reward-icon">
      {reward.type === 'BADGE' ? (
        <g>
          <path d="M16 2 L24 16 L32 2 Z" fill={reward.owned ? '#e84b4b' : '#cfcfd6'} />
          <circle cx="24" cy="28" r="16" fill={color} stroke="#ffffffaa" strokeWidth="3" />
          <path d="M24 19l2.6 5.4 5.9.7-4.4 4.1 1.2 5.8-5.3-2.9-5.3 2.9 1.2-5.8-4.4-4.1 5.9-.7z" fill="#ffffffcc" />
        </g>
      ) : (
        <g fill={reward.owned ? '#7a5cc9' : '#cfcfd6'}>{SLOT_SHAPES[reward.slot ?? 'HEAD']}</g>
      )}
    </svg>
  );
}
