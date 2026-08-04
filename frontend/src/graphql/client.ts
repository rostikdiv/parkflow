import { Client, fetchExchange, subscriptionExchange, errorExchange } from 'urql';
import { createClient as createWSClient } from 'graphql-ws';
import { getStoredToken } from '../lib/auth';

let wsUrl = import.meta.env.DEV
  ? 'ws://localhost:8080/graphql-ws'
  : import.meta.env.VITE_WS_URL || `wss://${window.location.host}/graphql-ws`;

if (wsUrl && !wsUrl.endsWith('/graphql-ws')) {
  wsUrl = wsUrl.replace(/\/$/, '') + '/graphql-ws';
}

const wsClient = createWSClient({
  url: wsUrl,
  lazy: true,
  connectionParams: () => {
    const token = getStoredToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }
});

// We intentionally skip cacheExchange (urql v5 default) because:
// 1. Without __typename the normalised cache can't track entities → stale data after mutations.
// 2. Availability data must always be fresh (real-time parking state).
// Every query uses requestPolicy 'network-only' by default instead.
export const client = new Client({
  url: '/graphql',
  preferGetMethod: false,
  fetchOptions: () => {
    const token = getStoredToken();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return {
      method: 'POST',
      headers,
    };
  },
  exchanges: [
    errorExchange({
      onError: (error, operation) => {
        console.error(`GraphQL Error [${operation.kind}]:`, error);
        // We log errors clearly to the console.
        // Component-level error boundaries or useQuery error states will handle the UI.
      },
    }),
    fetchExchange,
    subscriptionExchange({
      forwardSubscription(request) {
        const input = { ...request, query: request.query || '' };
        return {
          subscribe(sink) {
            const unsubscribe = wsClient.subscribe(input, sink);
            return { unsubscribe };
          },
        };
      },
    }),
  ],
});
