import { Client, cacheExchange, fetchExchange, subscriptionExchange } from 'urql';
import { createClient as createWSClient } from 'graphql-ws';

const wsClient = createWSClient({
  // Connect directly to the backend in dev to bypass Vite's WS proxy issues.
  // In production, window.location.host would point to the same server.
  url: import.meta.env.DEV
    ? 'ws://localhost:8080/graphql-ws'
    : `ws://${window.location.host}/graphql-ws`,
  // Only connect when the first subscription is made, not on page load
  lazy: true,
});

export const client = new Client({
  url: '/graphql',
  preferGetMethod: false,
  fetchOptions: {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
  },
  exchanges: [
    cacheExchange,
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
