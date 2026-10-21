import { Component, type ReactNode } from 'react';

/** Isole une zone fragile (scène 3D) : en cas d'erreur, on affiche le repli sans casser la page. */
export class ErrorBoundary extends Component<{ fallback: ReactNode; onError?: () => void; children: ReactNode }, { failed: boolean }> {
  state = { failed: false };

  static getDerivedStateFromError() {
    return { failed: true };
  }

  componentDidCatch() {
    this.props.onError?.();
  }

  render() {
    return this.state.failed ? this.props.fallback : this.props.children;
  }
}
