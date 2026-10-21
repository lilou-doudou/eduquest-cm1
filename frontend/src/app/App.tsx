import { useEffect, useState } from 'react';
import { BrowserRouter, Link, Route, Routes, useNavigate, useParams } from 'react-router';
import { QueryClient, QueryClientProvider, useQueryClient } from '@tanstack/react-query';
import { ApiError, onSessionExpired } from '../api/client';
import { Layout } from '../components/Layout';
import { RequireRole } from '../components/Guards';
import { HomePage } from '../features/auth/HomePage';
import { LoginPage } from '../features/auth/LoginPage';
import { RegisterPage } from '../features/auth/RegisterPage';
import { ForgotPasswordPage, ResetPasswordPage, VerifyEmailPage } from '../features/auth/TokenPages';
import { ChildrenPage } from '../features/family/ChildrenPage';
import { ProfilesPage } from '../features/family/ProfilesPage';
import { PlayPage } from '../features/missions/PlayPage';
import { SummaryPage } from '../features/missions/SummaryPage';
import { NoticePage } from '../features/privacy/NoticePage';
import { PrivacyPage } from '../features/privacy/PrivacyPage';
import { ParentProgressPage } from '../features/progress/ParentProgressPage';
import { AdventurerPage } from '../features/rewards/AdventurerPage';
import { SettingsPage } from '../features/settings/SettingsPage';
import { AdventurePage } from '../features/worlds/AdventurePage';
import { WorldPage } from '../features/worlds/WorldPage';
import { ME_KEY } from './session';

export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: (count, error) => !(error instanceof ApiError && error.status >= 400 && error.status < 500) && count < 2,
        refetchOnWindowFocus: false,
      },
      mutations: { retry: false },
    },
  });
}

function SessionWatcher() {
  const qc = useQueryClient();
  const navigate = useNavigate();
  useEffect(
    () =>
      onSessionExpired(() => {
        const hadSession = Boolean(qc.getQueryData(ME_KEY));
        qc.setQueryData(ME_KEY, null);
        qc.removeQueries({ predicate: (q) => q.queryKey[0] !== 'me' });
        if (hadSession) navigate('/connexion?expiree=1', { replace: true });
      }),
    [qc, navigate],
  );
  return null;
}

function NotFound() {
  return (
    <section>
      <h1>Page introuvable</h1>
      <p>Ce chemin n'existe pas ou n'est plus disponible.</p>
      <Link to="/">Retour à l'accueil</Link>
    </section>
  );
}

/** Une clé par mission : passer d'une mission à l'autre démarre une nouvelle partie proprement. */
function PlayRoute() {
  const { id } = useParams();
  return <PlayPage key={id} />;
}

export function AppRoutes() {
  return (
    <Layout>
      <SessionWatcher />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/connexion" element={<LoginPage />} />
        <Route path="/inscription" element={<RegisterPage />} />
        <Route path="/verifier-email" element={<VerifyEmailPage />} />
        <Route path="/mot-de-passe-oublie" element={<ForgotPasswordPage />} />
        <Route path="/reinitialiser-mot-de-passe" element={<ResetPasswordPage />} />
        <Route path="/confidentialite" element={<NoticePage />} />
        <Route path="/parametres" element={<SettingsPage />} />

        <Route path="/profils" element={<RequireRole role="PARENT"><ProfilesPage /></RequireRole>} />
        <Route path="/parent/enfants" element={<RequireRole role="PARENT"><ChildrenPage /></RequireRole>} />
        <Route path="/parent/suivi/:childId" element={<RequireRole role="PARENT"><ParentProgressPage /></RequireRole>} />
        <Route path="/parent/confidentialite" element={<RequireRole role="PARENT"><PrivacyPage /></RequireRole>} />

        <Route path="/aventure" element={<RequireRole role="CHILD"><AdventurePage /></RequireRole>} />
        <Route path="/mondes/:slug" element={<RequireRole role="CHILD"><WorldPage /></RequireRole>} />
        <Route path="/missions/:id/jouer" element={<RequireRole role="CHILD"><PlayRoute /></RequireRole>} />
        <Route path="/parties/:id/bilan" element={<RequireRole role="CHILD"><SummaryPage /></RequireRole>} />
        <Route path="/mon-aventurier" element={<RequireRole role="CHILD"><AdventurerPage /></RequireRole>} />

        <Route path="*" element={<NotFound />} />
      </Routes>
    </Layout>
  );
}

export function App() {
  const [client] = useState(createQueryClient);
  return (
    <QueryClientProvider client={client}>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </QueryClientProvider>
  );
}
