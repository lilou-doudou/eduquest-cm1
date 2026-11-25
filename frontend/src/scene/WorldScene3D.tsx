import { useRef, useState } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import type { Group } from 'three';
import type { WorldSummary } from '../api/types';
import { WORLD_COLORS, islandPosition } from './webgl';

interface Props {
  worlds: WorldSummary[];
  reducedMotion: boolean;
  onSelect: (slug: string) => void;
  onSlow: () => void;
}

function Island({ world, index, count, reducedMotion, onSelect }: { world: WorldSummary; index: number; count: number; reducedMotion: boolean; onSelect: (slug: string) => void }) {
  const ref = useRef<Group>(null);
  const [hover, setHover] = useState(false);
  const [x, z] = islandPosition(index, count);
  const playable = world.availability === 'PLAYABLE';
  const colors = WORLD_COLORS[world.assetKey] ?? WORLD_COLORS['world-numbers'];
  useFrame(({ clock }) => {
    if (!ref.current || reducedMotion) return;
    ref.current.position.y = Math.sin(clock.elapsedTime * 0.8 + index) * 0.15;
  });
  const top = playable ? colors.top : '#b9b9c4';
  return (
    <group
      ref={ref}
      position={[x, 0, z]}
      scale={hover && playable ? 1.08 : 1}
      onClick={(e) => {
        e.stopPropagation();
        onSelect(world.slug);
      }}
      onPointerOver={() => setHover(true)}
      onPointerOut={() => setHover(false)}
    >
      <mesh position={[0, -0.6, 0]}>
        <coneGeometry args={[1.3, 1.6, 7]} />
        <meshStandardMaterial color={playable ? colors.ground : '#8d8d96'} flatShading />
      </mesh>
      <mesh position={[0, 0.25, 0]} rotation={[Math.PI, 0, 0]}>
        <cylinderGeometry args={[1.35, 1.3, 0.3, 7]} />
        <meshStandardMaterial color={top} flatShading />
      </mesh>
      {playable ? (
        <>
          <mesh position={[0.3, 0.9, 0]}>
            <coneGeometry args={[0.35, 1, 6]} />
            <meshStandardMaterial color={colors.accent} flatShading />
          </mesh>
          <mesh position={[-0.5, 0.65, 0.3]}>
            <sphereGeometry args={[0.3, 8, 8]} />
            <meshStandardMaterial color="#ffd166" flatShading />
          </mesh>
          {world.completed && (
            <mesh position={[0, 1.7, 0]}>
              <octahedronGeometry args={[0.25]} />
              <meshStandardMaterial color="#f2b632" emissive="#8a6a00" />
            </mesh>
          )}
        </>
      ) : (
        <mesh position={[0, 0.75, 0]}>
          <boxGeometry args={[0.5, 0.6, 0.3]} />
          <meshStandardMaterial color="#6b6b78" />
        </mesh>
      )}
    </group>
  );
}

const WARMUP_MS = 1000;

/**
 * Mesure les images/s pendant 3 s, après 1 s de chauffe (compilation des shaders, chargement) ;
 * sous 30 i/s, on bascule vers la carte 2D.
 */
function FpsWatch({ onSlow }: { onSlow: () => void }) {
  const frames = useRef(0);
  const first = useRef<number | null>(null);
  const done = useRef(false);
  useFrame(() => {
    if (done.current) return;
    const now = performance.now();
    if (first.current === null) first.current = now;
    const elapsed = now - first.current - WARMUP_MS;
    if (elapsed < 0) return;
    frames.current++;
    if (elapsed > 3000) {
      done.current = true;
      if ((frames.current * 1000) / elapsed < 30) onSlow();
    }
  });
  return null;
}

export default function WorldScene3D({ worlds, reducedMotion, onSelect, onSlow }: Props) {
  return (
    <div className="scene3d" aria-hidden="true">
      <Canvas
        camera={{ position: [0, 9.5, 10.5], fov: 46 }}
        onCreated={({ camera }) => camera.lookAt(0, -0.6, 0)}
        frameloop={reducedMotion ? 'demand' : 'always'}
        dpr={[1, 2]}
      >
        <color attach="background" args={['#141a3c']} />
        <fog attach="fog" args={['#141a3c', 18, 32]} />
        <ambientLight intensity={0.7} />
        <directionalLight position={[5, 10, 5]} intensity={1.1} />
        {worlds.map((w, i) => (
          <Island key={w.slug} world={w} index={i} count={worlds.length} reducedMotion={reducedMotion} onSelect={onSelect} />
        ))}
        <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -1.6, 0]}>
          <circleGeometry args={[30, 48]} />
          <meshStandardMaterial color="#2a3a7a" />
        </mesh>
        {!reducedMotion && <FpsWatch onSlow={onSlow} />}
      </Canvas>
    </div>
  );
}
