FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install --production
COPY . .
ENV API_URL=http://parkflow:8080/api/internal/v1/sensor-events
ENV BATCH_INTERVAL_MS=5000
CMD ["node", "index.js"]
