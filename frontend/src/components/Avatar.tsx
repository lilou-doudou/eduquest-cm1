import type { ReactElement } from 'react';
import type { Slot } from '../api/types';

// Illustrations vectorielles originales du projet (voir docs/assets-licenses.md).
export const AVATARS: Record<string, { label: string; fur: string; accent: string }> = {
  fox: { label: 'Renard', fur: '#e8793a', accent: '#fff4e6' },
  owl: { label: 'Hibou', fur: '#9a7552', accent: '#f4e2c8' },
  turtle: { label: 'Tortue', fur: '#5aa86b', accent: '#d9f2d0' },
  cat: { label: 'Chat', fur: '#8b8fa3', accent: '#eef0f6' },
  panda: { label: 'Panda', fur: '#f5f5f5', accent: '#2d2d2d' },
  rabbit: { label: 'Lapin', fur: '#d8b4d8', accent: '#fbeefb' },
};

const HEAD_ITEMS: Record<string, ReactElement> = {
  CAPTAIN_CAP: <path d="M30 26 Q50 8 70 26 L76 30 H24 Z" fill="#1f4e8c" />,
  RAINBOW_BAND: (
    <g>
      <rect x="26" y="24" width="48" height="5" rx="2" fill="#e84b4b" />
      <rect x="26" y="29" width="48" height="4" rx="2" fill="#f2b632" />
      <rect x="26" y="33" width="48" height="4" rx="2" fill="#3fa65b" />
    </g>
  ),
  STRAW_HAT: (
    <g>
      <ellipse cx="50" cy="28" rx="34" ry="6" fill="#e3c06b" />
      <path d="M34 28 Q50 4 66 28 Z" fill="#d9ad4a" />
    </g>
  ),
  FLOWER_CROWN: (
    <g>
      {[30, 40, 50, 60, 70].map((x, i) => (
        <circle key={x} cx={x} cy={26 - (i % 2) * 3} r="5" fill={['#f27aa8', '#f2b632', '#ffffff', '#9b7bf2', '#f27aa8'][i]} />
      ))}
    </g>
  ),
  SPACE_HELMET: <circle cx="50" cy="52" r="38" fill="#bfe3ff" fillOpacity="0.35" stroke="#7fa7c9" strokeWidth="3" />,
  MUSHROOM_HAT: (
    <g>
      <path d="M24 30 Q50 0 76 30 Z" fill="#d93d3d" />
      {[38, 52, 64].map((x, i) => (
        <circle key={x} cx={x} cy={20 + (i % 2) * 4} r="3.5" fill="#ffffff" />
      ))}
    </g>
  ),
  INVENTOR_GOGGLES: (
    <g>
      <rect x="24" y="40" width="52" height="4" rx="2" fill="#6b4a2b" />
      <circle cx="40" cy="45" r="8" fill="#bfe3ff" fillOpacity="0.6" stroke="#c99a2e" strokeWidth="3" />
      <circle cx="60" cy="45" r="8" fill="#bfe3ff" fillOpacity="0.6" stroke="#c99a2e" strokeWidth="3" />
    </g>
  ),
};

const BACK_ITEMS: Record<string, string> = {
  STAR_CAPE: '#3b3f9e',
  EXPLORER_BAG: '#8a5a2b',
  LEAF_CAPE: '#4f9d4a',
  BUTTERFLY_WINGS: '#f29fd0',
  NATURALIST_BAG: '#6f8f3a',
  PROPELLER_PACK: '#c96b2a',
};

const COMPANIONS: Record<string, string> = {
  OWL_FRIEND: '#9a7552',
  KITTEN_FRIEND: '#f0a35e',
  TURTLE_FRIEND: '#5aa86b',
  FOX_FRIEND: '#e8793a',
  STAR_RABBIT: '#c9b8f5',
  SQUIRREL_FRIEND: '#b8662f',
  ROBOT_FRIEND: '#9aa4b2',
};

interface Props {
  code: string;
  size?: number;
  equipment?: Partial<Record<Slot, string>>;
  decorative?: boolean;
}

export function Avatar({ code, size = 64, equipment = {}, decorative = false }: Props) {
  const a = AVATARS[code] ?? AVATARS.fox;
  const back = equipment.BACK ? BACK_ITEMS[equipment.BACK] : undefined;
  const companion = equipment.COMPANION ? COMPANIONS[equipment.COMPANION] : undefined;
  const ears =
    code === 'rabbit' ? (
      <g fill={a.fur}>
        <ellipse cx="38" cy="18" rx="6" ry="16" />
        <ellipse cx="62" cy="18" rx="6" ry="16" />
      </g>
    ) : code === 'turtle' ? null : code === 'owl' ? (
      <g fill={a.fur}>
        <path d="M26 26 L32 12 L40 26 Z" />
        <path d="M74 26 L68 12 L60 26 Z" />
      </g>
    ) : code === 'panda' ? (
      <g fill="#2d2d2d">
        <circle cx="30" cy="28" r="9" />
        <circle cx="70" cy="28" r="9" />
      </g>
    ) : (
      <g fill={a.fur}>
        <path d="M24 34 L30 12 L44 26 Z" />
        <path d="M76 34 L70 12 L56 26 Z" />
      </g>
    );
  return (
    <svg
      viewBox="0 0 100 100"
      width={size}
      height={size}
      className="avatar"
      role={decorative ? undefined : 'img'}
      aria-hidden={decorative ? true : undefined}
      aria-label={decorative ? undefined : `Avatar ${a.label}`}
    >
      {back && <path d="M22 60 Q50 30 78 60 L86 96 H14 Z" fill={back} />}
      {ears}
      <circle cx="50" cy="52" r="30" fill={a.fur} stroke="#00000022" strokeWidth="1.5" />
      <ellipse cx="50" cy="62" rx="16" ry="12" fill={code === 'panda' ? '#ffffff' : a.accent} />
      {code === 'panda' && (
        <g fill="#2d2d2d">
          <ellipse cx="39" cy="48" rx="7" ry="8" />
          <ellipse cx="61" cy="48" rx="7" ry="8" />
        </g>
      )}
      <circle cx="40" cy="48" r="4" fill={code === 'panda' ? '#ffffff' : '#2d2d2d'} />
      <circle cx="60" cy="48" r="4" fill={code === 'panda' ? '#ffffff' : '#2d2d2d'} />
      <ellipse cx="50" cy="58" rx="4" ry="3" fill="#2d2d2d" />
      <path d="M44 65 Q50 70 56 65" stroke="#2d2d2d" strokeWidth="2" fill="none" strokeLinecap="round" />
      {equipment.HEAD && HEAD_ITEMS[equipment.HEAD]}
      {companion && (
        <g>
          <circle cx="84" cy="84" r="12" fill={companion} stroke="#ffffff" strokeWidth="2" />
          <circle cx="80" cy="82" r="2" fill="#2d2d2d" />
          <circle cx="88" cy="82" r="2" fill="#2d2d2d" />
        </g>
      )}
    </svg>
  );
}
