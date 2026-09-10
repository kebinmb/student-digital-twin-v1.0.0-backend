# Frontend RBAC Verification & Implementation Roadmap

**Document Version:** 1.0.0  
**Target Platform:** Student Digital Twin (`SDT-v1.0.0`)  
**Frontend Framework:** Angular 19+ (Standalone Components, Signals) / PrimeNG  
**Reference Document:** [`00 Role Base Access.md`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/student-digital-twin/06%20Summary/00%20Role%20Base%20Access.md)  
**Primary Configuration References:**
- [`app.routes.ts`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/app.routes.ts)
- [`role.guard.ts`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/core/guards/authorization/role.guard.ts)
- [`auth-service.ts`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/core/service/authentication/auth-service.ts)
- [`dashboard-layout.component.ts`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/features/dashboard/dashboard-layout/dashboard-layout.component.ts)

---

## 1. Executive Audit Summary

A comprehensive client-side security audit of `student-digital-twin-v1.0.0-frontend` was conducted against the backend REST API contract documented in `00 Role Base Access.md`.

### 1.1 Overall Implementation Status: **PARTIALLY IMPLEMENTED (Significant Architectural Gaps)**
While basic authentication (`authGuard`), JWT refresh cycling, and coarse role checks exist, the frontend exhibits significant privilege coordination flaws:
1. **Route Hierarchy Blocking:** Over-restrictive parent routes in [`app.routes.ts`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/app.routes.ts) block authorized institutional roles (e.g., `FACULTY` blocked from `/curriculum` and `/scheduling`; `CHAIRPERSON` blocked from `/institution`).
2. **Static Navigation Exposure:** The navigation sidebar ([`dashboard-layout.component.ts`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/features/dashboard/dashboard-layout/dashboard-layout.component.ts)) statically displays administrative routes to all roles (including `STUDENT`), relying solely on guard bounce-backs.
3. **Missing Client User Identity (`sub`/`id`):** [`AuthService.currentUser`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/core/service/authentication/auth-service.ts) neglects to extract `payload.sub` (the numeric User ID). Consequently, instructor ownership checks in the gradebook (`userId === roster.primaryInstructorId`) evaluate to `undefined === id` (`false`), locking legitimate instructors out of grade submissions.
4. **Unprotected In-Page Mutation Triggers:** Critical administrative action buttons (e.g., "Admit Student", "Credit Transferee", "Add Schedule", "Update Status") lack template-level `@if` or structural directive guards, allowing unauthorized roles to open submission modals and trigger `403 Forbidden` API calls.
5. **Student IDOR & Privilege Traps:** The enrollment store (`EnrollmentStore.loadInitialData()`) unconditionally invokes `GET /api/v1/students/search` upon loading, which is forbidden to students by backend `@PreAuthorize`, causing immediate `403` toasts on student login.

### 1.2 Security Exposure Level: **HIGH (Client-Side Privilege Confusion & Operational Failure)**
- **Privilege Leakage:** Unauthorized users see controls for actions they cannot perform, degrading UX and signaling internal attack vectors.
- **Denial of Access:** Valid instructors and department chairs are locked out of their legitimate operational workflows due to mismatched parent route guards and missing user IDs.

---

## 2. RBAC Gap Analysis Table

The table below cross-references each frontend module, route, and UI element against the backend RBAC contract established in `00 Role Base Access.md`:

