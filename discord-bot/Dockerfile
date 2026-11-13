FROM node:24-slim
WORKDIR /app
ENV NODE_ENV=production
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY src ./src
COPY site ./site
ENV DB_PATH=/app/data/jojoha.db
EXPOSE 8080
CMD ["node", "src/index.js"]
