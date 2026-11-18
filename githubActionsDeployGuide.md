# githubActionsDeployGuide.md — Package `lab-app-auth` + `lab-raj` into ONE Docker image on GHCR

Goal: every push to GitHub builds **one Docker image** that contains

- the **Angular** app `lab-app-auth` (built to static files), served by
- the **Spring Boot** app `lab-raj` (REST API + JWT security + SQLite),

and publishes it to **GitHub Container Registry (GHCR)** so anyone can run it with:

```bash
docker run -p 8117:8117 ghcr.io/isk6708/lab-raj-fullstack:latest
```

and open `http://localhost:8117` → the Angular **Login** page.

Repository: `https://github.com/isk6708/springboot-series` (working branch: `nov-2026`).

---

## 0. The big picture

```
 springboot-series/ (git repo)
 ├── lab-app-auth/          Angular 21  ──┐ stage 1: node:22   → npm ci + ng build
 ├── lab-raj/               Spring Boot ──┤ stage 2: temurin-17 JDK → copy Angular files into
 │                                        │          src/main/resources/static, mvnw package → JAR
 ├── Dockerfile             (new)        ─┤ stage 3: temurin-17 JRE → run the JAR only
 ├── .dockerignore          (new)         │
 └── .github/workflows/                   ▼
     └── lab-fullstack-docker.yml (new)  GitHub Actions → docker build → push to ghcr.io
```

Inside the running container:

```
 Browser ──▶ http://localhost:8117/                 → index.html   (Angular)
            http://localhost:8117/main-XXXX.js      → static file  (Angular)
            http://localhost:8117/login, /todos     → forwarded to index.html (Angular router)
            http://localhost:8117/api/v1/...        → Spring Boot REST controllers (JWT)
```

**One port, one origin** → the browser calls the API on the *same* address, so **no CORS** is
needed in production.

| Step | What | Where |
|---|---|---|
| 1 | Angular calls the API with a **relative** URL + dev proxy | `lab-app-auth` |
| 2 | Spring Boot serves Angular files & routes, config from env vars | `lab-raj` |
| 3 | Multi-stage `Dockerfile` + `.dockerignore` | repo root |
| 4 | Build & run the image locally | your PC |
| 5 | GitHub Actions workflow → GHCR | `.github/workflows/` |
| 6 | Push, watch the run, make the package public | GitHub |
| 7 | Download & run the image (docker run / compose) | anyone |
| 8 | Releases with version tags | GitHub |

---

## Step 1 — Angular: use a relative API URL (`lab-app-auth`)

### 1.1 `src/app/apis/api.config.ts`

Today:

```ts
export const API_BASE_URL = 'http://localhost:8117/api/v1';
```

Change to a **relative** path, so the app calls whatever server it was loaded from:

```ts
// Same origin as the page: works in Docker (one port) and in dev (via proxy, see 1.2)
export const API_BASE_URL = '/api/v1';
```

The interceptor checks `req.url.endsWith('/login')` / `'/users/register'` — still works with
relative URLs.

### 1.2 Keep `ng serve` working: dev proxy

During development Angular runs on `http://localhost:4230` (see `angular.json` → `serve.options.port`)
and Spring Boot on `8117`. A **proxy** makes `/api/...` calls from 4230 go to 8117.

Create `lab-app-auth/proxy.conf.json`:

```json
{
  "/api": {
    "target": "http://localhost:8117",
    "secure": false,
    "changeOrigin": true
  }
}
```

In `lab-app-auth/angular.json`, add `proxyConfig` to the **serve** options:

```json
"serve": {
  "builder": "@angular/build:dev-server",
  "options": {
    "port": 4230,
    "proxyConfig": "proxy.conf.json"
  },
```

Bonus: with the proxy, the browser also sees one origin in dev → CORS is not used at all.

### 1.3 Small clean-up

- `src/app/header/header.html` still shows `http://localhost:8116/api/v1/todos` → change the text to
  `/api/v1` (or remove the badge).
- `src/index.html` title is `LabRajApp` → rename if you like.

### 1.4 Know your build output folder

`angular.json` still calls the project **`lab-raj-app`** (it was copied from that folder), so:

