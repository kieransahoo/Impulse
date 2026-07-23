# Impulse Product Design System

## 1. Document purpose

This document is the design source of truth for the current Impulse web
prototype and the future Android application built with Kotlin and Jetpack
Compose.

It defines:

- Product experience principles
- Information architecture and navigation
- Core user flows
- Visual design tokens
- Reusable components
- Interaction and content rules
- Loading, empty, success, error, and offline states
- Accessibility requirements
- Responsive web and Android Compose guidance

The goal is visual and behavioral consistency across the web test surface,
browser extension, and Android application. It is not an instruction to copy
the desktop web layout directly onto a phone.

## 2. Product experience

### Product promise

Impulse turns content a person deliberately saves into useful personal
knowledge, then retrieves that knowledge to help them decide, plan, compare,
and act.

```text
Save → Understand → Remember → Retrieve → Act
```

### Primary user outcome

A user should be able to save something with minimal effort and later receive
an actionable response that clearly shows which saved memories informed it.

### Experience principles

1. **Outcome before technology**  
   Talk about memories, sources, answers, and plans. Do not expose embeddings,
   RAG, model names, UUIDs, or internal processing terminology in the normal
   product experience.

2. **Capture must be effortless**  
   A URL is sufficient to begin. Collection names and notes are optional and
   may be suggested automatically.

3. **Trust must be visible**  
   Personalized answers and plan steps cite the memories that support them.
   General AI knowledge must be labeled separately.

4. **Processing must feel observable**  
   Show what is queued, reading, understanding, ready, or blocked. Never leave
   the user with a silent spinner.

5. **Memory must remain controllable**  
   Users can inspect, edit, organize, retry, or delete memories.

6. **One clear next action**  
   Every screen has one dominant action. Secondary actions should not compete
   visually with it.

7. **Progressive disclosure**  
   Show summaries first. Reveal technical details, reasons, scores, and
   developer controls only when requested.

## 3. Product terminology

| Use | Avoid in user-facing UI |
|---|---|
| Memory | Vector, embedding, document chunk |
| Saved source | Ingestion payload |
| Ready | Processed |
| Use your memories | Retrieve and reason |
| Personalized plan | LLM/RAG response |
| Based on your saved sources | Grounding pipeline |
| Needs attention | Processing failure |
| Workspace or account | User UUID |

“Personalized” or “based on your memories” may only be used when the result
contains relevant cited memories. If the system uses general model knowledge,
say so explicitly.

## 4. Information architecture

### Android primary navigation

Use a four-destination bottom navigation:

1. **Home** — prompt, recent activity, suggested outcomes
2. **Memories** — browse, search, filter, collections
3. **Plans** — active and saved plans
4. **Profile** — account, privacy, integrations, developer settings

Use a centered or prominent floating `Add` action when appropriate. On compact
screens it opens the capture sheet; on larger screens it may be a persistent
button.

### Web test-page structure

The web page may remain a single page, organized in this order:

1. Header and readiness
2. Primary “What do you want to do?” prompt
3. Suggested use cases
4. Add knowledge
5. Recent memories and collections
6. Current result or plan
7. Collapsed developer settings

The editable development user ID belongs in developer settings, not in the
primary journey.

### Deep-link destinations

The Android app should support internal destinations for:

- Add URL
- Memory details
- Collection details
- Search results
- Plan details
- Source details
- Processing issue

## 5. Core user flows

### 5.1 Capture a shared link

```text
Android share sheet
  → Impulse capture preview
  → Optional note or collection
  → Save
  → Background processing status
  → Memory ready
  → View memory or dismiss
```

The share target must prefill URL, source title, thumbnail, and platform when
available. Saving should require one tap after preview.

### 5.2 Paste one or more links

```text
Tap Add
  → Paste links
  → Validate each link
  → Optional purpose
  → Save
  → Per-source progress
  → Collection summary
```

Collection name is optional. Suggest one from the processed topics.

### 5.3 Use saved knowledge

```text
Enter goal or question
  → Detect intent
  → Retrieve relevant memories
  → Answer / Plan / Compare / Recommend
  → Show citations
  → User edits, saves, or acts
```

Manual search is available in Memories but is not a prerequisite for planning.

### 5.4 Weak or missing grounding

If relevant memory is insufficient:

- Do not pretend the result is personalized.
- Explain what relevant content is missing.
- Offer `Search all memories`, `Add a source`, or `Use general suggestions`.
- Clearly label any general suggestion as not based on saved memories.

### 5.5 Plan completion

