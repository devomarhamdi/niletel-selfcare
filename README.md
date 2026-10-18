# NileTel Self-Care

NileTel is a mobile operator. Its old billing system, **bss-core**, keeps the money of every customer, and nobody may change the way other programs talk to it. This project has two parts:

1. **Build the old system exactly as its Interface Contract describes it** (`bss-core`, the frozen "legacy" side).
2. **Build a modern self-care service and web site** (`selfcare-api`, `selfcare-web`) that follow all of the legacy formats and rules, so that a customer can see the balance on their line and buy a bundle alone.

Final project, teams of 4, 3 days.

## Architecture

```
Customer's browser
      │ loads the pages            │ fetch() with a JWT
      ▼                            ▼
selfcare-web  (:5500)        selfcare-api  (:8081)
static HTML / CSS / JS       Spring Boot 3, JWT, own database
                                   │ server to server, inside Docker
                                   │ http://bss-core:8080/bss-core/api
                                   ▼
                             bss-core  (WildFly, Jakarta EE 10, no published port)
                                   │ JDBC
                                   ▼
        PostgreSQL ── database bss_core  (user bss_app)    ← bss-core only
                   └─ database selfcare  (user selfcare_app) ← selfcare-api only
```

- The browser talks **only** to `selfcare-api`. It never sees the legacy session (`JSESSIONID`).
- `selfcare-api` is the only client of `bss-core`.
- The two databases are separate: no table and no foreign key crosses them.

| Part | Technology | Address |
|---|---|---|
| `bss-core` | Jakarta EE 10 WAR on WildFly, plain JDBC (no Spring, no JPA) | `http://bss-core:8080/bss-core` (Docker network only) |
| `selfcare-api` | Spring Boot 3, Spring Security, Spring Data JPA, JJWT | `http://localhost:8081` |
| `selfcare-web` | Plain HTML, CSS, JavaScript (no framework, no theme) | `http://localhost:5500` |
| PostgreSQL 16 | databases `bss_core` and `selfcare` | internal to Docker (`5432`) |

## Repository layout

```
niletel-selfcare/
├── README.md
├── docs/            ER diagrams, the two notes, execution plans, the three project PDFs
├── db/
│   ├── 00-create-databases.sql     roles and databases (run once, as the admin)
│   ├── bss-core/    01-ddl.sql  02-seed.sql  reports.sql
│   └── selfcare/    01-ddl.sql  02-seed.sql  reports.sql
├── bss-core/        Maven WAR   (rest → service → dao;  filter, backoffice, messaging)
├── selfcare-api/    Spring Boot (controller → service → repository / legacy gateway)
├── selfcare-web/    static site (js/api is the only place that calls fetch)
├── api-tests/       bss-core.http, selfcare-api.http
└── deploy/          compose.yaml, .env.example, postgres/init.sh, k8s/ (bonus)
```

## The three documents

Read them before you write code. The Interface Contract is the source of truth for every exact format.

| Document | Use |
|---|---|
| Project Brief | What to build and in which order (sections 1 to 6 first, together). Every requirement has an id such as `L-05` (legacy), `N-04` (new API), `F-03` (frontend), `D-03` (database), `S-01` (design), `G-01` (deliverables), `T-01` (team). |
| Interface Contract | Requests, answers, error codes, test data and setup. Keep it open while you code. |
| Business Background | Optional, the longer story. Nothing in it is a requirement. |

Put a copy of the three PDFs in `docs/` so the whole team uses the same version.

## Getting started

### Option A: everything with Docker (recommended)

You need Docker with Compose. Nothing else (no Java, Maven or PostgreSQL) is required.

```bash
cp deploy/.env.example deploy/.env      # choose your own passwords; deploy/.env is never committed
docker compose -f deploy/compose.yaml up --build
```

Then open:

| What | URL |
|---|---|
| Web site | http://localhost:5500 |
| selfcare-api | http://localhost:8081 (answers `401` without a token; this is correct) |
| selfcare-api health | http://localhost:8081/actuator/health |

