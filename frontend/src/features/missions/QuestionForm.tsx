import { useState, type FormEvent } from 'react';
import type { Answer, Correction, Question } from '../../api/types';

interface Props {
  question: Question;
  correction: Correction | null;
  pending: boolean;
  onSubmit: (answer: Answer) => void;
}

/** Saisie d'une réponse. Le formulaire ne connaît jamais la bonne réponse avant la correction serveur. */
export function QuestionForm({ question, correction, pending, onSubmit }: Props) {
  const [value, setValue] = useState('');
  const locked = pending || correction !== null;
  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (locked || value.trim() === '') return;
    if (question.type === 'MCQ') onSubmit({ optionId: value });
    else if (question.type === 'NUMBER') onSubmit({ number: value });
    else onSubmit({ text: value });
  };

  return (
    <form onSubmit={submit} className="question-form" noValidate>
      {question.type === 'MCQ' ? (
        <fieldset className="options" disabled={locked}>
          <legend className="question-prompt">{question.prompt}</legend>
          {question.options!.map((o) => {
            const isExpected = correction?.expectedAnswer.optionId === o.id;
            const isChosen = value === o.id;
            const cls = ['option', isChosen ? 'chosen' : '', correction && isExpected ? 'expected' : '', correction && isChosen && !correction.correct ? 'wrong' : '']
              .filter(Boolean)
              .join(' ');
            return (
              <label key={o.id} className={cls}>
                <input type="radio" name={`q-${question.id}`} value={o.id} checked={isChosen} onChange={() => setValue(o.id)} />
                <span>{o.label}</span>
                {correction && isExpected && <span className="sr-only"> (bonne réponse)</span>}
              </label>
            );
          })}
        </fieldset>
      ) : (
        <div className="field">
          <label htmlFor={`answer-${question.id}`} className="question-prompt">
            {question.prompt}
          </label>
          <div className="answer-row">
            <input
              id={`answer-${question.id}`}
              type="text"
              inputMode={question.type === 'NUMBER' ? 'decimal' : 'text'}
              autoComplete="off"
              autoCapitalize="off"
              autoCorrect="off"
              spellCheck={false}
              maxLength={100}
              value={value}
              disabled={locked}
              onChange={(e) => setValue(e.target.value)}
              aria-describedby={question.unit ? `unit-${question.id}` : undefined}
            />
            {question.unit && (
              <span id={`unit-${question.id}`} className="unit">
                {question.unit}
              </span>
            )}
          </div>
          {question.type === 'NUMBER' && <p className="field-help">Écris un nombre. Tu peux utiliser la virgule.</p>}
        </div>
      )}
      {!correction && (
        <button type="submit" className="btn btn-primary" disabled={locked || value.trim() === ''}>
          {pending ? 'Vérification…' : 'Valider'}
        </button>
      )}
    </form>
  );
}

export function expectedLabel(question: Question, correction: Correction): string {
  const e = correction.expectedAnswer;
  if (e.optionId) return question.options?.find((o) => o.id === e.optionId)?.label ?? e.optionId;
  if (e.number) return `${e.number}${question.unit ? ` ${question.unit}` : ''}`;
  return e.text ?? '';
}