Each plan step can be:

- Marked complete
- Expanded to show its reason
- Opened to inspect cited memories
- Edited
- Skipped

Progress persists across sessions. Completion should produce a quiet
confirmation, not a disruptive celebration.

## 6. Visual direction

### Design character

The current visual language is **editorial, warm, intelligent, and human**.
It deliberately avoids a cold enterprise dashboard or generic AI gradient
style.

Characteristics:

- Warm paper background
- Near-black ink
- Burnt orange primary accent
- Lime highlight for successful intelligence and useful output
- Serif display typography paired with clean sans-serif UI typography
- Rounded cards with restrained shadows
- Generous whitespace
- Small uppercase editorial labels

### Brand mark

The current mark is a white italic `I` inside a near-black rounded square.

- Web header: 34 × 34 px, 9 px radius
- Android app bar: 32 × 32 dp, 9 dp radius
- Launcher icon: create an adaptive icon derived from the same mark
- Minimum clear space: 8 dp on every side

## 7. Color system

### Core light theme

| Token | Hex | Role |
|---|---:|---|
| `Ink` | `#171714` | Primary text, dark surfaces |
| `Muted` | `#69685F` | Secondary text |
| `Paper` | `#F5F3EC` | App background |
| `Surface` | `#FFFDF7` | Primary cards and sheets |
| `SurfaceBright` | `#FFFEFB` | Inputs and nested cards |
| `Outline` | `#DCD8CA` | Borders and dividers |
| `Primary` | `#E85D35` | Main action and accent |
| `PrimaryPressed` | `#BC3F20` | Pressed/strong accent |
| `Secondary` | `#D6EE72` | Useful output and secondary action |
| `Success` | `#26734D` | Ready and completed |
| `Error` | `#A33D32` | Failure and destructive state |
| `Warning` | `#D79C35` | Processing and attention |
| `DarkSurface` | `#20201D` | Featured plan container |
| `DarkSurfaceRaised` | `#292925` | Source card on dark surface |
| `DarkInput` | `#2D2D29` | Input on dark surface |
| `DarkOutline` | `#44443E` | Dark-theme divider |
| `OnDarkMuted` | `#AAA99F` | Secondary text on dark surfaces |

### Supporting state containers

| Token | Hex |
|---|---:|
| `SuccessContainer` | `#E6F3E9` |
| `SuccessContainerSoft` | `#EFF8F0` |
| `ErrorContainer` | `#FAE7E3` |
| `ErrorContainerSoft` | `#FFF0EC` |
| `AccentContainer` | `#F4DACF` |
| `NeutralContainer` | `#EFECDF` |

### Color usage rules

- `Primary` is reserved for the dominant action and important emphasis.
- `Secondary` highlights useful generated outcomes; do not use it as decoration
  on every card.
- Status must never rely on color alone. Pair color with a label or icon.
- Body text on `Paper` or `Surface` uses `Ink`.
- Body text should not use `Primary`; reserve it for actions and short accents.
- Meet WCAG AA contrast for normal text and interactive controls.

### Dark theme guidance

The current web design uses a dark featured planning surface, not a complete
dark theme. For Android dark mode:

- Preserve orange and lime brand accents.
- Use a warm charcoal background rather than pure black.
- Recalculate all content colors for accessible contrast.
- Do not simply invert the light palette.

## 8. Typography

### Font strategy

Web currently uses:

- UI/body: Inter with system fallbacks
- Display: Georgia

Recommended Android equivalents:

- UI/body: **Inter** bundled as a variable font, or system sans during early MVP
- Display: **Source Serif 4** or **Newsreader**, bundled and licensed

Do not depend on Georgia being present on Android.

### Type scale

| Style | Web | Android Compose |
|---|---|---|
| Display Large | 48–88 px / 0.94 | 52 sp / 56 sp |
| Display Medium | 40 px / 44 px | 40 sp / 44 sp |
| Headline Large | 27–32 px | 30 sp / 36 sp |
| Headline Medium | 25 px | 24 sp / 30 sp |
| Title Large | 20 px | 20 sp / 26 sp |
| Title Medium | 16 px | 16 sp / 22 sp |
| Body Large | 19 px | 18 sp / 28 sp |
| Body Medium | 14–16 px | 16 sp / 24 sp |
| Body Small | 12–13 px | 14 sp / 20 sp |
| Label Medium | 12–13 px | 13 sp / 18 sp |
| Editorial Label | 9–12 px uppercase | 11 sp / 16 sp |

### Typography rules