| Feature / Route / Module | Required Backend Role(s) | Route Guard Status | Sidebar / Nav Visibility Status | Action / Button Protection Status | Risk / Gap Notes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Authentication & Reset**<br>`/login`<br>`/forgot-password`<br>`/reset-password` | Public / `permitAll()` | **Aligned** (`guestGuard` prevents authenticated re-entry) | N/A (Outside dashboard shell) | **Aligned** (Form validation active) | **Low Risk.** Public access matches backend security configuration. |
| **Dashboard Shell**<br>`/dashboard` | Authenticated (All Roles) | **Aligned** (`authGuard` checks token & refresh) | **Exposed.** Static menu displays all 13 nav items to all roles. | **Exposed.** Profile menu links to unrouted pages. | **Medium Risk.** Unfiltered navigation causes student confusion and broken routes (`/dashboard/twin`, `/dashboard/courses`). |
| **Institutional Master Data**<br>`/dashboard/institution` | Read: All Roles<br>Write: `ADMIN`, `DEAN`, `REGISTRAR` | **Misaligned.** Parent route blocks `CHAIRPERSON`. | **Exposed.** Visible to `STUDENT` & `FACULTY` who are bounced on click. | **Partially Aligned.** Subcomponents check `canManage()`, but with role leakage. | **High Risk.** `CHAIRPERSON` cannot access course or CILO-PILO matrix views. `STUDENT` sees administrative link. |
| - *Academic Periods*<br>`/institution/academic-periods` | `ADMIN`, `REGISTRAR`<br>(Term: `DEAN`, `REGISTRAR`) | `roleGuard(['ADMIN', 'DEAN', 'REGISTRAR', 'CHAIRPERSON'])` | N/A (Tab inside Institution) | **Misaligned.** `TermManager` allows `CHAIRPERSON` to toggle windows (fails with 403). | **Medium Risk.** Over-permissive `canManage` in `TermManager` exposes enrollment/grading window toggles. |
| - *Organizational Hierarchy*<br>`/institution/hierarchy` | Read: All<br>Campus: `ADMIN`<br>Dept: `ADMIN`, `DEAN`<br>Prog: `ADMIN`, `DEAN`, `CHAIRPERSON` | `roleGuard(['ADMIN', 'DEAN', 'REGISTRAR', 'CHAIRPERSON'])` | N/A (Tab inside Institution) | **Aligned.** `CampusManager`, `DeptManager`, and `ProgramManager` enforce fine-grained `canManage`. | **Low Risk.** Sub-forms respect role boundaries. |
| - *Course Catalog*<br>`/institution/courses` | Read: All<br>Write: `ADMIN`, `DEAN`, `CHAIRPERSON` | `roleGuard(['ADMIN', 'DEAN', 'REGISTRAR', 'CHAIRPERSON'])` | N/A (Tab inside Institution) | **Aligned.** `CourseCatalogManager` checks `ADMIN`, `DEAN`, `CHAIRPERSON`. | **Medium Risk.** `CHAIRPERSON` is blocked by parent route guard in `app.routes.ts`. |
| - *CILO-PILO Matrix*<br>`/institution/cilo-pilo-matrix` | Read: All<br>Write: `ADMIN`, `DEAN`, `CHAIRPERSON` | `roleGuard(['ADMIN', 'DEAN', 'REGISTRAR', 'CHAIRPERSON'])` | N/A (Tab inside Institution) | **Aligned.** Matrix editing restricted to `ADMIN`, `DEAN`, `CHAIRPERSON`. | **Medium Risk.** Blocked by parent route guard for `CHAIRPERSON`. |
| - *Grading Scales*<br>`/institution/grading-scales` | Read: All<br>Write: `ADMIN`, `DEAN`, `REGISTRAR`<br>Delete: `ADMIN` | `roleGuard(['ADMIN', 'DEAN', 'REGISTRAR', 'CHAIRPERSON'])` | N/A (Tab inside Institution) | **Misaligned.** `canManage` includes `CHAIRPERSON` (fails 403 on edit). Delete visible to non-admins. | **Medium Risk.** Chairperson given illusion of edit rights; delete button shown to non-admins. |
| - *Financial Foundations*<br>`/institution/financials` | Read: All<br>Write: `ADMIN`, `DEAN`<br>Delete: `ADMIN` | `roleGuard(['ADMIN', 'DEAN', 'REGISTRAR', 'CHAIRPERSON'])` | N/A (Tab inside Institution) | **Misaligned.** `canManage` checks `['ADMIN', 'DEAN', 'REGISTRAR', 'CHAIRPERSON']`. | **High Risk.** `REGISTRAR` and `CHAIRPERSON` see "Add Fee", "Add Scholarship", etc., which fail with 403. |
| **Curriculum Designer**<br>`/dashboard/curriculum` | Read: All (inc. `FACULTY`, `STUDENT`)<br>Write: `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` | **Misaligned.** Parent route blocks `FACULTY`. | **Exposed.** Static link shown to `STUDENT`. Hardcoded to ID `/designer/1`. | **Under-permissive.** `hasEditRole` locks out `REGISTRAR`. | **High Risk.** `FACULTY` blocked from reviewing curriculum; `REGISTRAR` cannot edit positions; student bounced on click. |
| **Class Scheduling & Timetables**<br>`/dashboard/scheduling` | Read: All<br>Section Write: `ADMIN`, `DEAN`, `CHAIRPERSON`<br>Room Write: `ADMIN`, `DEAN`, `REGISTRAR` | **Misaligned.** Parent route blocks `FACULTY`. | **Exposed.** Visible to `STUDENT` in sidebar. | **Exposed.** "Add Schedule" button in `section-builder.component.html` has NO role guard! | **High Risk.** `FACULTY` blocked from schedule; "Add Schedule" button shown to all who can access page. |
| **Student Enrollment & Advising**<br>`/dashboard/enrollment` | Advising: All Staff + Student Self<br>Enlistment: `ADMIN`, `REGISTRAR` + Student Self | **Aligned.** `roleGuard(['ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'FACULTY', 'STUDENT'])`. | **Exposed.** Shown to all roles. | **Critical Leak.** "Admit Student" and "Credit Transferee" buttons are completely un-guarded! | **Critical Risk.** Students see "Admit Student" & "Credit Transferee" buttons. Store calls forbidden search API on init. |
| - *Admissions Intake*<br>`CreateStudentRequest` | `ADMIN`, `REGISTRAR` | Inside Advising view | N/A | **Exposed.** Button has no `@if`. | **High Risk.** Students and faculty can click and open the admissions wizard. |
| - *Transferee Crediting*<br>`creditTransfereeCourses` | `ADMIN`, `DEAN`, `REGISTRAR` | Inside Advising view | N/A | **Exposed.** Button has no `@if`. | **High Risk.** Students see "Credit Transferee" button for their own profile. |
| - *Registrar Oversight & Audit*<br>`updateEnrollmentStatus` | `ADMIN`, `DEAN`, `REGISTRAR` | Inside Enrollment view | N/A | **Misaligned.** `isStaff` includes `CHAIRPERSON`, exposing status change action. | **Medium Risk.** Chairperson sees status update buttons that return 403 on backend. |
| **Faculty Gradebook**<br>`/dashboard/grades` | Staff: `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`<br>Instructor: `FACULTY` (Assigned) | **Aligned.** `roleGuard(['ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY'])`. | **Exposed.** Visible to `STUDENT` (bounced on click). | **Broken.** `isAssignedInstructor` fails because `user.id` is `undefined` in `AuthService`. | **Critical Risk.** Legitimate instructors cannot edit grades. Chairperson verification button missing. |

