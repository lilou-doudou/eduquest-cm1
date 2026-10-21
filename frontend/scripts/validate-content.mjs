// Validation hors base du dossier content/ : schémas JSON puis cohérence croisée
// (références monde/niveau/compétence/asset, 6 missions de 5 exercices par monde jouable, clés de réponse).
// Le backend refait ces contrôles à l'import (ContentImporter) ; ce script sert à la CI et aux auteurs.
import Ajv2020 from 'ajv/dist/2020.js';
import { readFileSync, readdirSync } from 'node:fs';
import { join, relative } from 'node:path';

const root = join(import.meta.dirname, '..', '..', 'content');
const read = (p) => JSON.parse(readFileSync(join(root, p), 'utf8'));
const ajv = new Ajv2020({ allErrors: true, strict: true, strictRequired: false, strictTypes: false });
const schema = (name) => ajv.compile(read(`schema/${name}.schema.json`));
const errors = [];
const check = (validate, data, file) => {
  if (!validate(data)) for (const e of validate.errors) errors.push(`${file}${e.instancePath} : ${e.message}`);
};

const worlds = read('worlds/worlds.json');
const rewards = read('rewards/rewards.json');
const assets = read('assets-manifest.json');
check(schema('worlds'), worlds, 'worlds/worlds.json');
check(schema('rewards'), rewards, 'rewards/rewards.json');
check(schema('assets-manifest'), assets, 'assets-manifest.json');

const assetKeys = new Set(assets.assets.map((a) => a.key));
const skills = new Map(worlds.skills.map((s) => [s.code, s]));
const playable = new Map(worlds.worlds.filter((w) => w.availability === 'PLAYABLE').map((w) => [w.slug, w]));
for (const w of worlds.worlds) if (!assetKeys.has(w.assetKey)) errors.push(`monde ${w.slug} : asset ${w.assetKey} absent du manifeste`);
for (const r of rewards.rewards) if (!assetKeys.has(r.assetKey)) errors.push(`récompense ${r.code} : asset ${r.assetKey} absent du manifeste`);
const dup = (list, label) => list.filter((x, i) => list.indexOf(x) !== i).forEach((x) => errors.push(`${label} en double : ${x}`));
dup(worlds.worlds.map((w) => w.slug), 'monde');
dup(worlds.skills.map((s) => s.code), 'compétence');
dup(rewards.rewards.map((r) => r.code), 'récompense');

const validateMission = schema('mission');
const missions = [];
for (const dir of ['maths', 'english', 'geometrie', 'geographie', 'sciences', 'physique']) {
  for (const name of readdirSync(join(root, dir)).filter((f) => f.endsWith('.json')).sort()) {
    const file = `${dir}/${name}`;
    const m = read(file);
    check(validateMission, m, file);
    missions.push({ file, m });
    const world = playable.get(m.world);
    if (!world) { errors.push(`${file} : monde ${m.world} inconnu ou non jouable`); continue; }
    if (!world.levels.some((l) => l.ordinal === m.level)) errors.push(`${file} : niveau ${m.level} absent de ${m.world}`);
    m.exercises?.forEach((ex, i) => {
      const where = `${file} exercice ${i + 1}`;
      const skill = skills.get(ex.skill);
      if (!skill) errors.push(`${where} : compétence ${ex.skill} inconnue`);
      else if (skill.subjectCode !== world.subjectCode) errors.push(`${where} : compétence ${ex.skill} hors matière ${world.subjectCode}`);
      if (ex.type === 'MCQ') {
        const ids = ex.options.map((o) => o.id);
        dup(ids, `${where} option`);
        dup(ex.options.map((o) => o.label.trim().toLowerCase()), `${where} libellé`);
        if (!ids.includes(ex.answer.optionId)) errors.push(`${where} : la bonne réponse ${ex.answer.optionId} n'est pas une option`);
      }
      if (ex.type === 'TEXT') dup(ex.answer.accepted.map((a) => a.trim().toLowerCase()), `${where} variante`);
    });
  }
}
dup(missions.map(({ m }) => m.code), 'code de mission');
dup(missions.map(({ m }) => `${m.world}/${m.level}/${m.ordinal}`), 'position de mission');
const exerciseCount = missions.reduce((n, { m }) => n + (m.exercises?.length ?? 0), 0);
for (const slug of playable.keys()) {
  const n = missions.filter(({ m }) => m.world === slug).length;
  if (n !== 6) errors.push(`monde ${slug} : 6 missions attendues, ${n} trouvées`);
}
if (missions.length !== playable.size * 6) errors.push(`${playable.size * 6} missions attendues, ${missions.length} trouvées`);
if (exerciseCount !== missions.length * 5) errors.push(`${missions.length * 5} exercices attendus, ${exerciseCount} trouvés`);

if (errors.length) {
  console.error(`Contenus invalides (${errors.length}) :\n- ${errors.join('\n- ')}`);
  process.exit(1);
}
console.log(`Contenus valides : ${playable.size} mondes jouables sur ${worlds.worlds.length}, ${missions.length} missions, ${exerciseCount} exercices, ${skills.size} compétences, ${rewards.rewards.length} récompenses (${relative(process.cwd(), root)}).`);
