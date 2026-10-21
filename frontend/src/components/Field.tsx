import { useId, type InputHTMLAttributes } from 'react';

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  help?: string;
  error?: string;
}

/** Champ de formulaire avec libellé explicite, aide et erreur reliées par aria-describedby. */
export function Field({ label, help, error, ...input }: Props) {
  const id = useId();
  const helpId = help ? `${id}-help` : undefined;
  const errorId = error ? `${id}-error` : undefined;
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {help && (
        <p id={helpId} className="field-help">
          {help}
        </p>
      )}
      <input id={id} aria-invalid={error ? true : undefined} aria-describedby={[helpId, errorId].filter(Boolean).join(' ') || undefined} {...input} />
      {error && (
        <p id={errorId} className="field-error">
          {error}
        </p>
      )}
    </div>
  );
}
