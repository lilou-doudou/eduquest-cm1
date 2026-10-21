import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { put, get } from '../../api/client';
import type { ChildProgress, RewardsResponse, Slot } from '../../api/types';
import { Avatar } from '../../components/Avatar';
import { ProgressBar } from '../../components/ProgressBar';
import { RewardIcon } from '../../components/RewardIcon';
import { ErrorMessage, Loading } from '../../components/States';

const SLOT_LABELS: Record<Slot, string> = { HEAD: 'Tête', BACK: 'Dos', COMPANION: 'Compagnon' };

export function AdventurerPage() {
  const qc = useQueryClient();
  const progress = useQuery({ queryKey: ['me', 'progress'], queryFn: () => get<ChildProgress>('/me/progress') });
  const rewards = useQuery({ queryKey: ['me', 'rewards'], queryFn: () => get<RewardsResponse>('/me/rewards') });
  const equip = useMutation({
    mutationFn: ({ slot, code }: { slot: Slot; code: string | null }) => put<RewardsResponse>(`/me/equipment/${slot}`, { rewardCode: code }),
    onSuccess: (data) => qc.setQueryData(['me', 'rewards'], data),
  });

  if (progress.isPending || rewards.isPending) return <Loading />;
  if (progress.error) return <ErrorMessage error={progress.error} onRetry={() => progress.refetch()} />;
  if (rewards.error) return <ErrorMessage error={rewards.error} onRetry={() => rewards.refetch()} />;
  const p = progress.data;
  const { rewards: list, equipment } = rewards.data;
  const badges = list.filter((r) => r.type === 'BADGE');
  const cosmetics = list.filter((r) => r.type === 'COSMETIC');

  return (
    <section>
      <h1>Mon aventurier</h1>
      <div className="hud">
        <Avatar code={p.avatarCode} size={140} equipment={equipment} />
        <div className="grow">
          <h2>{p.nickname}</h2>
          <p>
            Rang <strong>{p.adventurerLevel}</strong> · {p.xpTotal} XP · {p.totalStars} étoiles
          </p>
          <ProgressBar value={p.xpIntoLevel} max={p.xpForNextLevel} label="XP jusqu'au prochain rang" />
          <p className="field-help">Le rang est cosmétique : il n'ouvre pas de mission et ne mesure pas les compétences.</p>
        </div>
      </div>

      <h2>Badges</h2>
      <ul className="reward-grid">
        {badges.map((b) => (
          <li key={b.code} className={`card reward ${b.owned ? '' : 'is-locked'}`}>
            <RewardIcon reward={b} />
            <div>
              <h3>{b.title}</h3>
              <p>{b.description}</p>
              {!b.owned && <p className="tag">À gagner</p>}
            </div>
          </li>
        ))}
      </ul>

      <h2>Accessoires</h2>
      {equip.error && <ErrorMessage error={equip.error} />}
      <ul className="reward-grid">
        {cosmetics.map((c) => {
          const slot = c.slot!;
          const equipped = equipment[slot] === c.code;
          return (
            <li key={c.code} className={`card reward ${c.owned ? '' : 'is-locked'}`}>
              <RewardIcon reward={c} />
              <div className="grow">
                <h3>{c.title}</h3>
                <p>
                  {SLOT_LABELS[slot]} · {c.description}
                </p>
                {c.owned ? (
                  <button
                    type="button"
                    className={equipped ? 'btn btn-secondary' : 'btn btn-primary'}
                    aria-pressed={equipped}
                    disabled={equip.isPending}
                    onClick={() => equip.mutate({ slot, code: equipped ? null : c.code })}
                  >
                    {equipped ? 'Retirer' : 'Équiper'}
                  </button>
                ) : (
                  <p className="tag">À gagner</p>
                )}
              </div>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