```bash
cd lab-app-auth
npm ci
npx ng build            # production is the default configuration
```

produces: `lab-app-auth/dist/lab-raj-app/browser/` → `index.html`, `main-XXXX.js`, `styles-XXXX.css`,
`favicon.ico`, …

> If you rename the project in `angular.json` to `lab-app-auth`, the folder becomes
> `dist/lab-app-auth/browser/` — then update the path in the **Dockerfile** (Step 3) too.

### ✅ Check (Step 1)

1. Start Spring Boot (`lab-raj`, port 8117).
2. `npm start` in `lab-app-auth` → open `http://localhost:4230`.
3. Register / Login / Todos still work. DevTools → Network: requests go to
   `http://localhost:4230/api/v1/...` (the proxy forwards them).

---

## Step 2 — Spring Boot: serve Angular (`lab-raj`)

Spring Boot automatically serves anything inside `src/main/resources/static/` (in the JAR:
`classpath:/static/`). The Dockerfile will copy the Angular build there. Two things must be
adjusted so it works.

### 2.1 Security rules: protect only `/api/**`

Current `SecurityConfig.securityFilterChain` ends with:

```java
.requestMatchers("/", "/index.html", "/app.js", "/static/**", "/*.html", "/*.js", "/*.css").permitAll()
.requestMatchers("/api/v1/todos/**", "/api/v1/users/**").authenticated()
.anyRequest().authenticated()
```

Problems in the combined app:

- `favicon.ico`, `/media/**` (fonts/images from Angular) → **403**.
- Refreshing the browser on `/login`, `/register`, `/todos` → **403**, because those are Angular
  routes, not files, and `anyRequest().authenticated()` blocks them.

Change the end of the rules to:

```java
.authorizeHttpRequests(auth -> auth
    // public API
    .requestMatchers("/api/v1/login", "/api/v1/users/register").permitAll()
    // swagger (keep your existing list)
    .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
    // every other API call needs a valid JWT
    .requestMatchers("/api/**").authenticated()
    // everything else = Angular files and Angular routes → public
    .anyRequest().permitAll()
)
```

> Security is still enforced where it matters: **all data** lives under `/api/**`. The Angular
> files are public anyway (every visitor downloads them).

### 2.2 Forward Angular routes to `index.html`

When the user refreshes on `http://localhost:8117/todos`, Spring Boot looks for a controller or a
file called `todos` — there is none → 404. We forward such paths to `index.html` and let the
Angular router take over.

New file `lab-raj/src/main/java/com/heitech/lab_raj/controllers/SpaController.java`:

```java
package com.heitech.lab_raj.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Sends Angular routes (e.g. /login, /register, /todos, /users) to index.html.
 * The pattern matches ONE path segment without a dot, so:
 *   /todos          → index.html   (Angular route)
 *   /main-AB12.js   → not matched  (real static file, has a dot)
 *   /api/v1/todos   → not matched  (more than one segment → REST controller)
 */
@Controller
public class SpaController {

    @GetMapping("/{path:[^\\.]*}")
    public String forwardToAngular() {
        return "forward:/index.html";
    }
}
```

> Student exercise: the simplest alternative is an explicit list,
> `@GetMapping({"/login", "/register", "/todos", "/users"})` — easier to read, but you must
> remember to add every new Angular route.

### 2.3 Configuration from environment variables

Spring Boot reads environment variables automatically (relaxed binding), so **no code change** is
needed — we only decide the values in the Docker image / `docker run`:

| Property in `application.properties` | Environment variable | Value in Docker |
|---|---|---|
| `server.port=8117` | `SERVER_PORT` | `8117` (keep) |
| `spring.datasource.url=jdbc:sqlite:heitechdata.db` | `SPRING_DATASOURCE_URL` | `jdbc:sqlite:/app/data/heitechdata.db` (on a **volume**) |
| `jwt.secret=...` | `JWT_SECRET` | a long random secret (≥ 32 chars) given at `docker run` |

Why `/app/data` for SQLite? A container's file system is thrown away when the container is
removed. Putting the DB on a **volume** keeps users and todos between restarts and upgrades.

### 2.4 CORS stays for development only

Keep the `corsConfigurationSource` bean (`localhost:4200`, `localhost:4230`) — harmless. In the
combined image the browser never needs CORS (same origin).

