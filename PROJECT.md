# Contact Management System

Each account has a private address book. A user registers with an email address or phone number, saves contacts with labeled details, and manages their password from My profile.

## Run on Windows

Install a JDK 17 or 21, Maven 3.9+, Node.js 22.12+ with npm, and SQL Server Developer or Express. Open the repository folder in VS Code. If using a local SQL Server instance, enable TCP/IP and use its actual listening port. The examples use port 1433.

1. In SQL Server Management Studio, connect to your instance and run `CREATE DATABASE contact_book;`. Create a database user for your SQL login, grant it permission to create the project tables and read/write their rows. The application initializes the schema on first start. For a dedicated local development database, granting this user `db_owner` on `contact_book` is sufficient.
2. In the VS Code PowerShell terminal at the repository root, run the commands below. Enter the database login when prompted. The local example trusts the development server certificate. Use a trusted certificate and `trustServerCertificate=false` on a deployed server.

```powershell
$env:DB_URL='jdbc:sqlserver://localhost:1433;databaseName=contact_book;encrypt=true;trustServerCertificate=true'
$env:DB_USER=Read-Host 'SQL Server login'
$env:DB_PASSWORD=[System.Net.NetworkCredential]::new('',(Read-Host 'SQL Server password' -AsSecureString)).Password
mvn spring-boot:run
```

3. Open a second terminal in the repository root and start the React development server.

```powershell
cd frontend
npm ci
npm run dev
```

4. Open **http://127.0.0.1:5173** and select **Create an account**. Registration opens the contacts dashboard. The React development server forwards API calls to Spring Boot on port 8080.

## Try the assignment

1. Create a contact with a name, title, work email, personal email and phone number. Select the name to open its detailed profile.
2. Search for part of either name. Page controls appear after ten contacts. Try canceling an edit or deletion before confirming one.
3. Open **My profile**, change your password, log out and sign in again. Register a second account to check that the two address books are separate.

## Architecture

React components in `frontend/src` call the Spring MVC controllers under `/api`. The service layer applies account ownership and password rules. Spring Data repositories persist accounts, contacts and ordered lists of email/phone entries. The SQL Server schema is in `src/main/resources/schema-sqlserver.sql`.

Sign-in uses a server-side session cookie. Passwords use individually salted PBKDF2-HMAC-SHA256. Changing a password rotates the active session and invalidates older sessions on their next request. All writes require the custom same-origin request header, and cross-origin access is disabled. Local servers bind to loopback by default. For HTTPS deployment, serve the UI and API on one origin and set `COOKIE_SECURE=true`.

SLF4J with Spring Boot's Logback provider records account and contact activity using numeric IDs. Validation failures return HTTP status codes and readable messages through global exception handling.

## Tests and SonarQube

Run `mvn clean verify` at the repository root for JUnit, Mockito, MVC and database tests. Automated local tests use an isolated H2 database. The CI workflow also runs the integration tests against a disposable SQL Server. Run `npm ci` and `npm run build` in `frontend` for the production bundle.

To run code analysis, start your SonarQube Server, create a project with key `cohort-10-java-19696-ali`, and select the **Sonar way** Java and JavaScript quality profiles under **Project Settings → Quality Profiles**. With SonarScanner CLI on PATH, run the following after Maven verification from the repository root. The scanner reads the source roots and JaCoCo coverage path from `sonar-project.properties`.

```powershell
$env:SONAR_HOST_URL='http://localhost:9000'
$env:SONAR_TOKEN=[System.Net.NetworkCredential]::new('',(Read-Host 'SonarQube project token' -AsSecureString)).Password
sonar-scanner
```

Branch builds start a temporary SonarQube Community server and publish the completed analysis metrics in the GitHub Actions job summary. To retain analysis history, the scanner can use an external server when the repository variable `SONAR_HOST_URL` and secret `SONAR_TOKEN` are configured for a reachable server. The app requires no SonarQube credentials to run.
