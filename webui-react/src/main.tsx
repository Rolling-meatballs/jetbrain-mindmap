import { createRoot } from 'react-dom/client';
import { App } from './App';
import { installMockHost } from './host/mockHost';
import './index.css';

// In a real IDE host (VS Code / JetBrains JCEF) the host injects window.vscode
// and drives import/save. Only stand in with the mock when running standalone
// (browser dev) where no host bridge exists.
if (!window.vscode) {
  installMockHost();
}

createRoot(document.getElementById('root')!).render(<App />);
