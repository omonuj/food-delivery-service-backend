# API Docs (Swagger UI) — Vercel static site

This folder is a self-contained Swagger UI site for the **Order Service** REST API.

- `openapi.json` — the OpenAPI 3.0 spec (create order, track order).
- `index.html` — Swagger UI that loads `openapi.json`.

The repo root `vercel.json` serves this folder as the deployed site.

## Deploy to Vercel

From the repository root:

```bash
npm i -g vercel     # or: npx vercel@latest
vercel login        # interactive, one-time
vercel --prod       # deploys and prints the public URL
```

Vercel serves the `docs/` folder as a static site (no build step, no backend).
The public URL shows the full API reference. The **"Try it out"** button will only
execute if you point the spec's `servers` block at a live, hosted Order Service.

## Live spec (when running the backend)

With the Order Service running (`localhost:8181`), springdoc also serves the spec live:

- Swagger UI:  http://localhost:8181/swagger-ui.html
- OpenAPI JSON: http://localhost:8181/v3/api-docs

To refresh `openapi.json` from a running instance:

```bash
curl http://localhost:8181/v3/api-docs -o docs/openapi.json
```