- Use serif only for product headlines, result titles, and selected editorial
  accents.
- Use sans-serif for controls, navigation, forms, metadata, and body text.
- Avoid all-uppercase text longer than a short label.
- Android minimum body size is 14 sp; prefer 16 sp.
- Support system font scaling without clipping at 200%.

## 9. Spacing and layout

### Base spacing scale

Use a 4 dp grid:

```text
2, 4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 72, 96
```

Preferred semantic values:

| Token | Value |
|---|---:|
| `SpaceXs` | 4 dp |
| `SpaceSm` | 8 dp |
| `SpaceMd` | 12 dp |
| `SpaceLg` | 16 dp |
| `SpaceXl` | 24 dp |
| `Space2xl` | 32 dp |
| `Space3xl` | 48 dp |
| `Space4xl` | 64 dp |

### Android layout

- Compact horizontal screen padding: 16 dp
- Medium screen padding: 24 dp
- Expanded screen padding: 32 dp
- Maximum readable content width: 720 dp
- Form fields stack vertically on phones
- Use two panes only on expanded widths where both remain useful
- Respect navigation bars, display cutouts, and IME insets

### Web layout

- Maximum shell width: 1180 px
- Standard desktop gap: 24 px
- Collapse multi-column layouts below 820 px
- Use 20 px page gutters on tablet and 12 px on small mobile web

## 10. Shape and elevation

### Corner radius

| Token | Value | Usage |
|---|---:|---|
| `RadiusSm` | 7–8 dp | Tags and compact source links |
| `RadiusMd` | 10–12 dp | Buttons, inputs, thumbnails |
| `RadiusLg` | 13–16 dp | Result and plan cards |
| `RadiusXl` | 17–22 dp | Panels, sheets, major containers |
| `RadiusFull` | 999 dp | Status pills and circular controls |

### Elevation

The web shadow is `0 20px 60px rgba(35, 31, 22, 0.08)`.

Compose mapping:

- Flat/nested cards: 0 dp with outline
- Standard panel: 2 dp tonal elevation
- Raised action or floating card: 4 dp
- Modal bottom sheet: 8 dp

Prefer borders and tonal separation over strong shadows.

## 11. Iconography and imagery

- Use Material Symbols Rounded or one consistent outlined icon family.
- Default icon size: 24 dp
- Compact metadata icon: 16–18 dp
- Touch targets remain at least 48 × 48 dp.
- Thumbnails use 4:3 or 16:9 media crops, depending on source.
- If a thumbnail is unavailable, show the first title letter on
  `AccentContainer`.
- Decorative images use empty accessibility descriptions.
- Informative thumbnails use a concise source/title description.

## 12. Component system

### 12.1 App bar

Contains:

- Brand or current screen title
- Optional contextual action
- Account/avatar on Home

Service readiness belongs in developer settings. Production outages should
appear as contextual banners only when they affect the current action.

### 12.2 Buttons

**Primary button**

- `Primary` background, white label
- One per visual section
- Height: 48–52 dp
- Radius: 10–12 dp

**Secondary button**

- `Secondary` background, `Ink` label
- Used for memory retrieval or supportive action

**Tertiary button**

- Neutral container or text-only
- Used for refresh, cancel, edit, and low-priority actions

**Destructive button**

- Error label or error container
- Requires confirmation for irreversible deletion

States: default, pressed, focused, disabled, loading. Loading preserves button
width and replaces or accompanies the label with progress.

### 12.3 Text fields

- Persistent label above or Material label within the field
- Minimum height: 52 dp
- Multiline prompt minimum: 112 dp
- Helper/error text directly below
- Clear button for populated search fields
- URL input may detect multiple pasted lines
- Never use placeholder text as the only label

### 12.4 Memory card

Required content:

- Thumbnail or fallback
- Title
- One- or two-line summary
- Source platform
- Collection or category
- Ready/processing/needs-attention state when relevant

Optional actions:

- Open memory
- Open original source
- Add to collection
- Edit
- Delete

Do not show raw similarity percentages by default. Use relevance ordering or
labels such as “Best match.” Scores may remain in developer mode.

### 12.5 Collection card

Required content:

- Collection name
- Memory count
- Processing summary
- Recent thumbnail stack or topic preview
- Updated date

Tap opens collection details. Failed sources expose `Review issues`.

### 12.6 Processing row

States:

```text
Queued → Reading → Understanding → Ready
                         ↘ Needs attention
```

Show one state label, supporting message, and relevant action. A progress
indicator may be indeterminate because external processing time is variable.

