# CampusFind

CampusFind is a campus lost-and-found web app for the fictional Westbridge University. Campus visitors can browse and search item reports, submit lost or found items, and find staffed handoff locations. The Student workspace adds a personal reports view and an alerts panel.

This is the **Second Deliverable — Initial Progress (50%)**. The Office and Student screens follow the [CampusFind Figma design](https://www.figma.com/design/YbUwzz01yEYvIQve4k4m9l/DSA?node-id=0-1).

## Team

| Name | Role |
| --- | --- |
| Lyle March Zambo | UI/UX design and documentation |
| Zen Afi Inarsao | UI/UX design and documentation |
| Heinz Colman | Coding |

## Run locally

1. Clone or download this repository and open a terminal in its root directory.
2. With Python 3 installed, start the local server.

   ```sh
   python3 -m http.server 8000 --bind 127.0.0.1
   ```

3. Open [http://127.0.0.1:8000](http://127.0.0.1:8000) in a modern browser.
4. Select **Student sign in**, then **Open Student demo** to enter the Student workspace.

The app uses HTML, CSS, and JavaScript. It requires no dependency installation or build step.

## Current functionality

- Office overview and a Student workspace with its own navigation.
- Campus board with search, status and category filters, and report details.
- Lost and found report forms with validation, a preview, and browser-local submission.
- My Reports, alerts, and safety pages with example handoff locations.

Student sign-in uses a demo profile. Reports persist in the current browser through local storage. Account authentication, shared report storage, live alerts, and claims processing are outside this milestone.

## Screenshots

The [screenshots folder](screenshots/) contains labeled desktop captures from the running application.

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
- `src/app.js` implements navigation, panels, and form interactions.
- `src/data.js` contains sample reports, filtering logic, and browser storage.
- `src/styles.css` defines the shared layout and responsive styling.
- `assets/` contains CampusFind design assets and the locally served Manrope font.
