# Master product plan document

## Document structure (sections to write)

### 1. Vision and positioning

- **One-liner:** ArtDrop as a **creator-first art community** (digital + traditional): discovery feed, collections, conversation, timed challenges—not a generic “another Pinterest.”
- **Differentiation (“spin”):**
  - **Home = calm browse:** infinite/sectioned scroll, algorithm-light or chronological options, emphasis on **medium, process, and artist voice** (e.g. optional “studio notes,” WIP vs finished).
  - **Commerce is optional and secondary:** artworks default to **not for sale**; creators opt in per post with a **clear “Available” / “Edition / Original”** affordance so the app never feels like a mall.
  - **Purchases = dedicated flow:** tapping a sellable card opens a **checkout-oriented detail page** (price, edition info, shipping scope, terms)—separate mental model from the main feed card.
  - Optional future hooks: **challenges as prompts** (weekly theme), **collections as curated exhibitions**, **comments with threading** (aligned with Comment + `parent_comment_id` in your spec).

### 2. Personas and core user stories (short)

- Guest / registered viewer; creator; buyer; moderator/admin (ties to `User.role` and JWT labs).

### 3. Domain model alignment (from Projekt 15 table + labs)

- Summarize entities: **Artwork, Collection, Comment, Challenge, User** with intended relationships.
- **Explicit “spec vs product” note:** e.g. `Collection.artwork_id` in the brief implies a single FK; a richer product likely wants **many-to-many** (collection ↔ artworks). Same for **Challenge ↔ Artwork** (lab suggests `@ManyToOne` from Challenge to Artwork; product-wise you may later want **submissions** or **many artworks per challenge**). The master plan should **flag these for a future migration** without blocking the course track.

### 4. Lab roadmap mapping

| Lab | Product outcome (1–2 lines each) |
|-----|----------------------------------|
| 1 | REST foundation, DTO vs domain, list/detail APIs |
| 2 | Create/delete + validation, search by title/medium |
| 3 | React list/detail with mock data |
| 4 | Router, API integration, CORS, create + optimistic delete |
| 5 | H2 + JDBC persistence |
| 6 | JPA, relationships, challenge API + React surfacing on artwork detail |
| 7 | JWT auth, roles, secured mutations |
| 8 | Login, `AuthContext`, Axios interceptors, protected routes, RBAC UI |
| 9 | Controller/service tests, coverage goals |
| 10 | Quartz jobs (challenge deadlines, `isActive` flips) |

### 5. Feature backlog (post-lab / product growth)

Group into **MVP (course-aligned)**, **Phase 2 (differentiation)**, **Phase 3 (commerce + polish)**:

- **MVP:** feed-quality list/detail, CRUD artworks, comments display, challenges on detail, auth, basic profiles.
- **Phase 2:** richer collections, challenge participation UX, notifications (out of scope for labs—mention as future).
- **Phase 3 (optional commerce):** `Artwork` (or satellite **Listing**) fields for **price, currency, stock/edition, purchase URL vs native checkout**; Stripe Checkout or Payment Links; webhooks for payment confirmation; legal (terms, refund policy, digital vs physical goods).

### 6. Payments: Stripe and Croatia

- **Stripe is available for businesses in Croatia** (accounts and accepting payments per Stripe’s Croatia resources and global availability). Link official pages in the doc: [Stripe – payments in Croatia](https://stripe.com/en-hr/resources/more/payments-in-croatia), [Stripe global](https://stripe.com/global).
- **Caveats to state plainly:** VAT/invoicing rules, **seller of record** vs marketplace model, and that **course projects** should start with **test mode** and avoid handling real card data on your own server (use Checkout / Elements as documented).

### 7. Non-functional requirements

- Security (JWT, CORS, HTTPS in prod), performance (pagination for feed), accessibility baseline, i18n if you ever target HR/EN UI.
- Frontend UI uses Tailwind CSS as the primary styling system and follows a mobile-first responsive approach (base styles for small screens, then breakpoint enhancements for larger viewports).

### 8. Monorepo layout reference

- Point to [`ArtDrop/`](ArtDrop/) (Spring Boot) and [`artdropapp-frontend/`](artdropapp-frontend/) so newcomers know where code lives.