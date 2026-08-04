import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Provider } from 'urql'
import { client } from './graphql/client'
import App from './App.tsx'
import './index.css'

import { ErrorBoundary } from './components/ErrorBoundary'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ErrorBoundary>
      <Provider value={client}>
        <App />
      </Provider>
    </ErrorBoundary>
  </StrictMode>,
)
