import { useSyncExternalStore } from 'react';

// Préférences non sensibles conservées localement (aucune donnée d'apprentissage).
export interface Preferences {
  sound: boolean;
  reducedMotion: boolean;
  mapMode: 'auto' | '2d';
}

const KEY = 'eduquest.preferences';
const DEFAULTS: Preferences = { sound: false, reducedMotion: false, mapMode: 'auto' };

function read(): Preferences {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? { ...DEFAULTS, ...JSON.parse(raw) } : DEFAULTS;
  } catch {
    return DEFAULTS;
  }
}

let current = read();
const listeners = new Set<() => void>();

export function setPreferences(patch: Partial<Preferences>) {
  current = { ...current, ...patch };
  try {
    localStorage.setItem(KEY, JSON.stringify(current));
  } catch {
    // stockage indisponible : la préférence reste valable pour la session
  }
  listeners.forEach((l) => l());
}

export function usePreferences(): Preferences {
  return useSyncExternalStore(
    (l) => {
      listeners.add(l);
      return () => listeners.delete(l);
    },
    () => current,
  );
}

/** Réduction des animations : préférence de l'application OU réglage système prefers-reduced-motion. */
export function useReducedMotion(): boolean {
  const prefs = usePreferences();
  const system = useSyncExternalStore(
    (l) => {
      const mq = window.matchMedia?.('(prefers-reduced-motion: reduce)');
      mq?.addEventListener?.('change', l);
      return () => mq?.removeEventListener?.('change', l);
    },
    () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false,
  );
  return prefs.reducedMotion || system;
}

/** Petit signal sonore synthétisé (pas de fichier audio), seulement si l'utilisateur a activé le son. */
export function playFeedback(correct: boolean) {
  if (!current.sound) return;
  try {
    const ctx = new AudioContext();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.frequency.value = correct ? 660 : 330;
    gain.gain.setValueAtTime(0.08, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.3);
    osc.connect(gain).connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + 0.3);
  } catch {
    // audio indisponible
  }
}
