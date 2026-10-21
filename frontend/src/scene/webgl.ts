/** Détecte un contexte WebGL utilisable (sinon carte 2D équivalente). */
export function hasWebGL(): boolean {
  try {
    const canvas = document.createElement('canvas');
    return Boolean(canvas.getContext('webgl2') ?? canvas.getContext('webgl'));
  } catch {
    return false;
  }
}

export const WORLD_COLORS: Record<string, { ground: string; top: string; accent: string }> = {
  'world-numbers': { ground: '#8a5a3b', top: '#4caf7a', accent: '#2f7dd1' },
  'world-words': { ground: '#7d5a8a', top: '#f29fd0', accent: '#c2569b' },
  'world-shapes': { ground: '#6b6b7a', top: '#9fc4e8', accent: '#5a7fa8' },
  'world-geography': { ground: '#7a6a4a', top: '#e8d48a', accent: '#c9a23a' },
  'world-life': { ground: '#5a4a3a', top: '#2f8f4f', accent: '#1f6b3a' },
  'world-inventors': { ground: '#5a5a6a', top: '#f0a35e', accent: '#c96b2a' },
};

/** Positions fixes des îles (anneau), partagées entre la scène 3D et la carte 2D. */
export function islandPosition(index: number, count: number): [number, number] {
  const angle = (index / count) * Math.PI * 2 - Math.PI / 2;
  return [Math.cos(angle) * 5.2, Math.sin(angle) * 3.4];
}