### ✅ Check (Step 2) — without Docker

```bat
cd lab-app-auth
npx ng build
xcopy /E /I /Y dist\lab-raj-app\browser ..\lab-raj\src\main\resources\static
cd ..\lab-raj
mvnw.cmd spring-boot:run
```

Open `http://localhost:8117` → Login page. Register, log in, refresh on `/todos` → still works.
Afterwards **delete** the copied files from `src/main/resources/static/` (the Dockerfile does this
copy for us; we don't commit build output).

---

## Step 3 — `Dockerfile` and `.dockerignore` (repo root)

The Dockerfile lives at the **root** of `springboot-series` because the build needs **both**
folders.

### 3.1 `springboot-series/Dockerfile`

```dockerfile
# syntax=docker/dockerfile:1

# ───────────── Stage 1: build Angular (lab-app-auth) ─────────────
FROM node:22-alpine AS frontend
WORKDIR /build/lab-app-auth

# Install dependencies first (cached unless package*.json changes)
COPY lab-app-auth/package.json lab-app-auth/package-lock.json ./
RUN npm ci

# Build the production bundle (ng build does NOT run unit tests)
COPY lab-app-auth/ ./
RUN npx ng build --configuration production
# Output: /build/lab-app-auth/dist/lab-raj-app/browser

# Angular unit tests are skipped. To enable them later, uncomment:
# RUN npx ng test --watch=false


# ───────────── Stage 2: build Spring Boot (lab-raj) with Angular inside ─────────────
FROM eclipse-temurin:17-jdk AS backend
WORKDIR /build/lab-raj

# Maven wrapper + pom first (dependency download is cached)
COPY lab-raj/.mvn/ .mvn/
COPY lab-raj/mvnw lab-raj/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Source code + Angular static files
COPY lab-raj/src/ src/
COPY --from=frontend /build/lab-app-auth/dist/lab-raj-app/browser/ src/main/resources/static/

# Tests are SKIPPED: -Dmaven.test.skip=true = do not compile AND do not run tests
RUN ./mvnw -B -Dmaven.test.skip=true package
# Output: /build/lab-raj/target/lab-raj-0.0.1-SNAPSHOT.jar

# To run the backend tests during the image build later, use this line instead:
# RUN ./mvnw -B package


# ───────────── Stage 3: small runtime image ─────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

# Run as a normal user, not root
RUN useradd --system --create-home --uid 1001 appuser \
 && mkdir -p /app/data && chown -R appuser /app

COPY --from=backend /build/lab-raj/target/lab-raj-*.jar /app/app.jar

ENV SERVER_PORT=8117 \
    SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/heitechdata.db \
    JAVA_OPTS=""

VOLUME ["/app/data"]
EXPOSE 8117
USER appuser

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
```

Teaching points:

- **Multi-stage**: Node and the JDK are only used to *build*. The final image contains only the
  JRE + one JAR → smaller and safer.
- **Layer caching**: copying `package*.json` / `pom.xml` before the source code means dependencies
  are re-downloaded only when those files change.
- **Tests are skipped** on purpose (`-Dmaven.test.skip=true`, no `ng test`) so a broken or
  generated `*.spec.ts` / JUnit test never blocks the image. The lines to enable them are left as
  comments.
- `chmod +x mvnw`: the repo is edited on Windows (`filemode = false` in `.git/config`), so the
  executable bit may be missing on Linux.
- `eclipse-temurin:17-jre` (Ubuntu based) is used instead of an Alpine JRE — the SQLite JDBC
  driver ships native libraries and works out of the box on glibc.
- **`JWT_SECRET` is NOT baked into the image** — it is given at `docker run` time (Step 7).
  If you don't give one, the value from `application.properties` is used (OK for class, not for
  real deployments).

### 3.2 `springboot-series/.dockerignore`

Keeps the build context small and avoids copying local junk into the image:

```gitignore
# git & IDE
.git
**/.vscode
**/.idea

# Angular
**/node_modules
**/dist
**/.angular

# Spring Boot
**/target
**/*.db

# Other folders / files not needed in the image
lab-raj-app
*.pdf
*.pptx
*.txt
**/*.md
```

