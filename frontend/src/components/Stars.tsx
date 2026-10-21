export function Stars({ count, max = 3, size = 'md' }: { count: number; max?: number; size?: 'sm' | 'md' | 'lg' }) {
  return (
    <span className={`stars stars-${size}`} role="img" aria-label={`${count} étoile${count > 1 ? 's' : ''} sur ${max}`}>
      {Array.from({ length: max }, (_, i) => (
        <svg key={i} viewBox="0 0 24 24" aria-hidden="true" className={i < count ? 'star on' : 'star off'}>
          <path d="M12 2.5l2.9 6.1 6.6.8-4.9 4.6 1.3 6.6L12 17.3l-5.9 3.3 1.3-6.6-4.9-4.6 6.6-.8z" />
        </svg>
      ))}
    </span>
  );
}