### 12.7 Prompt composer

This is the primary Home action.

- Multiline input
- Submit/send action
- Suggested prompt chips when empty
- Optional mode selector: Answer, Plan, Compare, Recommend
- Optional collection filter under advanced controls

The system should infer a mode when possible instead of forcing a selection.

### 12.8 Plan header

Contains:

- Goal/title
- Short explanation
- Grounding label
- Completion count and progress
- Save/edit overflow actions

Grounding label options:

- `Based on 6 memories`
- `Partly based on your memories`
- `General suggestion`

### 12.9 Plan step

Contains:

- Completion control
- Step title
- Optional duration
- Reason, collapsed by default
- One or more cited source chips
- Edit/skip action

If a step claims to be grounded, it must cite at least one memory.

### 12.10 Source citation

A citation shows:

- Source title
- Platform
- Optional thumbnail
- Open-source icon

Opening a citation shows the saved memory first, with a secondary action to open
the external URL. This preserves context and user trust.

### 12.11 Feedback and banners

- Snackbar: short confirmation such as “Memory saved”
- Inline feedback: validation or processing result tied to a section
- Banner: offline, permission, or service-wide issue
- Dialog: destructive confirmation or required user decision only

## 13. Screen specifications

### 13.1 Home

**Goal:** Start from an outcome, not from storage management.

Content order:

1. Greeting and account
2. “What do you want to do?” prompt
3. Suggested actions
4. Continue active plan
5. Recently saved memories
6. Add knowledge action

Empty state includes example outcomes and a clear `Add your first source`
action.

### 13.2 Add knowledge

Presented as a modal bottom sheet or dedicated screen.

Options:

- Paste URL
- Add note
- Import playlist
- Open Android share instructions

The first MVP may implement Paste URL and share-target capture only.

### 13.3 Memories

Content:

- Search field
- Filter chips: All, Videos, Articles, Places, Products, Recipes
- Collection carousel or list
- Memory list
- Add action

Filters must be horizontally scrollable and retain selection during the
session.

### 13.4 Memory details

Content:

- Thumbnail and source
- Title and editable summary
- Topics/tags
- Useful actions learned from the source
- Collection membership
- Original source action
- Processing metadata under developer details

### 13.5 Result/answer

Content:

- Direct answer
- Grounding status
- Inline citations
- Retrieved memories
- Follow-up composer
- Save as plan where relevant

### 13.6 Plan details

Content:

- Plan header
- Interactive steps
- Source citations
- Edit/regenerate actions
- Completion feedback

Step completion is optimistic locally and synchronized with the backend.

### 13.7 Profile and settings

Content:

- Account
- Connected capture integrations
- Privacy and data controls
- AI provider disclosure
- Export/delete data
- Developer settings in debug builds only

Never expose private API keys in normal application UI or logs.

## 14. Required UX states

### Loading

- Prefer skeletons for memory and collection lists.
- Use inline progress for capture and planning.
- Keep previous results visible during refresh.
- Disable duplicate submission while a request is active.

### Empty

Every empty state explains:

1. What belongs here
2. Why it is useful
3. What the user can do next

### Success

- Confirm the outcome in plain language.
- Offer a relevant next action.
- Do not rely only on a transient snackbar for important state.

### Error

- Explain what failed without internal stack details.
- Preserve user input.
- Provide retry where safe.
- Isolate per-source errors so one failed URL does not hide successful memories.

### Offline

- Explain that new captures will sync later.
- Queue user-authorized saves locally.
- Display pending count.
- Do not imply a memory is fully processed until backend confirmation.

### Permission denied

- Explain why the permission is useful.
- Provide a settings action.
- Allow reduced functionality when possible.

### First-time user

- Use lightweight contextual onboarding, not a long carousel.
- Demonstrate one loop: save a source, inspect its memory, ask a question.

### Returning user

- Restore active plan and recent workspace.
- Prioritize unfinished work and recent memories.

## 15. Motion and feedback

- Standard transitions: 150–250 ms
- Page/sheet transitions: 250–350 ms
- Progress changes animate without blocking input
- Completed plan steps reduce emphasis and may use a subtle check animation
- Respect Android “Remove animations” and system duration scale
- Avoid looping decorative animation
- Haptic feedback may be used for successful save and plan-step completion

## 16. Accessibility

Required:

