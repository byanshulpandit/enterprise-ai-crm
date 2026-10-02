# Enterprise AI-CRM Platform (`CS-CRM-2026`)
# Enterprise Design System Specification
# Document Version: 2.0.0 — Status: Approved Verified Baseline

---

## 1. Visual Philosophy & Design Manifesto

The **Enterprise AI-CRM Design System** is deliberately engineered for high-density, professional business workflows. It rejects the aesthetic tropes of generic SaaS landing pages, consumer applications, and AI-generated dashboard templates.

### Core Visual Principles
1. **Utility-First Density:** Information density takes precedence over decorative white space. Tables, grids, and forms present maximum operational clarity with compact enterprise spacing.
2. **Restrained, Purposeful Palette:** Built upon a foundational canvas of rich **graphite and charcoal**, illuminated with an intentional **muted warm amber / bronze** accent. Visual saturation is strictly reserved for actionable telemetry and semantic status indicators.
3. **No Fluff or Fake UI:** Zero giant floating cards, zero rainbow gradients, zero excessive glassmorphism, and zero neon accent colors. Every visual element maps directly to backend domain state.
4. **Subtle Elevation & Crisp Boundaries:** Interfaces use sharp 1px borders with 2px-4px border radii and minimal, low-blur elevation shadows to establish unmistakable physical hierarchy without visual clutter.
5. **Ergonomic Typography:** Clean, modern monospace and sans-serif typography (`Inter`, `JetBrains Mono`) with calibrated line heights ensuring fatigue-free scanning across thousands of data points.

---

## 2. Color Palette & Token Architecture

The design system provides a high-contrast dark foundation optimized for prolonged professional workstation usage, paired with a clean, high-legibility light mode surface standard.

### 2.1 Foundation Tokens (Dark Canvas Baseline)
```css
:root {
  /* Surface & Canvas Hierarchy */
  --crm-canvas-background:       #0d1117;   /* Deep charcoal app backdrop */
  --crm-surface-ground:          #161b22;   /* Main viewport surface */
  --crm-surface-panel:           #1f242c;   /* Elevated panels, sidebars, cards */
  --crm-surface-raised:          #282e38;   /* Dialogs, dropdowns, popovers */
  --crm-surface-hover:           #323945;   /* Interactive row & button hover */
  --crm-surface-selected:        #3b4352;   /* Active selection highlight */

  /* Structural Borders */
  --crm-border-subtle:           #2d333b;   /* Low-contrast divider lines */
  --crm-border-contrast:         #444c56;   /* Input borders, card outlines */
  --crm-border-strong:           #636e7b;   /* Active focus & header borders */

  /* Brand Accent: Muted Warm Amber / Bronze */
  --crm-accent-primary:          #d97706;   /* Core action amber (HSL 37, 91%, 44%) */
  --crm-accent-hover:            #f59e0b;   /* Lighter hover amber */
  --crm-accent-active:           #b45309;   /* Pressed state deep amber */
  --crm-accent-subtle:           rgba(217, 119, 6, 0.12); /* Tinted badge & row background */
  --crm-accent-border:           rgba(217, 119, 6, 0.40); /* Amber outline highlight */

  /* Neutral Text Hierarchy */
  --crm-text-primary:            #f0f6fc;   /* High-contrast headings and primary labels */
  --crm-text-secondary:          #8b949e;   /* Supporting metadata, table headers */
  --crm-text-muted:              #6e7681;   /* Disabled text, placeholder hints */
  --crm-text-inverse:            #0d1117;   /* Text on amber primary buttons */

  /* Semantic Status Tokens */
  --crm-status-success-base:     #10b981;   /* Forest Emerald (Sent, Active, Completed) */
  --crm-status-success-bg:       rgba(16, 185, 129, 0.12);
  --crm-status-success-border:   rgba(16, 185, 129, 0.35);

  --crm-status-warning-base:     #f59e0b;   /* Warm Amber (Draft, Fallback, Partial) */
  --crm-status-warning-bg:       rgba(245, 158, 11, 0.12);
  --crm-status-warning-border:   rgba(245, 158, 11, 0.35);

  --crm-status-error-base:       #ef4444;   /* Crimson Red (Failed, Deactivated, Error) */
  --crm-status-error-bg:         rgba(239, 68, 68, 0.12);
  --crm-status-error-border:     rgba(239, 68, 68, 0.35);

  --crm-status-info-base:        #38bdf8;   /* Steel Sky Blue (Running, Pending) */
  --crm-status-info-bg:          rgba(56, 189, 248, 0.12);
  --crm-status-info-border:      rgba(56, 189, 248, 0.35);
}
```

---

## 3. Typography Scale & Hierarchy

- **Primary Font Family:** `'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif`
- **Monospace Code/Data Family:** `'JetBrains Mono', 'Fira Code', 'Roboto Mono', monospace`

