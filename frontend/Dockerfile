FROM nginx:1.31-alpine

# Remove default nginx static assets
RUN rm -rf /usr/share/nginx/html/*

# Copy static frontend assets
COPY index.html /usr/share/nginx/html/
COPY styles.css /usr/share/nginx/html/
COPY app.js /usr/share/nginx/html/

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -q -O - http://localhost/ || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