---

## 3. Identified Vulnerabilities, Defects & Leaks

### 3.1 Defect 1: Instructor Identity Stripping (`AuthService.currentUser`)
- **Location:** [`src/app/core/service/authentication/auth-service.ts`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/core/service/authentication/auth-service.ts#L33-L50)
- **Root Cause:**
  ```typescript
  const payload = JSON.parse(atob(token.split('.')[1]...));
  return {
    username: payload.preferred_username || payload.username || 'Student User',
    email: payload.email || '',
    role: primaryRole,
    roles: normalizedRoles
    // MISSING: id: Number(payload.sub)
  };
  ```
- **Consequence:** In [`faculty-gradebook.component.ts`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/features/gradebook/faculty-gradebook.component.ts#L153-L158), `(user as { id?: number }).id === ros.primaryInstructorId` compares `undefined === 5`, resulting in `false`. Faculty instructors are permanently treated as non-assigned, completely locking them out of saving scores or editing class records.

### 3.2 Defect 2: Student Initialization Privilege Trap (IDOR & 403 Error)
- **Location:** [`src/app/features/enrollment/state/enrollment.store.ts`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/features/enrollment/state/enrollment.store.ts#L168-L170)
- **Root Cause:** `loadInitialData()` unconditionally calls `searchStudents('')`, which queries `GET /api/v1/students/search`.
- **Backend Contract:** `GET /api/v1/students/search` requires `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY')")`.
- **Consequence:** When an enrolled student accesses the enrollment tab, the application immediately fires a forbidden request, presenting a warning toast ("Access Denied: You do not have the required permissions"). If any students are returned, line 107 executes `this.setStudentId(list[0].id)`, attempting to bind the student to another student's ID (IDOR risk).

### 3.3 Defect 3: Parent Route Guard Inconsistencies Locking Out Valid Staff
- **Location:** [`src/app/app.routes.ts`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/app.routes.ts#L43-L59)
- **Root Cause:**
  1. `path: 'curriculum'`: Protected with `['ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR']`. Blocks `FACULTY` from accessing [`curriculum.routes.ts`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/features/curriculum/curriculum.routes.ts#L17) which allows `FACULTY`.
  2. `path: 'institution'`: Protected with `['ADMIN', 'DEAN', 'REGISTRAR']`. Blocks `CHAIRPERSON` from accessing [`institution.routes.ts`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/features/institution/institution.routes.ts#L34) which allows `CHAIRPERSON` for Courses and CILO-PILO Matrix.
  3. `path: 'scheduling'`: Protected with `['ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR']`. Blocks `FACULTY` from viewing timetable assignments.

### 3.4 Defect 4: Unprotected Action Buttons in Enrollment Advising View
- **Location:** [`src/app/features/enrollment/components/student-advising/student-advising.component.html`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/features/enrollment/components/student-advising/student-advising.component.html#L27-L42)
- **Root Cause:**
  ```html
  <p-button label="Admit Student" icon="pi pi-user-plus" (onClick)="openAdmissionsDialog()"></p-button>
  <p-button label="Credit Transferee" icon="pi pi-file-edit" (onClick)="openCreditingDialog()"></p-button>
  ```
  Neither button has an `@if` condition checking `isAdmin`, `isRegistrar`, or `isDean`.
- **Consequence:** Enrolled students viewing their own advising checklist are shown buttons to register new students and credit transferee courses.

### 3.5 Defect 5: Unfiltered Static Sidebar Navigation
- **Location:** [`src/app/features/dashboard/dashboard-layout/dashboard-layout.component.ts`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/features/dashboard/dashboard-layout/dashboard-layout.component.ts#L74-L103)
- **Root Cause:** `navSections` is a static array without role metadata or computed filtering.
- **Consequence:** Students see links to administrative modules ("Institutional Registry", "Curriculum Designer", "Class Scheduling", "Grades & Progress"). Clicking them triggers silent redirects to `/dashboard`, producing a broken user experience.

---

## 4. Step-by-Step Implementation Roadmap

Below are concrete, production-ready implementation steps to remediate all identified defects and establish bulletproof client-side RBAC.

```
+---------------------------------------------------------------------------------------------------+
|                                       RBAC REMEDIATION ROADMAP                                    |
+---------------------------------------------------------------------------------------------------+
|  Step 1: Auth State & User Context   -> Extract User ID (`sub`) and build reactive role signals   |
|  Step 2: Functional Route Guards      -> Synchronize parent/child routes; add 403 navigation      |
|  Step 3: Dynamic Navigation Engine   -> Implement computed signal for role-filtered sidebar items |
|  Step 4: Structural HasRole Directive -> Create `*hasRole` & `@if` template authorization helpers |
|  Step 5: Student Store Separation     -> Fix EnrollmentStore init logic to use `/students/me`     |
|  Step 6: Dedicated 403 Forbidden View -> Create polished Access Denied component & error handler  |
+---------------------------------------------------------------------------------------------------+
```

---

### Step 1: Auth State & Role Model Enhancement

Update the user context model and [`AuthService`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/core/service/authentication/auth-service.ts) to parse and expose `id` from the JWT `sub` claim.

#### 1.1 Update `src/app/core/models/auth.model.ts`
```typescript
export interface UserContext {
  id: number | null;
  username: string;
  email: string;
  role: string;
  roles: string[];
}
```

#### 1.2 Update `src/app/core/service/authentication/auth-service.ts`
```typescript
readonly currentUser = computed<UserContext>(() => {
  const token = this.accessTokenSignal();
  if (!token) {
    return { id: null, username: '', email: '', role: 'GUEST', roles: [] };
  }
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    const rawRoles: string[] = Array.isArray(payload.roles)
      ? payload.roles
      : (payload.realm_access?.roles || []);
    const normalizedRoles = rawRoles.map((r: string) => r.replace(/^ROLE_/, '').toUpperCase());
    const primaryRole = normalizedRoles[0] || 'STUDENT';
    const userId = payload.sub ? Number(payload.sub) : null;

    return {
      id: isNaN(userId as number) ? null : userId,
      username: payload.preferred_username || payload.username || 'User',
      email: payload.email || '',
      role: primaryRole,
      roles: normalizedRoles
    };
  } catch {
    return { id: null, username: '', email: '', role: 'GUEST', roles: [] };
  }
});
```

---

### Step 2: Route Hierarchy & Guard Alignment

Fix the parent-child route guard conflicts in [`app.routes.ts`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/app.routes.ts) so authorized staff are not prematurely blocked.

#### 2.1 Update `src/app/app.routes.ts`
```typescript
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'forgot-password', component: ForgotPasswordComponent, canActivate: [guestGuard] },
  { path: 'reset-password', component: ResetPasswordComponent, canActivate: [guestGuard] },
  { path: 'forbidden', component: ForbiddenComponent }, // Step 6
  {
    path: 'dashboard',
    component: DashboardLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', component: DashboardComponent },
      {
        path: 'institution',
        // Allow CHAIRPERSON so they can reach courses & cilo-pilo-matrix
        canActivate: [roleGuard(['ADMIN', 'DEAN', 'REGISTRAR', 'CHAIRPERSON'])],
        loadChildren: () => import('./features/institution/institution.routes').then(m => m.INSTITUTION_ROUTES)
      },
      {
        path: 'curriculum',
        // Allow FACULTY to view published curricula
        canActivate: [roleGuard(['ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY'])],
        loadChildren: () => import('./features/curriculum/curriculum.routes').then(m => m.CURRICULUM_ROUTES)
      },
      {
        path: 'scheduling',
        // Allow FACULTY to view schedules
        canActivate: [roleGuard(['ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY'])],
        loadChildren: () => import('./features/scheduling/scheduling.routes').then(m => m.SCHEDULING_ROUTES)
      },
      {
        path: 'enrollment',
        loadChildren: () => import('./features/enrollment/enrollment.routes').then(m => m.ENROLLMENT_ROUTES)
      },
      {
        path: 'grades',
        canActivate: [roleGuard(['ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY'])],
        loadComponent: () => import('./features/gradebook/faculty-gradebook.component').then(m => m.FacultyGradebookComponent)
      },
      { path: '**', redirectTo: '' }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
```

#### 2.2 Update `src/app/core/guards/authorization/role.guard.ts`
Redirect unauthorized users to `/forbidden` instead of silently bouncing to `/dashboard`.
```typescript
function checkRoles(roles: string[], authService: AuthService, router: Router, state: RouterStateSnapshot) {
  const checkRole = (): boolean => {
    if (roles.length === 0) return true;
    return authService.hasAnyRole(roles);
  };

  if (authService.isAuthenticated()) {
    if (checkRole()) return true;
    // Redirect to Forbidden page with attempted URL context
    return router.createUrlTree(['/forbidden'], { queryParams: { blockedUrl: state.url } });
  }

  return authService.refreshToken().pipe(
    map(() => {
      if (checkRole()) return true;
      return router.createUrlTree(['/forbidden'], { queryParams: { blockedUrl: state.url } });
    }),
    catchError(() => of(router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } })))
  );
}
```

---

### Step 3: Dynamic Navigation Engine (Role-Filtered Sidebar)

Add role requirements to navigation items and implement reactive menu filtering in [`DashboardLayoutComponent`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/features/dashboard/dashboard-layout/dashboard-layout.component.ts).

#### 3.1 Update `NavItem` Interface
```typescript
export interface NavItem {
  label: string;
  icon: string;
  routerLink: string;
  exact?: boolean;
  badge?: string;
  badgeSeverity?: 'info' | 'success' | 'warn' | 'danger';
  roles?: string[]; // Allowed roles (undefined = all authenticated users)
}
```

#### 3.2 Update `DashboardLayoutComponent`
```typescript
readonly allNavSections: NavSection[] = [
  {
    title: 'Overview',
    items: [
      { label: 'Dashboard', icon: 'pi pi-home', routerLink: '/dashboard', exact: true }
    ]
  },
  {
    title: 'Institutional Management',
    items: [
      {
        label: 'Institutional Registry',
        icon: 'pi pi-building',
        routerLink: '/dashboard/institution',
        roles: ['ADMIN', 'DEAN', 'REGISTRAR', 'CHAIRPERSON']
      },
      {
        label: 'Curriculum Designer',
        icon: 'pi pi-sitemap',
        routerLink: '/dashboard/curriculum/designer/1',
        roles: ['ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY']
      },
      {
        label: 'Class Scheduling',
        icon: 'pi pi-calendar-plus',
        routerLink: '/dashboard/scheduling',
        roles: ['ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY']
      }
    ]
  },
  {
    title: 'Academic Services',
    items: [
      {
        label: 'Enrollment & Advising',
        icon: 'pi pi-user-plus',
        routerLink: '/dashboard/enrollment',
        roles: ['ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT']
      },
      {
        label: 'Faculty Gradebook',
        icon: 'pi pi-chart-line',
        routerLink: '/dashboard/grades',
        roles: ['ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY']
      }
    ]
  }
];

// Reactive Computed Signal: Automatically updates when auth state changes
readonly navSections = computed<NavSection[]>(() => {
  return this.allNavSections
    .map(section => ({
      ...section,
      items: section.items.filter(item => {
        if (!item.roles || item.roles.length === 0) return true;
        return this.authService.hasAnyRole(item.roles);
      })
    }))
    .filter(section => section.items.length > 0);
});
```

---

### Step 4: Template-Level Security (Custom `HasRoleDirective`)

Create a reusable structural directive `*hasRole` for declarative template authorization.

#### 4.1 Create `src/app/core/directives/has-role.directive.ts`
```typescript
import { Directive, Input, TemplateRef, ViewContainerRef, inject, effect } from '@angular/core';
import { AuthService } from '../service/authentication/auth-service';

@Directive({
  selector: '[hasRole]',
  standalone: true
})
export class HasRoleDirective {
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);
  private readonly authService = inject(AuthService);

  private requiredRoles: string[] = [];
  private isVisible = false;

  @Input() set hasRole(roles: string | string[]) {
    this.requiredRoles = Array.isArray(roles) ? roles : [roles];
    this.updateView();
  }

  constructor() {
    // Re-evaluate automatically if user role signal changes
    effect(() => {
      // Register dependency on currentUser
      this.authService.currentUser();
      this.updateView();
    });
  }

  private updateView(): void {
    const hasPermission = this.authService.hasAnyRole(this.requiredRoles);

    if (hasPermission && !this.isVisible) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.isVisible = true;
    } else if (!hasPermission && this.isVisible) {
      this.viewContainer.clear();
      this.isVisible = false;
    }
  }
}
```

#### 4.2 Guard Critical Template Actions
1. **Student Advising Header Actions (`student-advising.component.html`):**
   ```html
   @if (authService.hasAnyRole(['ADMIN', 'REGISTRAR'])) {
     <p-button label="Admit Student" icon="pi pi-user-plus" severity="success" (onClick)="openAdmissionsDialog()"></p-button>
   }
   @if (authService.hasAnyRole(['ADMIN', 'DEAN', 'REGISTRAR'])) {
     <p-button label="Credit Transferee" icon="pi pi-file-edit" severity="warn" (onClick)="openCreditingDialog()"></p-button>
   }
   ```
2. **Scheduling Action Button (`section-builder.component.html`):**
   ```html
   @if (auth.hasAnyRole(['ADMIN', 'DEAN', 'CHAIRPERSON'])) {
     <p-button label="Add Schedule" icon="pi pi-plus" severity="success" (onClick)="openCreateModal()"></p-button>
   }
   ```
3. **Enrollment Status Update (`enrollment-audit.component.html`):**
   ```html
   @if (store.canUpdateStatus()) { <!-- ADMIN, DEAN, REGISTRAR -->
     <p-button label="Update Status" (onClick)="updateStatus(...)"></p-button>
   }
   ```

---

### Step 5: Enrollment Store Separation for Students

Eliminate the student privilege trap and IDOR vulnerability by branching student initialization in [`EnrollmentStore`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-frontend/src/app/features/enrollment/state/enrollment.store.ts).

#### 5.1 Update `EnrollmentStore.loadInitialData()`
```typescript
loadInitialData(): void {
  const currentUser = this.authService.currentUser();

  if (currentUser.role === 'STUDENT') {
    // Student path: Fetch own profile via /api/v1/students/me (NO search call)
    this.isLoading.set(true);
    this.enrollmentApi.getCurrentStudentProfile().pipe(
      takeUntilDestroyed(this.destroyRef),
      tap(profile => {
        this.setStudentId(profile.id);
        this.loadTermsAndAdvising(profile.id);
      }),
      catchError(err => {
        this.errorMessage.set('Could not load student profile.');
        return of(null);
      }),
      finalize(() => this.isLoading.set(false))
    ).subscribe();
  } else {
    // Institutional Staff path: Load student search directory
    this.searchStudents('');
    this.loadTermsOnly();
  }
}
```

---

### Step 6: Dedicated 403 Forbidden Component & Navigation

Provide clear visual feedback and a recovery path when an unauthorized action or route is attempted.

#### 6.1 Create `src/app/features/forbidden/forbidden.component.ts`
```typescript
import { Component, inject } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';

@Component({
  selector: 'app-forbidden',
  standalone: true,
  imports: [ButtonModule, CardModule],
  template: `
    <div class="flex items-center justify-center min-h-[70vh] p-4">
      <div class="max-w-md w-full text-center bg-white dark:bg-slate-900 shadow-xl rounded-2xl p-8 border border-red-100 dark:border-red-950">
        <div class="w-16 h-16 mx-auto mb-4 bg-red-50 dark:bg-red-950/50 rounded-full flex items-center justify-center text-red-600 dark:text-red-400">
          <i class="pi pi-shield text-3xl"></i>
        </div>
        <h1 class="text-2xl font-bold text-slate-900 dark:text-slate-100 mb-2">Access Restricted</h1>
        <p class="text-sm text-slate-600 dark:text-slate-400 mb-6">
          Your current institutional account does not possess the permissions required to view this module.
        </p>
        <p-button 
          label="Return to Dashboard" 
          icon="pi pi-arrow-left" 
          styleClass="w-full"
          (onClick)="goHome()">
        </p-button>
      </div>
    </div>
  `
})
export class ForbiddenComponent {
  private readonly router = inject(Router);
  goHome(): void {
    this.router.navigate(['/dashboard']);
  }
}
```

---

## 5. Summary of Verification Test Scenarios

The following verification tests must be added to validate the implementation:

1. **`auth-service.spec.ts`:** Verify that decoding a JWT with `sub: "42"` sets `currentUser().id === 42`.
2. **`faculty-gradebook.component.spec.ts`:** Verify that when `currentUser().id === 5` and `roster.primaryInstructorId === 5`, `isAssignedInstructor()` evaluates to `true` and `canEditGrades()` is enabled.
3. **`role.guard.spec.ts`:** Verify that navigating to `/dashboard/curriculum` as `FACULTY` returns `true`, and navigating as `STUDENT` redirects to `/forbidden`.
4. **`dashboard-layout.component.spec.ts`:** Verify that for a `STUDENT` user, `navSections()` contains only `Overview` and `Student Services`, completely omitting `Institutional Management` items.
5. **`enrollment.store.spec.ts`:** Verify that for a `STUDENT` user, `loadInitialData()` invokes `getCurrentStudentProfile()` and never triggers `searchStudents()`.
