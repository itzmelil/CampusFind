# CampusFind

CampusFind is a campus lost-and-found Java Swing desktop app for the **University of Mindanao, Matina campus, Davao City**. Campus visitors can browse and search item reports, submit lost or found items, and find staffed handoff locations. The Student workspace adds a personal reports view and an alerts panel.

This is the **Second Deliverable — Initial Progress (50%)**. The Office and Student screens follow the [CampusFind Figma design](https://www.figma.com/design/YbUwzz01yEYvIQve4k4m9l/DSA?node-id=0-1).

## Team

| Name | Role |
| --- | --- |
| Lyle March Zambo | UI/UX design and documentation |
| Zen Afi Inarsao | UI/UX design and documentation |
| Heinz Colman | Coding |

## Run in VS Code

1. Install the **Java 21 JDK** or newer, then clone or download and extract this repository.
2. Open the project folder in VS Code and choose **Terminal → New Terminal**. On Windows, run:

   ```powershell
   .\run-desktop.bat
   ```

3. CampusFind opens in its own window. To enter the Student workspace, select **Student sign in**, then **Open Student demo**.

The Windows launcher compiles and starts CampusFind using the JDK. You can also compile and start it directly from the project folder on Windows, macOS, or Linux:

```sh
javac -encoding UTF-8 -d build/desktop src/main/java/campusfind/CampusFind.java src/main/java/campusfind/CampusFindDesktop.java
java -cp build/desktop campusfind.CampusFindDesktop
```

## Current functionality

- Office overview and a Student workspace with its own navigation.
- Campus board with search, status and category filters, and report details.
- Lost and found report forms with validation, a preview, and local submission.
- My Reports, alerts, and safety pages with example handoff locations.

Student sign-in uses a local demo profile. Submitted reports persist in the project’s `data/` directory and appear on the campus board and in My Reports. Handoff locations and campus listings are illustrative. Account authentication, live alerts, and claims processing are outside this milestone.

## Screenshots

The [screenshots folder](screenshots/) contains labeled screenshots of the Java Swing application.

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

- `src/main/java/campusfind/CampusFindDesktop.java` implements the Swing interface and starts the app.
- `src/main/java/campusfind/CampusFind.java` implements validation, filtering, sample data, and local report storage.
- `run-desktop.bat` compiles and launches the app on Windows.
- `assets/` contains the bundled item photos and icons.

The bundled logo at `assets/campusfind-logo.png` appears in the sidebar and as the window icon.