- WCAG 2.2 AA color contrast
- 48 × 48 dp minimum touch target
- Logical TalkBack traversal order
- Semantic heading hierarchy
- Content descriptions for meaningful icons
- State announcements for loading, ready, failed, and completed
- Visible focus indicators on keyboard-capable devices
- Dynamic type up to 200%
- No meaning conveyed by color alone
- Reduced-motion support
- Error text associated with its field

For completion controls, expose a checked/unchecked semantic state rather than
only changing the step number color.

## 17. Responsive and adaptive behavior

### Compact: below 600 dp

- Bottom navigation
- Single column
- Full-width cards
- Modal bottom sheets
- Sticky primary action where useful

### Medium: 600–839 dp

- Navigation rail may replace bottom navigation
- Two-column memory grids where readable
- List/detail layout for collections

### Expanded: 840 dp and above

- Navigation rail or permanent drawer
- Two-pane Memories and Plan experiences
- Maximum content width prevents overlong lines

Use Compose Material 3 adaptive window-size classes rather than device names.

## 18. Jetpack Compose implementation mapping

### Theme structure

Recommended package:

```text
designsystem/
  theme/
    Color.kt
    Type.kt
    Shape.kt
    Spacing.kt
    ImpulseTheme.kt
  component/
    ImpulseButton.kt
    ImpulseTextField.kt
    MemoryCard.kt
    CollectionCard.kt
    ProcessingRow.kt
    PromptComposer.kt
    PlanHeader.kt
    PlanStepCard.kt
    SourceCitation.kt
    EmptyState.kt
    StatusBanner.kt
```

### Token naming

Expose semantic tokens rather than raw web names:

```kotlin
@Immutable
data class ImpulseColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val brandPrimary: Color,
    val brandSecondary: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val outline: Color,
)
```

Use `MaterialTheme.colorScheme` for standard Material components and a small
`ImpulseTheme.colors` extension only where brand semantics are not represented.

### Component rules

- Components receive immutable UI models and callbacks.
- Screens own state; reusable visual components do not call repositories.
- Every component includes loading, disabled, error, and accessibility behavior
  where applicable.
- Add `@Preview` coverage for light, dark, large font, and narrow configurations.
- Use `LazyColumn`/`LazyGrid` for memory and collection lists.
- Use stable IDs for list keys.
- Use `collectAsStateWithLifecycle` for observable screen state.
- Navigation passes IDs, not complete mutable objects.

### Android share target

The Android app should declare a share intent for `text/plain`, validate the
shared URL, and open a capture preview. It must not collect browser history or
private application data.

## 19. Content guidelines

Voice is clear, warm, direct, and non-technical.

### Buttons

Use verb + outcome:

- `Save memory`
- `Create plan`
- `Compare products`
- `Retry source`
- `Open original`

Avoid vague labels such as `Submit`, `Continue`, or `Process`.

### Status examples

| Situation | Copy |
|---|---|
| Queued | “Waiting to be processed” |
| Reading | “Reading the source” |
| Understanding | “Creating a useful memory” |
| Ready | “Memory ready” |
| Failure | “We couldn’t read this source” |
| Weak retrieval | “Your memories don’t contain enough about this yet” |
| Offline | “Saved on this device. We’ll process it when you’re online.” |

### AI transparency

Use:

- “Based on 4 saved memories”
- “This step uses general guidance”
- “Impulse could not verify this from your saved sources”

Avoid absolute claims when source support is incomplete.

## 20. Design quality checklist

Before a screen or component is complete:

- [ ] One primary user outcome is clear
- [ ] One dominant action is visible
- [ ] Loading, empty, success, error, and offline states are designed
- [ ] Grounded output exposes citations
- [ ] General AI content is labeled
- [ ] Touch targets are at least 48 dp
- [ ] Text works at 200% scaling
- [ ] TalkBack semantics are defined
- [ ] Light and dark themes are reviewed
- [ ] Compact and expanded layouts are reviewed
- [ ] Existing design tokens and components are reused
- [ ] No internal identifiers or AI infrastructure terms leak into normal UI

## 21. MVP design delivery order

### Phase 1 — Product loop

1. Home prompt
2. Add/paste/share URL
3. Processing status
4. Memory card and details
5. Personalized answer/plan with citations

### Phase 2 — Organization

1. Memories screen
2. Collections
3. Filters and search
4. Saved plans

### Phase 3 — Retention and control

1. Plan persistence and completion
2. Memory editing and deletion
3. Feedback
4. Privacy/export controls

The current HTML page remains the design reference for brand character. The
Android application should implement the product hierarchy and interaction
rules in this document rather than reproducing the desktop two-column layout.
