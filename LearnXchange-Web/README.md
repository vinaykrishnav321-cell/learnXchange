# LearnXchange (web version)

Learning through exchange: trade the skills you have for the skills you want. No money involved.

Spring Boot 3 (Java 17) REST API + a single-page front end (HTML, CSS, vanilla JS) served by the same app.
MySQL 8 for data, pooled with HikariCP. One deployable unit: `Dockerfile`.

## Run locally
1. Create an empty MySQL database: `CREATE DATABASE learnxchange;`
2. Set environment variables, then start the app (tables and starter skills are created automatically on first start):

   macOS/Linux:
   ```bash
   export DB_URL="jdbc:mysql://localhost:3306/learnxchange?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   export DB_USER=root
   export DB_PASSWORD=your_password
   export ADMIN_EMAIL=you@example.com
   export ADMIN_PASSWORD='Choose-a-strong-pass-1'
   mvn spring-boot:run
   ```
   Windows (PowerShell): use `$env:DB_URL="..."` etc. before `mvn spring-boot:run`.
3. Open http://localhost:8080

## Environment variables
| Name | Required | Meaning |
|---|---|---|
| `DB_URL` | yes | JDBC URL, e.g. `jdbc:mysql://HOST:PORT/DBNAME?sslMode=REQUIRED` for hosted MySQL |
| `DB_USER`, `DB_PASSWORD` | yes | database login |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | recommended | creates the first admin if none exists (password: 8+ chars, letter + digit). Without them no admin is created. |
| `ADMIN_NAME` | no | display name of the admin |
| `COOKIE_SECURE` | production | set `true` when served over https |
| `DB_POOL_SIZE` | no | default 5 |
| `PORT` | no | default 8080 (hosts like Render set it for you) |

## Deploy (example: Render + hosted MySQL)
Vercel cannot run this (it needs a Java server), so use a host that runs Docker/Java.
1. Create a MySQL database on a provider of your choice (Aiven, Railway, TiDB Cloud, etc.; check current free tiers) and note host, port, database, user, password.
2. Push this folder to a GitHub repo.
3. On render.com: New > Web Service > connect the repo. Render detects the `Dockerfile`.
4. Add the environment variables above (`COOKIE_SECURE=true`).
   Hosted MySQL usually needs TLS: `DB_URL=jdbc:mysql://HOST:PORT/DBNAME?sslMode=REQUIRED`
5. Deploy. Render gives you a public https URL. Free instances sleep when idle, so the first load can be slow.

## Project layout
- `model`, `dao`, `service`, `util` - same layered core as the desktop version (matching algorithm in `MatchingService`)
- `web` - REST controllers (`*Api`), DTOs, session guard, error handler, security headers, startup bootstrap
- `src/main/resources/static` - `index.html`, `styles.css`, `app.js`
- `src/main/resources/schema.sql` - tables + starter skills (run automatically)

## API summary
`/api/auth/{register,login,logout,me}`, `/api/profile`, `/api/profile/skills`, `/api/skills`, `/api/matches`,
`/api/users/{id}`, `/api/requests...`, `/api/sessions...`, `/api/reports`, `/api/admin/...` (admin only).

## Security notes
Passwords are PBKDF2-hashed; sessions use an HttpOnly cookie and rotate on login; sign-in is throttled after 5 failures per email;
all SQL uses prepared statements; the UI escapes all user text; a Content-Security-Policy blocks inline scripts.
Not included: email verification, password reset, CSRF tokens (mitigated by SameSite=Lax and JSON-only POSTs), and in-app chat.
