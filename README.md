# Country Explorer

Country Explorer is a web application for browsing information about the countries of the
world. It uses data from the [REST Countries](https://restcountries.com) API.

## Core functionality

**Countries list** (`/`)
- Paged table of all countries, 10 per page, with Previous/Next navigation
- Search by name (matches both the common and the official name)
- Filter by region (Africa, Americas, Asia, Europe, Oceania)
- Sort by population, ascending or descending

**Country detail page** (`/countries/{code}`)
- Flag, capital, population, area, region, languages and currencies
- Facts card: calling codes, top-level domains, driving side, UN/EU membership, landlocked,
  form of government, demonym, first day of the week, short description, Wikipedia link
- Bordering countries, each linking to its own detail page

**Flag quiz** (`/game`)
- 10 rounds of "which country does this flag belong to?"
- Country-name autocomplete while typing, and the option to skip a round
- Answers are checked by the backend nd accepts either the common or the official name. The backend only reveals
  the correct name once you've answered, so it can't be read from the network traffic.
- Final score and a Play again button

## Also included in the project

- **Docker support:** a Dockerfile for the backend and one for the frontend, plus a
  `docker-compose.yml` that starts the whole application with one command. In the
  container, nginx serves the Angular app and forwards `/api` requests to the backend.
- **Unit tests for both backend services:**
  - `CountryServiceTest`
  - `GameServiceTest`

## Tools and technologies

| Area | Technology |
|---|---|
| Backend language | Java 21 |
| Backend framework | Spring Boot 4.1 (Spring Web MVC, Spring Cache) |
| Backend build tool | Gradle 9.7 (via the included Gradle wrapper) |
| JSON | Jackson |
| API documentation | springdoc-openapi 2.8 (OpenAPI 3 + Swagger UI) |
| Backend testing | JUnit 5, Mockito, Spring MockMvc |
| Frontend framework | Angular 21 (standalone components, signals, lazy-loaded routes) |
| Frontend language | TypeScript 5.9 |
| Reactive programming | RxJS 7.8 |
| UI components | ZardUI (shadcn-style components for Angular) |
| Styling | Tailwind CSS 4 (via PostCSS) |
| Icons | ng-icons with the Lucide icon set |
| Frontend testing | Vitest 4 with jsdom (via `ng test`) |
| Package manager | npm |
| Containers | Docker, Docker Compose |
| Web server in container | nginx 1.29 |
| External data source | REST Countries API v5 |

## Building and running the application

The backend needs a REST Countries API key. Without one it falls back to the built-in
`rc_live_demo` key, which returns only a single sample country.

### With Docker

Requirements: Docker with the Compose plugin. Java and Node.js do not need to be installed.

1. Open a terminal in the project root (the folder that contains `docker-compose.yml`).
2. Create the environment file and enter your API key in it:

       cp .env.example .env

   ```
   REST_COUNTRIES_API_KEY=your_key
   ```

3. Build the images and start the containers:

       docker compose up --build

   The first build takes a few minutes because it downloads Gradle and all dependencies.
   Later builds are much faster.

4. Open the application:
   - App: http://localhost:4200
   - Swagger UI: http://localhost:8080/swagger

5. To stop the application, press `Ctrl+C`, then run:

       docker compose down


### Without Docker

Requirements: JDK 21 and Node.js 20.19+, 22.12+ or 24 (with npm).

**Backend** (port 8080). In a terminal:

    cd Backend
    REST_COUNTRIES_API_KEY=your_key sh ./gradlew bootRun

On Windows (PowerShell):

    cd Backend
    $env:REST_COUNTRIES_API_KEY="your_key"
    .\gradlew.bat bootRun


**Frontend** (port 4200). In a second terminal:

    cd Frontend
    npm install
    npm start

The development server forwards `/api` to `http://localhost:8080` (see
`proxy.conf.json`), so the backend must be running.

- App: http://localhost:4200
- Swagger UI: http://localhost:8080/swagger


## Running the unit tests

The tests run without Docker and do not need an API key or an internet connection.
Nothing in them calls the real REST Countries API.

### Backend tests

Requirement: JDK 21.

    cd Backend
    sh ./gradlew test

On Windows: `.\gradlew.bat test`. If `./gradlew` is executable (`chmod +x gradlew`),
`./gradlew test` also works.

Gradle only prints output when a test fails. `BUILD SUCCESSFUL` means every test passed.

Useful variants:

    sh ./gradlew test --rerun                                    # run again even if nothing changed
    sh ./gradlew test --tests '*CountryServiceTest'              # one test class
    sh ./gradlew test --tests '*GameServiceTest.acceptsOfficialName'   # one test method