`bss-core` has no published port, as the brief requires. It is reachable from `selfcare-api` as `http://bss-core:8080/bss-core`. Its health address inside the network is `/bss-core/health`.

Useful commands:

```bash
docker compose -f deploy/compose.yaml ps                      # status and health of the four services
docker compose -f deploy/compose.yaml logs -f selfcare-api    # follow one service
docker compose -f deploy/compose.yaml up --build -d selfcare-api   # rebuild and restart one service
docker compose -f deploy/compose.yaml down -v                 # stop and delete the databases (start from empty)
```

The database scripts run **only on the first start of an empty volume**. After you change a DDL or seed script, run `down -v` and start again.

### Option B: run the parts yourself

Requirements: Java 17 or newer, Maven 3.9+, PostgreSQL 16 or 17, WildFly 27 or newer (Jakarta EE 10).

1. **Databases.** Follow section 5 of the Interface Contract:
   ```bash
   cd db
   psql -h localhost -U postgres -v bss_pw=<pw1> -v sc_pw=<pw2> -f 00-create-databases.sql
   psql -h localhost -U bss_app      -d bss_core -f bss-core/01-ddl.sql
   psql -h localhost -U bss_app      -d bss_core -f bss-core/02-seed.sql
   psql -h localhost -U selfcare_app -d selfcare -f selfcare/01-ddl.sql
   psql -h localhost -U selfcare_app -d selfcare -f selfcare/02-seed.sql
   ```
2. **bss-core.** Add the PostgreSQL driver, the data source `java:jboss/datasources/BssCoreDS` and the queue `java:/jms/queue/SmsQueue` to WildFly as in the contract (start WildFly with `-c standalone-full.xml`). Then:
   ```bash
   cd bss-core && mvn package
   cp target/bss-core.war $WILDFLY_HOME/standalone/deployments/
   # http://localhost:8080/bss-core/health  ->  UP
   ```
   `bss-core/docker/configure-wildfly.cli` is the same configuration as a script.
3. **selfcare-api.**
   ```bash
   cd selfcare-api
   SELFCARE_DB_PASSWORD=<pw2> mvn spring-boot:run      # http://localhost:8081
   ```
   Settings come from environment variables: `SELFCARE_DB_URL`, `SELFCARE_DB_USER`, `SELFCARE_DB_PASSWORD`.
4. **selfcare-web.**
   ```bash
   cd selfcare-web && python3 -m http.server 5500
   ```
   Open `http://localhost:5500`, **not** `http://127.0.0.1:5500`: the browser treats them as two different origins and only the first is allowed by CORS.

## How we work together

### Split the work by feature, not by application

Four owners, each with a feature that needs work in all three applications, so nobody waits for another person. The suggested split is in section 18 of the Project Brief:

| Owner | Feature | Main requirement ids |
|---|---|---|
| 1 | Identity: sessions and tokens | L-02, N-01, N-03, N-04, N-06, N-14, F-02 |
| 2 | Subscriber: login, profile and money in bss-core | L-03, L-04, L-05, L-07, L-09, N-05, N-07, N-10, F-03 |
| 3 | Bundles: catalogue, purchase, errors | L-06, N-02, N-11, N-13, F-04, F-06, D-03 and D-04 for `selfcare` |
| 4 | Platform: shared base | L-01, L-10, N-15, F-01, F-05, D-03 and D-04 for `bss_core`, G-01, G-02 |

Every member writes SQL and code in at least two of the three applications. The final owner table goes in the section "Team" below.

### Rules for the repository

- **Contract first.** Before you code a requirement, read its part of the Interface Contract and copy its examples into the `.http` files in `api-tests/`. Legacy endpoints first return the correct envelope with fixed data, so nobody is blocked.
- **`main` always builds and starts.** Never push a broken `main`.
- **One short branch per requirement**, named with its id: `feature/L-05-debit`, `feature/N-04-session-store`.
- **Commit messages contain the requirement id**, for example `L-05: debit endpoint with BAL002`.
- **Open a pull request**, have another member review it, merge at least twice a day.
- **Tell the team before you change a shared file**: any `pom.xml`, a DDL script, `deploy/compose.yaml`, the API module (`selfcare-web/js/api`).
- **Never commit a password or a secret.** `deploy/.env` is ignored by Git; only `deploy/.env.example` is committed.

