# Attendance App

An offline-first Android attendance management application that uses on-device facial recognition and geofencing to securely verify staff attendance.

The goal of this project was to build a secure and simple attendance system without relying on a backend server or cloud-based facial recognition service. All application data, including biometric information, is stored and processed locally on the device.

The application supports two roles: **Admin** and **Staff**, with separate functionality and permissions for each.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** Multi-Module Clean Architecture
- **Dependency Injection:** Dagger Hilt
- **Local Database:** Room
- **Facial Recognition:** OpenCV + SFace
- **Location Verification:** Android Location Services + Geofencing
- **Data Processing:** Fully on-device

## Architecture

The application follows a **Multi-Module Clean Architecture** approach to keep the codebase modular, scalable, and easier to maintain.

The project is divided into shared core modules and feature-specific modules such as:

- Authentication
- Admin
- Attendance
- Staff
- Shared/Core utilities

This separation keeps individual features independent and reduces unnecessary dependencies between different parts of the application.

The application is written entirely in **Kotlin**, with the UI built using **Jetpack Compose**.

**Dagger Hilt** is used for dependency injection, while **Room Database** acts as the primary source of truth for application data.

Since the application is designed to work completely offline, the local database contains the complete application state. The database currently consists of **11 relational tables** covering data such as users, staff profiles, attendance records, holidays, notices, review requests, and biometric metadata.

## Facial Recognition

Facial recognition is implemented using **OpenCV with SFace**.

Instead of uploading staff photos or biometric information to an external service, face processing and matching happen directly on the device.

This approach provides two important benefits:

1. Attendance verification does not depend on internet connectivity.
2. Biometric information stays on the device and is not sent to third-party facial recognition services.

When an admin creates a staff profile, the staff member's reference image is registered for facial matching. During attendance, the captured face is compared against the stored reference data.

## Location Verification & Geofencing

In addition to facial recognition, the application uses **geofencing** to make sure staff members can only mark attendance when they are physically close to the configured workplace location.

The application uses the admin-configured latitude and longitude as the center of the allowed attendance area.

A staff member must be within a **100-meter radius** of this location before they can proceed with check-in.

The attendance flow therefore performs two levels of verification:

`Location Verification → Facial Recognition → Attendance`

If the staff member is within the allowed 100-meter radius, the application allows them to continue with facial verification.

If they are outside the allowed radius, the check-in action is blocked and an alert is displayed asking them to move closer to the configured attendance location.

For example:

> **Outside Attendance Area**
> You are currently outside the allowed check-in area. Please move within 100 meters of the workplace location and try again.

The admin can also **update the attendance location dynamically**. This allows the authorized check-in location to be changed whenever required without modifying the application.

The updated coordinates are stored locally and used as the new center point for subsequent attendance verification.

This adds an additional security layer to attendance verification:

`Authorized Location + Verified Face = Valid Attendance`

## Getting Started

There are no hardcoded admin or staff credentials in the application.

When the application is launched for the first time, it detects that no admin account exists and displays the **Admin Setup** screen.

The initial flow is:

`First Launch → Create Admin Account → Admin Login → Configure Attendance Location → Create Staff → Register Staff Face → Staff Login`

After creating the admin account, the admin can log in and start setting up staff accounts.

For every staff member, the admin can:

- Create login credentials
- Configure the staff profile
- Upload/register a reference photo for facial recognition
- Manage account access

The admin can also configure the latitude and longitude used by the geofencing system for attendance verification.

Since everything is stored locally, the complete application can be configured directly from the device without requiring any server-side setup.

## Admin Features

The Admin section acts as the main management interface for the application.

Admins can create and manage staff accounts, including updating credentials and blocking or disabling accounts when required.

The employee directory allows admins to view individual staff profiles along with their attendance history and attendance percentage.

Admins can also review attendance attempts that could not be confidently verified by facial recognition.

When a face match falls within the manual-review range, the attendance attempt can be placed in the review queue. The admin can inspect the request and either **approve** or **reject** it.