### Typography Matrix
| Style Class | Font Size | Weight | Line Height | Tracking | Usage |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `display-lg` | 24px (`1.5rem`) | 600 (SemiBold) | 32px | -0.02em | Main Page Titles, Dashboard Primary Header |
| `heading-md` | 18px (`1.125rem`)| 600 (SemiBold) | 24px | -0.01em | Panel Titles, Modal Headers, Section Dividers |
| `heading-sm` | 15px (`0.9375rem`)| 600 (SemiBold) | 20px | 0.00em | Card Titles, Drawer Section Titles |
| `body-regular`| 13px (`0.8125rem`)| 400 (Regular) | 18px | 0.00em | Standard Grid Rows, Body Copy, Descriptions |
| `body-medium` | 13px (`0.8125rem`)| 500 (Medium) | 18px | 0.00em | Form Labels, Table Primary Column, Button Text |
| `caption-sm` | 11px (`0.6875rem`)| 500 (Medium) | 14px | +0.02em | Table Headers (Uppercase), Badges, Timestamps |
| `code-data` | 12px (`0.75rem`) | 400 (Mono) | 16px | 0.00em | IDs, Monetary Values, AST JSON, Tokens |

---

## 4. Spacing Scale, Radius & Elevation

### 4.1 Enterprise Spacing Scale
```css
--crm-space-2xs: 2px;
--crm-space-xs:  4px;
--crm-space-sm:  8px;
--crm-space-md:  12px;
--crm-space-lg:  16px;
--crm-space-xl:  24px;
--crm-space-2xl: 32px;
```

### 4.2 Border Radius
```css
--crm-radius-xs: 2px;   /* Small badges */
--crm-radius-sm: 4px;   /* Standard buttons, input fields, badges */
--crm-radius-md: 6px;   /* Cards, panels, modal dialogs */
--crm-radius-lg: 8px;   /* Main application container */
```

### 4.3 Elevation & Shadows
```css
--crm-shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.40);
--crm-shadow-md: 0 3px 6px rgba(0, 0, 0, 0.50), 0 1px 2px rgba(0, 0, 0, 0.30);
--crm-shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.65), 0 2px 4px rgba(0, 0, 0, 0.40);
```

---

## 5. Component Style Specifications

### 5.1 Buttons & Action Controls
- **Primary Button (`btn-primary`):** Solid Warm Amber background (`#d97706`), dark text (`#0d1117`), font-weight 600, 4px radius. Used strictly for primary workflow commitments (e.g., "Save Segment", "Launch Campaign", "Import Data").
- **Secondary / Outline Button (`btn-secondary`):** Graphite surface with contrast border (`#444c56`), high-contrast text (`#f0f6fc`), hover elevates to `#323945`.
- **Tertiary / Ghost Button (`btn-ghost`):** Borderless, subtle text, hover adds background `#282e38`. Used for inline table actions.
- **Destructive Button (`btn-danger`):** Crimson red border and text; on hover transitions to solid crimson background with white text. Requires confirmation dialog.

### 5.2 Forms & Input Controls
- Compact vertical padding (height: 32px for standard inputs).
- Charcoal background (`#161b22`) with subtle border (`#444c56`).
- **Focus State:** 1px solid Warm Amber outline (`#d97706`) with 0 0 0 2px `rgba(217, 119, 6, 0.20)` ring.
- **Error State:** Red border (`#ef4444`) with crisp inline error text below the input field.

### 5.3 High-Density Data Grids (`vaadin-grid`)
- Row height fixed at compact 38px.
- Alternate row striping using subtle luminance shifts (`#161b22` and `#1a1f27`).
- Table headers: 11px uppercase, tracking +0.03em, muted color (`#8b949e`), pinned to top during scroll.
- Numeric columns (Spend, Visits, Percentages) strictly right-aligned with monospace formatting.
- Interactive row hover highlights with `#282e38`.

### 5.4 Semantic Status Badges
Badges are compact pills (height: 20px, font-size: 11px, font-weight 600) with a 6px status dot:
- **COMPLETED / SENT / ACTIVE (Success):** Forest Emerald dot and text on emerald-tinted background.
- **DRAFT / PARTIAL_SUCCESS (Warning):** Warm Amber dot and text on amber-tinted background.
- **FAILED / DEACTIVATED (Error):** Crimson Red dot and text on red-tinted background.
- **RUNNING / PENDING (Info):** Steel Sky Blue dot and text on blue-tinted background.

---

## 6. Responsive Breakpoint Matrix

| Breakpoint | Target Viewport | Layout Adaptations |
| :--- | :--- | :--- |
| **Desktop (Default)** | `>= 1200px` | Full multi-column grids, persistent expanded sidebar navigation, side-by-side forms and previews. |
| **Tablet / Laptop** | `768px - 1199px` | Sidebar collapses to icon-only rail; table columns hide secondary timestamps; 4-column KPI grids wrap into 2x2 cards. |
| **Mobile** | `< 768px` | Sidebar becomes off-canvas hamburger drawer; tables convert into stacked card lists; form layouts stack vertically. |

---

## 7. Accessibility & WCAG 2.1 AA Compliance

1. **Color Contrast:** All body text meets minimum contrast ratio of 4.5:1 against respective panel surfaces; headings and large buttons meet 7:1 contrast.
2. **Keyboard Navigation:** Every interactive component is accessible via `Tab`, `Shift+Tab`, `Enter`, and `Space`.
3. **Focus Indicators:** Unambiguous 2px focus outline in Warm Amber visible across all active elements.
4. **ARIA & Screen Readers:** Modal dialogs enforce focus trapping and register `role="dialog"` with `aria-labelledby` attributes. Status updates (such as delivery count changes) declare `aria-live="polite"`.