### Milestones

1. Agree: documents read, owners chosen, everybody can run the stack.
2. Two small paths: legacy login and `GET /subscriber/me` answer with the envelope; `POST /api/auth/login` returns a JWT; the shop shows bundles from the `selfcare` database.
3. **Walking skeleton** (tag it `skeleton`): login page → `POST /api/auth/login` → legacy login → JWT → dashboard → `GET /api/me` → database.
4. The CORE: buying a bundle from the screen to the ledger.
5. Every Must row with its error cases.
6. Should rows: finish one whole feature before starting the next.
7. Freeze (last two hours): no new features, test from empty databases, run the demo checklist, fill in the status table.

## Design rules

These come from the Project Brief (sections 7 and 14) and are checked at the end.

| Application | Layers, from outside to inside | Not allowed |
|---|---|---|
| bss-core | REST resource or Servlet → session bean (rules, transaction) → DAO (SQL) | SQL or business rules in a resource, Servlet or JSP |
| selfcare-api | controller → service → repository (JPA) and legacy gateway | Business rules in a controller; JPA entities as JSON; legacy formats outside the `legacy` package |
| selfcare-web | page scripts → API module | `fetch` outside the API module |

- **bss-core is frozen.** Every format, path and error code is exactly as in the Interface Contract. Do not "improve" it.
- **Only the `legacy` package of selfcare-api knows bss-core.** Search the project for `PIASTERS`, `JSESSIONID` and `dd/MM/yyyy`: they may appear only there.
- No SQL built by joining strings with user input (use `PreparedStatement` or JPA parameters).
- No UI frameworks or themes (no React, Vue, Angular, jQuery, Bootstrap, Tailwind). Own HTML, CSS and JavaScript.
- Allowed libraries: `bss-core` uses only what WildFly provides plus the PostgreSQL driver. `selfcare-api` uses Spring Boot starters, a JWT library, the PostgreSQL driver and Lombok. Ask before adding anything else.
- JPA never creates the schema (`ddl-auto=validate`); the SQL scripts do.

## Test data

The fixed test accounts are in section 4 of the Interface Contract. The main demo account is **Mona Adel**, `01012345678`, PIN `1234`, balance `67.50 EGP`. The seed scripts must contain exactly that data.

## Team

> Fill this in. Required by G-01.

| Member | Owns |
|---|---|
| | |
| | |
| | |
| | |

## Status of the requirements

> Fill this in as you build. Required by G-01. A row that is not in the table counts as not done, and the table must be true.

| Id | Requirement | Status | Notes |
|---|---|---|---|
| L-06 | bss-core `GET /health` | done | `UP` / `DOWN` from `SELECT 1` |
| | | | |

Done so far: the project skeleton, Docker setup, the health endpoints and the folder structure. No business feature is built yet.

## Troubleshooting

| Problem | Cause and fix |
|---|---|
| `docker compose` says "copy deploy/.env.example to deploy/.env" | The password file is missing. Copy the example and choose passwords. |
| A role or database does not exist after changing the SQL | The scripts run only on a new volume. Run `docker compose -f deploy/compose.yaml down -v`, then start again. |
| `selfcare-api` stays unhealthy | Read its log. Usually the database is missing or a JPA entity does not match the DDL (`ddl-auto=validate`). |
| The browser asks for a username and password | You are seeing an old build of `selfcare-api`. Rebuild with `up --build -d selfcare-api`. |
| CORS error in the browser | Open the site as `http://localhost:5500`, not `127.0.0.1`. |
| Port 8080, 8081, 5500 or 5432 already in use | Stop the other program using the port. |
| Text with wrong line endings on Windows | `.gitattributes` forces LF for shell scripts, SQL and Dockerfiles; do not override it. |
