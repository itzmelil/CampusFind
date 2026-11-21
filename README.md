# CampusFind

CampusFind is a campus lost-and-found web app for the **University of Mindanao, Matina campus, Davao City**. Campus visitors can browse and search item reports, submit lost or found items, and find staffed handoff locations. The Student workspace adds a personal reports view and an alerts panel.

This is the **Second Deliverable — Initial Progress (50%)**. The Office and Student screens follow the [CampusFind Figma design](https://www.figma.com/design/YbUwzz01yEYvIQve4k4m9l/DSA?node-id=0-1).

## Team

| Name | Role |
| --- | --- |
| Lyle March Zambo | UI/UX design and documentation |
| Zen Afi Inarsao | UI/UX design and documentation |
| Heinz Colman | Coding |

## Run the Java website

1. Clone or download and extract this repository. In VS Code, open the project folder and choose **Terminal → New Terminal**.
2. With Java 21 or later installed, run this command from the project root.

   ```powershell
   java src/main/java/campusfind/CampusFind.java
   ```

3. Leave the terminal open and visit [http://127.0.0.1:8000](http://127.0.0.1:8000).
4. To enter the Student workspace, select **Student sign in**, then **Open Student demo**.

Java 21 is the only runtime you need to install for CampusFind. It serves the site and handles validation, search, filtering, and report storage. The browser interface uses HTML, CSS, and JavaScript. To use a different port, pass it as the first argument, for example `java src/main/java/campusfind/CampusFind.java 8080`.

The optional Swing desktop version can still be launched on Windows with `run-desktop.bat`.

## Current functionality

- Office overview and a Student workspace with its own navigation.
- Campus board with search, status and category filters, and report details.
- Lost and found report forms with validation, a preview, and submission to the Java server.
- My Reports, alerts, and safety pages with example handoff locations.

Student sign-in uses a demo profile. Submitted reports persist in the local Java server’s `data/` directory and appear on its campus board. My Reports uses a browser cookie to identify the current demo profile. Handoff locations and campus listings are illustrative. Account authentication, a hosted database, live alerts, and claims processing are outside this milestone.

## Screenshots

The [screenshots folder](screenshots/) contains labeled browser screenshots from the running Java website.

| Panel | Office | Student |
| --- | --- | --- |
| Home | [Overview](screenshots/01-office-overview.png) | [Student Home](screenshots/07-student-home.png) |
| Item listings | [Browse Items](screenshots/02-office-browse-items.png) | [Campus Board](screenshots/08-student-campus-board.png) |
| Item details | [Office Item Details](screenshots/06-office-item-details.png) | [Student Item Details](screenshots/12-student-item-details.png) |
| Sign-in | [Student Sign-In](screenshots/03-student-sign-in.png) | — |
| Reports and claims | — | [My Reports](screenshots/09-student-my-reports.png) · [After Submission](screenshots/21-student-my-reports-after-submission.png) |
| Alerts | [Office Alerts](screenshots/04-office-alerts.png) | [Student Alerts](screenshots/10-student-alerts.png) |
| Safety | [Safety & Privacy](screenshots/05-office-safety-and-privacy.png) | [Student Safety](screenshots/11-student-safety.png) |
| Lost item form | [Office Form](screenshots/13-office-report-lost-item.png) | [Student Form](screenshots/17-student-report-lost-item.png) |
| Lost report preview | [Office Preview](screenshots/14-office-lost-report-preview.png) | [Student Preview](screenshots/18-student-lost-report-preview.png) |
| Found item form | [Office Form](screenshots/15-office-report-found-item.png) | [Student Form](screenshots/19-student-report-found-item.png) |
| Found report preview | [Office Preview](screenshots/16-office-found-report-preview.png) | [Student Preview](screenshots/20-student-found-report-preview.png) |

## UML diagram

[View the CampusFind UML use case diagram](uml/campusfind-use-case.png).

## Source layout

- `index.html` loads the application.
- `src/main/java/campusfind/CampusFindDesktop.java` implements the native Swing interface.
- `run-desktop.bat` compiles and launches the desktop version on Windows.
- `src/app.js` renders the browser panels and connects form interactions to the Java API.
- `src/main/java/campusfind/CampusFind.java` serves the app and implements report validation, filtering, sample data, and local file storage.
- `src/styles.css` defines the shared layout and responsive styling.
- `assets/` contains CampusFind design assets and the locally served Manrope font.
