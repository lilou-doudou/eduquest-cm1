// Contrats DTO publics de l'API /api/v1 (voir docs/api/openapi.yaml).

export type Role = 'PARENT' | 'CHILD';

export interface Me {
  role: Role;
  parent: { id: string; email: string; emailVerified: boolean } | null;
  child: { id: string; nickname: string; avatarCode: string } | null;
}

export interface Child {
  id: string;
  nickname: string;
  avatarCode: string;
  grade: 'CM1';
  createdAt: string;
}

export type Availability = 'PLAYABLE' | 'COMING_SOON';

export interface WorldSummary {
  slug: string;
  title: string;
  subjectCode: string;
  displayOrder: number;
  availability: Availability;
  assetKey: string;
  description: string;
  missionCount: number;
  completedMissions: number;
  stars: number;
  maxStars: number;
  completed: boolean;
}

export type MissionState = 'LOCKED' | 'AVAILABLE' | 'COMPLETED';

export interface MissionSummary {
  id: string;
  code: string;
  ordinal: number;
  title: string;
  state: MissionState;
  bestStars: number;
  bestCorrectCount: number;
  completedAttempts: number;
  inProgressAttemptId: string | null;
}

export interface LevelSummary {
  ordinal: number;
  title: string;
  unlocked: boolean;
  missions: MissionSummary[];
}

export interface WorldDetail {
  world: WorldSummary;
  levels: LevelSummary[];
}

export type ExerciseType = 'MCQ' | 'NUMBER' | 'TEXT';

/** Question publique : ne contient jamais la bonne réponse ni l'explication. */
export interface Question {
  id: string;
  position: number;
  total: number;
  type: ExerciseType;
  instruction: string;
  prompt: string;
  options?: { id: string; label: string }[];
  unit?: string;
  hintUsed: boolean;
  hint?: string;
}

export interface Summary {
  attemptId: string;
  missionId: string;
  correctCount: number;
  questionCount: number;
  stars: number;
  hintsUsed: number;
  xpDelta: number;
  xpTotal: number;
  adventurerLevel: number;
  newRewards: string[];
  unlockedMissionIds: string[];
  skills: { code: string; label: string; correct: number; total: number }[];
  nextMissionId: string | null;
}

export interface AttemptView {
  attemptId: string;
  missionId: string;
  missionTitle: string;
  worldSlug: string;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED';
  answeredCount: number;
  total: number;
  readyToComplete: boolean;
  currentQuestion?: Question;
  summary?: Summary;
}

export type Answer = { optionId: string } | { number: string } | { text: string };

export interface Correction {
  exerciseId: string;
  correct: boolean;
  hintUsed: boolean;
  expectedAnswer: Partial<{ optionId: string; number: string; text: string }>;
  explanation: string;
  answeredCount: number;
  readyToComplete: boolean;
}

export interface ChildProgress {
  childId: string;
  nickname: string;
  avatarCode: string;
  xpTotal: number;
  adventurerLevel: number;
  xpIntoLevel: number;
  xpForNextLevel: number;
  totalStars: number;
  badges: { code: string; title: string; assetKey: string }[];
  worlds: WorldSummary[];
}

export type Slot = 'HEAD' | 'BACK' | 'COMPANION';

export interface Reward {
  code: string;
  type: 'BADGE' | 'COSMETIC';
  title: string;
  description: string;
  assetKey: string;
  slot: Slot | null;
  owned: boolean;
  grantedAt: string | null;
}

export interface RewardsResponse {
  rewards: Reward[];
  equipment: Partial<Record<Slot, string>>;
}

export type SkillStatus = 'DISCOVERY' | 'TO_REVIEW' | 'IN_PROGRESS' | 'CONSISTENT';

export interface SkillIndicator {
  code: string;
  label: string;
  subjectCode: string;
  status: SkillStatus;
  successRate: number | null;
  evaluatedCount: number;
  attemptCount: number;
  hintsUsed: number;
  correctWithHint: number;
}

export interface ParentProgress {
  childId: string;
  nickname: string;
  avatarCode: string;
  xpTotal: number;
  adventurerLevel: number;
  totalStars: number;
  worlds: WorldSummary[];
  skills: SkillIndicator[];
  history: {
    items: {
      attemptId: string;
      missionTitle: string;
      worldTitle: string;
      status: string;
      startedAt: string;
      completedAt: string | null;
      correctCount: number | null;
      stars: number | null;
      hintsUsed: number;
    }[];
    page: number;
    size: number;
    totalElements: number;
  };
  suggestions: { kind: 'SKILL' | 'MISSION'; label: string; detail: string }[];
}