> `lab-raj/heitechdata.db` is tracked in git — `**/*.db` makes sure your local test data is
> **never** copied into the image. The container starts with an empty DB on its volume.

---

## Step 4 — Build & run the image on your PC first

Requires Docker Desktop. From the repo root (`springboot-series`):

```bash
docker build -t lab-raj-fullstack:local .

docker run --rm -p 8117:8117 \
  -v lab-raj-data:/app/data \
  -e JWT_SECRET=change-me-to-a-long-random-string-at-least-32-chars \
  lab-raj-fullstack:local
```

(Windows `cmd`: put everything on one line or use `^` instead of `\`.)

### ✅ Check (Step 4)

| Test | Expected |
|---|---|
| `http://localhost:8117/` | Angular Login page |
| Register → Login → Todos | Works |
| Refresh on `/todos` | Still the Todos page (SpaController) |
| `http://localhost:8117/api/v1/todos` in a new tab (no token) | `403` |
| `http://localhost:8117/swagger-ui.html` | Swagger UI |
| Stop container, run again with the same `-v lab-raj-data:/app/data` | Your user still exists |
| `docker images lab-raj-fullstack` | Roughly 250–300 MB (JRE + JAR) |

**Do not continue to Step 5 until this works** — debugging locally is much faster than on GitHub.

---

## Step 5 — GitHub Actions workflow

Create `springboot-series/.github/workflows/lab-fullstack-docker.yml`:

```yaml
name: Build & publish lab fullstack image (GHCR)

# ── MANUAL ONLY ──────────────────────────────────────────────────────────
# Runs ONLY when someone clicks "Run workflow" in the Actions tab.
# Pushes, pull requests and tags do NOT start it.
on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Optional version tag for the image (e.g. 1.0.0). Leave empty to skip.'
        required: false
        default: ''

# Automatic triggers are disabled on purpose. Uncomment to enable later:
#  push:
#    branches: [main, nov-2026]
#    paths:
#      - 'lab-app-auth/**'
#      - 'lab-raj/**'
#      - 'Dockerfile'
#      - '.dockerignore'
#      - '.github/workflows/lab-fullstack-docker.yml'
#    tags: ['v*']
#  pull_request:
#    branches: [main]

permissions:
  contents: read
  packages: write         # needed to push to ghcr.io with GITHUB_TOKEN

env:
  REGISTRY: ghcr.io
  # Must be lowercase. Result: ghcr.io/isk6708/lab-raj-fullstack
  IMAGE_NAME: ${{ github.repository_owner }}/lab-raj-fullstack

jobs:
  # ── TESTS: SKIPPED (commented out) ─────────────────────────────────────
  # Uncomment this job, and the "needs: test" line below, to run tests first.
  #
  # test:
  #   runs-on: ubuntu-latest
  #   steps:
  #     - uses: actions/checkout@v4
  #
  #     - name: Set up Java 17
  #       uses: actions/setup-java@v4
  #       with:
  #         distribution: temurin
  #         java-version: '17'
  #         cache: maven
  #
  #     - name: Backend tests (lab-raj)
  #       working-directory: lab-raj
  #       run: chmod +x mvnw && ./mvnw -B test
  #
  #     - name: Set up Node 22
  #       uses: actions/setup-node@v4
  #       with:
  #         node-version: '22'
  #         cache: npm
  #         cache-dependency-path: lab-app-auth/package-lock.json
  #
  #     - name: Frontend tests (lab-app-auth)
  #       working-directory: lab-app-auth
  #       run: npm ci && npx ng test --watch=false

  build-and-push:
    runs-on: ubuntu-latest
    # needs: test          # ← uncomment together with the test job above

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Docker metadata (tags & labels)
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=ref,event=branch
            type=sha,prefix=sha-
            type=raw,value=${{ inputs.version }},enable=${{ inputs.version != '' }}
            type=raw,value=latest,enable=${{ github.ref == 'refs/heads/main' || github.ref == 'refs/heads/nov-2026' }}

      - name: Build and push image (tests skipped inside Dockerfile)
        uses: docker/build-push-action@v6
        with:
          context: .
          file: ./Dockerfile
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Summary
        run: |
          echo "### 🐳 Image published" >> $GITHUB_STEP_SUMMARY
          echo '```' >> $GITHUB_STEP_SUMMARY
          echo "${{ steps.meta.outputs.tags }}" >> $GITHUB_STEP_SUMMARY
          echo '```' >> $GITHUB_STEP_SUMMARY
          echo "Run: \`docker run -p 8117:8117 ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest\`" >> $GITHUB_STEP_SUMMARY
```

Line by line, what students should understand:

| Part | Meaning |
|---|---|
| `on: workflow_dispatch` | **Manual only.** The workflow starts only from the **Run workflow** button. A `git push` does **not** build anything. |
| `inputs.version` | Optional box shown when you click *Run workflow*; e.g. `1.0.0` → extra image tag `:1.0.0`. |
| Commented `push:` / `pull_request:` | Automatic triggers kept as comments — uncomment to switch to auto-build later. |
| Commented `test:` job | **Tests are skipped.** Uncomment the job **and** `needs: test` to make the image build wait for passing tests. |
| `permissions.packages: write` | Allows the built-in `GITHUB_TOKEN` to push to GHCR — **no personal token or secret needed**. |
| `docker/login-action` | Logs in to `ghcr.io` as the user who triggered the run. |
| `docker/metadata-action` | Creates tags automatically and adds the `org.opencontainers.image.source` label, which **links the package to this repository** on GitHub. |
| Tags produced | run on `nov-2026` → `:nov-2026`, `:sha-1a2b3c4`, `:latest` · plus `:1.0.0` if you typed `1.0.0` in *version* |
| `cache-from/to: type=gha` | Re-uses Docker layers between runs → much faster second build. |
| `GITHUB_STEP_SUMMARY` | Shows the image name + run command on the run's summary page. |

> `latest` is set for both `main` and `nov-2026` because the course works on `nov-2026`. When
> `nov-2026` is merged into `main`, you can drop it from the `enable` condition.

---

## Step 6 — Push, watch the run, make the package public

### 6.1 Commit & push

```bash
cd springboot-series
git add Dockerfile .dockerignore .github/workflows/lab-fullstack-docker.yml \
        lab-app-auth/proxy.conf.json lab-app-auth/angular.json lab-app-auth/src \
        lab-raj/src
git commit -m "Package lab-app-auth + lab-raj into one Docker image on GHCR"
git push origin nov-2026
```

### 6.2 Watch the workflow

GitHub → repository **springboot-series** → **Actions** tab → *Build & publish lab fullstack
image (GHCR)* → open the run. Steps turn green ✅. The first run takes several minutes (npm +
Maven downloads); later runs are faster thanks to the cache.

### 6.3 Find the image

GitHub → your profile (`isk6708`) → **Packages** → `lab-raj-fullstack`
(also shown on the repository page, right-hand side, under **Packages**).

### 6.4 Make it downloadable by everyone

New GHCR packages are **private** by default. For students to `docker pull` without logging in:

1. Open the package `lab-raj-fullstack` → **Package settings** (right side).
2. **Danger Zone → Change visibility → Public** → type the package name to confirm.

(If the repository itself is private and you prefer to keep the package private, see 7.3.)

---

## Step 7 — Download & run the image (students / any machine)

### 7.1 `docker run`

```bash
docker pull ghcr.io/isk6708/lab-raj-fullstack:latest

docker run -d --name lab-raj \
  -p 8117:8117 \
  -v lab-raj-data:/app/data \
  -e JWT_SECRET=please-use-your-own-very-long-secret-32+ \
  ghcr.io/isk6708/lab-raj-fullstack:latest
```

Open `http://localhost:8117` → Login → **Register** your account → use the app.

Useful commands:

```bash
docker logs -f lab-raj        # see Spring Boot logs (SQL, errors)
docker stop lab-raj           # stop
docker start lab-raj          # start again (data kept in volume lab-raj-data)
docker rm -f lab-raj          # remove container (volume and data still kept)
docker volume rm lab-raj-data # ⚠ deletes all users/todos
```

Different port on the host, e.g. 9000: `-p 9000:8117` → open `http://localhost:9000`
(no rebuild needed — Angular uses relative URLs).

### 7.2 `docker-compose.yml` (optional, nicer for class)

```yaml
services:
  lab-raj:
    image: ghcr.io/isk6708/lab-raj-fullstack:latest
    container_name: lab-raj
    ports:
      - "8117:8117"
    environment:
      JWT_SECRET: please-use-your-own-very-long-secret-32+
    volumes:
      - lab-raj-data:/app/data
    restart: unless-stopped

volumes:
  lab-raj-data:
```

```bash
docker compose up -d
docker compose pull && docker compose up -d   # upgrade to the newest image
```

### 7.3 If the package is private

Each user needs a GitHub **Personal Access Token (classic)** with scope `read:packages`:

```bash
echo YOUR_TOKEN | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin
docker pull ghcr.io/isk6708/lab-raj-fullstack:latest
```

---

## Step 8 — Versioned releases (optional)

`latest` always moves. For a stable version students can rely on:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow publishes `ghcr.io/isk6708/lab-raj-fullstack:1.0.0` and `:1.0`.
Students then use `ghcr.io/isk6708/lab-raj-fullstack:1.0.0` in `docker run` / compose.

---

## Troubleshooting

| Symptom | Cause / Fix |
|---|---|
| Workflow: `denied: installation not allowed to Create organization package` / `permission_denied: write_package` | `permissions: packages: write` missing, or repo **Settings → Actions → General → Workflow permissions** too strict. Also check the package's **Manage Actions access** includes this repo. |
| Workflow: `repository name must be lowercase` | `IMAGE_NAME` must be lowercase (`isk6708/lab-raj-fullstack`). |
| Workflow: `./mvnw: Permission denied` | Keep `chmod +x mvnw` in the Dockerfile. |
| Workflow: `COPY failed: .../dist/lab-raj-app/browser: not found` | Angular project name in `angular.json` differs → fix the path in the Dockerfile (Step 1.4). |
| Workflow: Angular build fails on **budgets** (`exceeded maximum budget`) | Increase `budgets` in `angular.json` or reduce CSS size (`anyComponentStyle` max is 8 kB). |
| Workflow: `npm ci` error about lock file | `package-lock.json` out of sync → run `npm install` locally, commit the lock file. |
| Page loads but every API call → `404` | `API_BASE_URL` still `http://localhost:8117/...` from an old build, or path typo → must be `/api/v1`. |
| Page loads, API calls → `403` before login | Expected for `/api/**`. After login still 403 → token not sent (interceptor) or `JWT_SECRET` changed (old token) → log out / log in. |
| Refresh on `/todos` → `403` | `anyRequest().permitAll()` not applied (Step 2.1). |
| Refresh on `/todos` → `404` / Whitelabel page | `SpaController` missing (Step 2.2). |
| Users disappear after restart | Container run **without** `-v lab-raj-data:/app/data`. |
| `SQLITE_CANTOPEN` / readonly DB | Volume folder not writable by `appuser` → use a **named** volume (as above), not a Windows bind mount, or remove `USER appuser` for the lab. |
| `docker pull` → `unauthorized` | Package still private → Step 6.4 or 7.3. |

---

## Files added / changed (summary)

| File | Step | Action |
|---|---|---|
| `lab-app-auth/src/app/apis/api.config.ts` | 1 | `API_BASE_URL = '/api/v1'` |
| `lab-app-auth/proxy.conf.json` | 1 | **New** – dev proxy `/api` → `localhost:8117` |
| `lab-app-auth/angular.json` | 1 | `serve.options.proxyConfig` |
| `lab-app-auth/src/app/header/header.html` | 1 | Update API badge text |
| `lab-raj/.../configs/SecurityConfig.java` | 2 | `/api/**` authenticated, `anyRequest().permitAll()` |
| `lab-raj/.../controllers/SpaController.java` | 2 | **New** – forward Angular routes to `index.html` |
| `Dockerfile` | 3 | **New** – 3-stage build (Node → JDK → JRE) |
| `.dockerignore` | 3 | **New** |
| `.github/workflows/lab-fullstack-docker.yml` | 5 | **New** – build & push to `ghcr.io/isk6708/lab-raj-fullstack` |
| `docker-compose.yml` | 7 | *Optional* – for students to run the image |
