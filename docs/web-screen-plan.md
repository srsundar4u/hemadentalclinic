# Web application screen plan

Status: updated with the user's dashboard and patient navigation requirements. Role visibility remains a proposed assumption.

## Users and deployment

- Dentist: Dr Hemshree.
- Receptionist: separate staff account.
- Initial deployment: Spring Boot and PostgreSQL on the laptop.
- Web interface first; staff-only Android app later.
- Synthetic patient data for development and demonstrations.

## First-version screens

| Screen | Receptionist | Dentist | Main actions |
| --- | --- | --- | --- |
| Login | Yes | Yes | Sign in; show login errors; logout available after sign-in |
| Today / dashboard | Yes | Yes | Show only today's appointment count, arrival count and amount collected; click a total to open its details |
| Patient list | Yes | Yes | Search by name, phone or patient ID; use Create new to register; click a patient in results to open their profile |
| Patient form (from Patient list) | Yes | Yes | Create new opens registration; edit opens from profile; name, phone, optional date of birth/address and generated patient ID |
| Patient profile (from search results) | Basic details, appointments, payments | All sections | Click a patient to see details, appointments, visit history and balances; clinical notes visible only to dentist under proposed role policy |
| Appointment list | Yes | Yes | View appointments for a selected day; filter by status; open appointment details |
| Appointment form | Yes | Yes | Select existing patient, date, start time, duration and reason; book or reschedule; prevent overlapping appointments for one dentist |
| Appointment details | Yes | Yes | View booking; receptionist marks arrived, cancels or marks no-show; dentist starts and completes the visit |
| Visit / consultation | No | Yes | Record complaint, relevant history/allergies, examination notes, treatment performed and follow-up recommendation; save draft or complete visit |
| Billing and payments | Yes | Yes | Record treatment charge descriptions and amounts, payment date/method/amount; show total charges, total paid and outstanding balance; record later payments |
| Daily summary | Yes, operational and payment information | Yes | Review selected day's bookings, completed visits, payments received that day and outstanding balances; no clinical-note access for receptionist |

These are logical screens, not necessarily separate pages for every operation. Create/edit forms share a template. Patient profile sections and appointment details may use tabs or panels.

## Dashboard and patient navigation

Dashboard has three summary cards and no appointment or payment detail tables:

- Appointments: count of today's bookings; opens Appointment list filtered to today.
- Arrivals: count of today's appointments that have been checked in, including those subsequently in consultation or completed; opens Appointment list filtered to today's checked-in visits.
- Collections: total money received today, including payments for older visits; opens Billing and payments filtered to today's payment transactions. This is an amount, not a payment count.

Appointment totals include cancelled and no-show bookings; their status is visible after opening the details. These counting definitions are proposed implementation defaults.

Patient list is the entry point for registration and profiles. Create new -> registration form -> save -> patient profile. Search -> click patient -> profile -> edit details, book appointment, or open visit/payment history. Registration and profile have no separate top-level navigation entries. Return to Patient list retains the search where practical.

## Workflow

Existing patient: login -> today or patient search -> patient profile -> book appointment -> mark arrived -> dentist opens consultation -> save treatment and complete visit -> record charges/payment -> book follow-up.

New patient: register patient before booking. Walk-in: register/find patient and create a same-day appointment, subject to availability.

Appointment statuses: Scheduled -> Arrived -> In consultation -> Completed. Scheduled appointments can also be Cancelled or No-show. Appointment status and payment status remain separate: a completed visit can still have an outstanding balance.

Payment methods for initial version: Cash, UPI, Card. Record payments only; no payment gateway integration. The daily payment total uses the payment date, including payments toward older visits.

## Shared behavior

- Navigation: Today, Patients, Appointments, Daily summary, Logout.
- Responsive layouts for laptop and phone browsers.
- Field validation, clear success/error messages, empty states, and friendly not-found pages.
- Role checks on the server as well as appropriate visible actions in the UI.
- No permanent patient deletion in the first version; correct basic details and retain visit/payment history.
- No self-service account registration. Provision the initial staff accounts during setup.
- Laptop-only access until authentication and permissions are complete.

## Deferred features

- Excel import/export, automated reminders, prescriptions and printable invoices.
- Dental tooth chart, X-rays/uploads, inventory, insurance and payment gateways.
- Multiple dentists/branches, patient accounts, staff administration UI and analytics.
- Android client, cloud deployment and offline mobile sync.

Backup and restore verification are prerequisites for real clinic use, even though a backup management screen is deferred. Use synthetic data while building.

## Suggested implementation order

1. Patient list, patient form and basic patient profile.
2. Appointment list, form and details, plus numeric dashboard cards with links to details.
3. Dentist consultation and clinical history in the profile.
4. Billing, payments and daily summary.
5. Complete role permissions and full workflow verification before shared clinic use.

User-selected order: implement login first, then develop the other screens one at a time. Login now has separate dentist/receptionist accounts; the current protected landing page is a welcome page. The numeric dashboard is a subsequent screen and does not yet show placeholder totals.

## Assumptions to confirm

- One dentist and one receptionist initially.
- Initial mobile experience is an online browser; installed Android app follows later.
- Charges and payments are simple visit records; formal invoice/tax requirements are not yet defined.
- The receptionist cannot read clinical notes. Dentist may manage reception actions when needed.