Admins can also configure and update the **authorized attendance location**. The selected coordinates become the center point of the 100-meter geofence used during staff check-in.

The Admin section also includes a notice system. Notices can be sent to:

- All staff
- Selected groups
- Individual staff members

## Staff Features

The Staff experience is designed around a simple but secure daily attendance flow.

Staff members log in using the credentials created by the admin and can mark their attendance using the device camera.

Before opening the facial verification flow, the application first checks the staff member's current location.

The check-in flow is:

`Check In → Verify Location → Verify Face → Record Attendance`

If the staff member is **within 100 meters** of the location configured by the admin, they can proceed with facial verification.

If the staff member is **outside the 100-meter geofence**, check-in is blocked and an alert asks them to move closer to the authorized location.

Once the location check succeeds, the captured face is processed locally and compared with the registered reference face.

The application currently uses the following matching logic:

| Match Score | Action |
|---|---|
| **Above 80%** | Attendance is verified and recorded automatically |
| **20% – 80%** | User is asked to retry or the attempt is sent for admin review |
| **Below 20%** | Verification fails and the user must retry |

This allows high-confidence matches to be processed immediately while uncertain results can be reviewed instead of being automatically accepted.

Staff members can also access their dashboard to view:

- Attendance history
- Attendance status
- Personal attendance records
- Upcoming company holidays
- Notices sent by the admin

## Attendance Verification Flow

To reduce the possibility of unauthorized attendance, a successful check-in requires both **location verification and facial verification**.

```text
Staff taps Check In
        ↓
Get Current Location
        ↓
Is Staff Within 100m?
     ↙        ↘
   No          Yes
    ↓            ↓
Block       Open Camera
Check-In         ↓
    ↓       Capture Face
Show Alert       ↓
            Face Matching
                 ↓
        ┌────────┼────────┐
      >80%     20-80%    <20%
        ↓         ↓         ↓
     Success    Review     Retry
        ↓
Record Attendance
```

This prevents a valid staff account and registered face from being used to mark attendance from an unauthorized location.

## Offline-First Approach

One of the main engineering decisions behind this project was to make the application **fully offline-first**.

Room acts as the local source of truth, while facial recognition is performed directly on the device using OpenCV and SFace.

Location verification is also performed on-device by comparing the staff member's current location against the attendance coordinates configured by the admin.

As a result, the core attendance flow does not require communication with a backend server.

This provides several advantages:

- Fast attendance verification
- Location-based attendance restriction
- No cloud facial-recognition API dependency
- Reduced external infrastructure requirements
- Biometric processing remains on-device
- Admin-configurable attendance location

## Known Limitations

The main limitation of the current implementation is that the application is **device-bound**.

Because there is currently no backend or cloud synchronization layer, all application data exists only inside the local database on the device.

This includes:

- Admin credentials
- Staff accounts
- Staff profiles
- Registered biometric data
- Attendance history
- Attendance location coordinates
- Holidays
- Notices
- Review records

If the application is uninstalled or its application data is cleared from Android settings, the locally stored information will be permanently deleted.

Location verification also depends on the accuracy and availability of the device's location services. GPS accuracy can vary depending on the device, surrounding buildings, and environmental conditions.

For a production deployment, a secure backup or synchronization mechanism could be introduced while still keeping facial recognition and sensitive biometric processing on-device.

## Summary

This project demonstrates an offline-first approach to biometric and location-based attendance management using modern Android development practices.

The application combines **Kotlin, Jetpack Compose, Clean Architecture, Room, Hilt, OpenCV, SFace, and Geofencing** to provide two layers of attendance verification:

**Location Verification + Facial Recognition**

Staff members must be within the admin-configured **100-meter attendance radius** and successfully pass facial verification before their attendance can be recorded.

The architecture also leaves room for future improvements such as encrypted backups, multi-device synchronization, backend integration, audit logging, anti-location-spoofing checks, and stronger biometric security.