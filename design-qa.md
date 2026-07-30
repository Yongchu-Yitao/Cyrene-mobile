# Cyrene Mobile 对话与任务页设计 QA

## Source of truth

- Desktop reference: `/var/folders/zm/qgh_rgw903j0b9t01yg2l0mc0000gn/T/codex-clipboard-7c8190d6-432c-40bc-952f-ddc3adc0869b.png` (1214 × 918)
- Mobile implementation: `qa/implementation-final.png` (1080 × 2400, Android API 35)
- Combined visual review: `qa/final-comparison.png`
- Matching-content review: `qa/chat-comparison.png`
- Sidebar implementation: `qa/sidebar-open-final.png`
- Sidebar header comparison: `qa/sidebar-header-comparison.png`
- Recent sessions implementation: `qa/sidebar-recent-sessions.png`
- Recent sessions comparison: `qa/sidebar-recent-sessions-comparison.png`
- Merged chat list and FAB: `qa/chat-list-fab.png`
- Chat list/FAB comparison: `qa/chat-list-fab-comparison.png`
- Task reference: `/var/folders/zm/qgh_rgw903j0b9t01yg2l0mc0000gn/T/codex-clipboard-7a4bc941-be76-4fe6-b9ab-46f720a78665.png`
- Task detail implementation: `qa/task-detail-chat-composer.png`
- Task list implementation: `qa/task-list-final.png`
- Dynamic conversation title implementation: `qa/chat-detail-title-final.png`
- Dark-mode conversation implementation: `qa/dark-chat-user-bubble.png`

The reference and implementation intentionally use different viewport classes. QA therefore compares the desktop information hierarchy, message styling, typography, Markdown semantics, and composer behavior at the native mobile viewport rather than stretching desktop pixel dimensions onto the phone.

## Visible findings and fixes

| Priority | Finding | Resolution |
| --- | --- | --- |
| P1 | Assistant messages were split into card bubbles and exposed raw Markdown syntax. | Replaced assistant cards with desktop-style continuous content and rendered headings, emphasis, lists, links, and code through Markwon. |
| P1 | User and assistant messages did not preserve the desktop role hierarchy. | Added the right-aligned purple user bubble, timestamp/action row, borderless assistant body, and copy/time footer. |
| P2 | Composer was a fixed bottom panel separated from the transcript. | Composer now floats above the conversation in the same layout layer; transcript continues behind it with safe bottom padding. |
| P2 | Composer used elevation/shadow and a page divider. | Removed elevation and the transcript/composer divider. The only remaining stroke is the composer’s own subtle outline plus its internal toolbar separator. |
| P2 | Composer controls did not match desktop behavior. | Added desktop-style multiline input, default/Plan mode menu, send/stop states, and attachment picker. |
| P2 | Product disclaimer remained below the composer. | Removed “Cyrene 是 AI，可能会犯错。请核实回复。” |
| P2 | Attachment action was decorative or absent. | Connected the paperclip to Android’s multi-document picker, shows removable selected-file chips, and sends bounded file payloads to the paired desktop. |
| P1 | Bottom tabs consumed conversation height and did not expose recent chats. | Replaced the bottom bar with a modal left navigation menu. Existing destinations appear first, followed by up to eight recent chats from the selected project. |
| P2 | The first sidebar header pass repeated “Cyrene” on two lines. | Removed the secondary line so the header contains one “Cyrene”, matching the supplied crop. |
| P1 | The sidebar still exposed the project destination and its lower section only supported chats. | Removed the project destination from the drawer and replaced “最近对话” with “最近会话”, combining task and chat records by update/create time. |
| P1 | The chat list repeated app, project, and page titles across three stacked header areas. | Collapsed the screen to one top-app-bar title, “对话”; removed the project label and duplicate content heading. |
| P1 | New-chat creation required a manually entered title in a full-width form row. | Removed the title field and moved creation to a circular bottom-right FAB. New chats are created with an empty title, become “新对话”, then use the desktop rule to derive the title from the first message (first 24 characters). |
| P1 | Task dispatch used a separate outlined “派发说明” field and large dispatch button. | Replaced both with the same floating composer used by conversations, including multiline input, attachment action, send state, and running stop/pause behavior. |
| P1 | Task attachments were not accepted by the desktop dispatch command. | Added bounded Base64 attachment transfer to `tasks.dispatch`, desktop temporary-file persistence, and cleanup on dispatch failures. |
| P2 | The task list repeated “任务” below the top app bar. | Removed the duplicate content heading so the screen has a single title. |
| P1 | An opened conversation repeated its title and “Cyrene” in a second content header. | Removed that content header and promoted the conversation’s real title into the top app bar, with “新对话” as the empty-title fallback. |
| P1 | User bubbles and execution/tool cards retained hard-coded light backgrounds in dark mode. | Replaced the fixed colors with explicit light/dark secondary and tertiary theme roles. User messages now use a dark purple container with matching text/border; execution cards use a dark green container with theme-aware status, progress, text, and border colors. |

## Interaction checks

- Conversation list → conversation: passed.
- Markdown headings and unordered lists: passed in the emulator screenshot.
- Composer remains reachable while transcript scrolls beneath it: passed.
- Default/Plan mode menu opens and changes state: passed (`qa/implementation-composer-mode-menu.png`).
- Attachment button opens the system multi-file picker: passed (`qa/implementation-attachment-picker.png`).
- Hamburger opens the left menu and the bottom tab bar is absent: passed (`qa/sidebar-open-final.png`).
- Sidebar destination buttons change sections and close the menu: passed.
- Selecting a recent chat switches to the conversation and closes the menu: passed (`qa/sidebar-recent-chat-opened.png`).
- Project is absent from drawer destinations: passed (`qa/sidebar-recent-sessions.png`).
- Recent task/chat records share one timestamp-sorted list and use distinct icons/summaries: passed by implementation review; the currently selected project supplies chats but no task records.
- Chat list displays only the merged “对话” page title: passed (`qa/chat-list-fab.png`).
- Circular new-chat FAB is visible at the bottom right without covering list content: passed.
- Desktop chat-title flow (`新对话` → first-message prefix) was verified against the shared desktop implementation.
- Empty send state and enabled/stop state wiring: passed by implementation review and build tests.
- Task detail uses the shared border-only floating composer with no page divider or shadow: passed (`qa/task-detail-chat-composer.png`).
- Task composer attachment selection and dispatch payload are connected end-to-end: passed by implementation and protocol review.
- Task list has one “任务” heading: passed (`qa/task-list-final.png`).
- Opening a conversation changes the app-bar title to that conversation and no duplicate title/status row remains: passed (`qa/chat-detail-title-final.png`).
- User-message bubbles preserve readable purple hierarchy in dark mode without light-theme blocks: passed (`qa/dark-chat-user-bubble.png`).
- Execution/tool cards consume only theme-aware tertiary roles; historical trace data is unavailable in the currently paired saved conversations, so the dark card state passed implementation, token, and build review rather than a historical-data screenshot.
- Historical execution cards render when trace data exists; live run events are also supported. The currently paired desktop’s older saved conversation payload does not include historical trace content, which is a data-availability constraint rather than a missing mobile component.

## Engineering verification

- `testDebugUnitTest`: passed.
- `lintDebug`: passed.
- `assembleDebug`: passed.
- Desktop remote attachment receiver: Python syntax compilation and temporary-file round-trip/cleanup checks passed.
- Attachment limits: maximum 5 files, 8 MB per file, and 8 MB total; failed sends clean up persisted temporary uploads.

## Final result

Passed. No remaining actionable visual issues were found for the requested mobile conversation, task detail, and shared composer states.

---

# 设置悬浮入口设计 QA

## Source of truth

- Source visual truth: `/var/folders/zm/qgh_rgw903j0b9t01yg2l0mc0000gn/T/codex-clipboard-5d309842-71a1-4f0c-9734-529e4e43a179.png`
- Source pixels: 548 × 1536; normalized to 840 × 2400 for drawer-region comparison.
- Implementation screenshot: `qa/settings-fab-drawer.png`
- Implementation pixels: 1080 × 2400 at Android API 35; drawer crop is 840 × 2400.
- CSS size and density: native Android Compose viewport, 1080 × 2400 physical pixels at emulator density; no browser CSS viewport applies.
- State: dark theme, paired device, conversation selected, navigation drawer open.
- Full-view comparison evidence: `qa/settings-fab-comparison.png`
- Focused interaction evidence: `qa/settings-fab-opened-settings.png`

## Findings

- No actionable P0/P1/P2 mismatch remains. The original upper navigation row for “设置” is absent, while the same outlined gear icon is now presented as a circular bottom-right floating action button.
- Typography, drawer spacing, purple selection token, dark surface colors, icon family, and recent-session copy remain consistent with the supplied drawer.
- The session list reserves bottom space so the floating button does not hide the final reachable item.
- The supplied visual did not include the requested post-change state, so the comparison treats the existing drawer styling as the visual truth and the user’s placement instruction as the delta.

## Interaction checks

- Hamburger opens the navigation drawer: passed.
- Settings FAB remains fixed at the drawer’s bottom-right while the session list scrolls: passed.
- Settings FAB exposes the localized “设置” accessibility description: passed.
- Tapping the FAB closes the drawer and opens the existing settings screen: passed.
- Android runtime log checked after both interactions; no fatal error was recorded.

## Comparison history

- Initial implementation comparison: no actionable P0/P1/P2 findings; no visual-fix iteration was required.

## Engineering verification

- `testDebugUnitTest`: passed.
- `assembleDebug`: passed.
- `lintDebug`: passed.

final result: passed

---

# 侧栏品牌入口 QA

## Source of truth

- Desktop header reference: `/var/folders/zm/qgh_rgw903j0b9t01yg2l0mc0000gn/T/codex-clipboard-c684a272-6e27-4047-928b-26866a16d0f4.png`.
- Desktop icon source: `/Users/syw/Documents/playground/Cyrene/build/icon.png`.
- Mobile drawer implementation: `qa/sidebar-brand-desktop-icon.png`.
- Focused side-by-side review: `qa/sidebar-brand-header-comparison.png`.
- Destination after tapping the brand row: `qa/sidebar-brand-about-click.png`.
- Interaction viewport: Android API 35, 1080 × 2400, dark theme.

## Findings

- No actionable P0/P1/P2 mismatch remains for the requested brand header.
- The mobile header uses `ic_launcher_full.png`, which is byte-for-byte identical to the desktop `build/icon.png` (SHA-256 `aef7c3d9e89d0cd368fcd1f7c60d85ba802512bbb224a5148d5f819d13d1ac95`).
- The icon and “Cyrene” label are vertically centered and retain the desktop brand order, scale relationship, and spacing.

## Interaction checks

- The complete icon/title row is one touch target: passed.
- Tapping the row closes the drawer and opens Settings → About and updates directly: passed in the emulator.
- The destination exposes the localized “关于” title and update controls: passed by screenshot and UI hierarchy inspection.
- The touch target has the localized “关于与更新” semantic description: passed by implementation review.

## Engineering verification

- `assembleDebug`: passed.
- `testDebugUnitTest`: passed.
- `lintDebug`: passed.
- `git diff --check`: passed.

final result: passed

---

# 左下角新建对话胶囊按钮 QA

## Source of truth

- Source visual truth: `/var/folders/zm/qgh_rgw903j0b9t01yg2l0mc0000gn/T/codex-clipboard-2db398fa-852c-407e-8a5c-c49309c326a1.png`
- Source pixels: 580 × 422; normalized to 720 × 630 for focused comparison.
- Implementation screenshot: `qa/new-chat-pill-themed.png`
- Implementation pixels: 1080 × 2400 at Android API 35.
- Focused implementation crop: `qa/new-chat-pill-themed-focus.png`, 720 × 630.
- Full focused comparison evidence: `qa/new-chat-pill-themed-comparison.png`.
- State: dark theme, paired-device cache loaded, navigation drawer open.
- CSS size and density: native Android Compose viewport; no browser CSS viewport applies.

## Findings

- No actionable P0/P1/P2 mismatch remains for the requested control.
- The reference’s extended pill silhouette, leading edit affordance, and “聊天” label are preserved.
- Following the requested refinement, the bright cyan reference color was intentionally replaced with the app’s existing primary-container token, matching the adjacent settings FAB and selected drawer styling.
- Typography uses the existing Material label scale and semibold weight; icon size returns to the app’s standard 24 dp rhythm.
- The button retains the drawer’s 20 dp edge spacing and 56 dp control height, and the session list still reserves enough bottom padding.
- Image/asset fidelity: the control uses the project’s Material outlined icon library; there are no raster placeholders, custom SVGs, or simulated icon drawings.
- Copy/content: Simplified Chinese shows “聊天”; the accessibility description remains “创建新对话”.

## Interaction checks

- The extended FAB has an independent clickable bounds rectangle and does not overlap the settings FAB: passed by rendered semantics inspection.
- On click, it switches to the conversation destination, clears the previous selection, closes the drawer, and calls the existing `createChat()` path: passed by implementation review and build.
- Live remote creation was not repeated because the paired desktop endpoint was offline during this visual QA; the same existing `createChat()` command path remains unchanged.

## Comparison history

- First pass used the reference’s saturated cyan and 20 sp bold label; user identified it as too visually abrupt.
- Final pass adopted the existing theme container color, 24 dp icon, and Material label scale. The revised evidence is `qa/new-chat-pill-themed.png`.

## Engineering verification

- `testDebugUnitTest`: passed.
- `assembleDebug`: passed.
- `lintDebug`: passed.
- `git diff --check`: passed.

final result: passed

---

# 桌面端右侧栏移动适配 QA

## Source of truth

- Desktop overview reference:
  `/var/folders/zm/qgh_rgw903j0b9t01yg2l0mc0000gn/T/codex-clipboard-4c29d6f7-270d-4f62-b08c-7ff26dc7be6b.png`.
- Desktop context reference:
  `/var/folders/zm/qgh_rgw903j0b9t01yg2l0mc0000gn/T/codex-clipboard-5e5783ec-3295-4b8d-8795-e8b6f193dee0.png`.
- Final mobile overview: `qa/final-overview-verified-color.png`.
- Final mobile context: `qa/final-context-verified-color.png`.
- Interaction viewport: Android API 35, 1080 × 2400, dark theme.

## Visible findings and fixes

| Priority | Finding | Resolution |
| --- | --- | --- |
| P1 | The initial right panel snapped only after a swipe completed. | Added a reveal-progress state so the panel translation and scrim opacity follow the finger continuously; release thresholds settle it open or closed. |
| P1 | Closing the right panel could hand the same gesture to the left navigation drawer. | Suppressed left-drawer gestures for the duration of a right-panel drag and a short release window. |
| P2 | The first mobile pass used a light surface-variant fill unlike the desktop sidebar. | Changed the panel to the page background token, cards to the surface token, removed inherited tonal elevation, and retained a subtle outline. |
| P1 | Sidebar tabs were hard-coded or could expose empty sections. | Built tabs from available data. Overview and Context are permanent; Plan, Artifacts, and Branches appear only when their protocol data exists. |
| P1 | Chat lineage was not available to mobile. | Added parent/fork metadata to remote chat summaries and a selectable Branches view for parent, child, and sibling chats. |
| P1 | Overview and Context initially showed generic mobile metadata rather than the desktop information model. | Added desktop-compatible context metrics, block groups, Agent inbox, used tool packages, run summary, chat metadata, and per-role token data to the remote protocol and mobile panels. |
| P2 | Text and card colors were too small and low-contrast in dark mode. | Increased the sidebar type scale and adopted the desktop navy panel (`#141F31`), raised card (`#202D40`), bright text, blue input, purple output, and green total tokens. |
| P2 | Context occupancy rendered a usage bar and a second split bar. | Kept one occupancy progress bar with the compression-threshold marker; role composition is presented as the legend below it. |
| P1 | Desktop-only tabs were omitted even when matching protocol data existed. | Added Subagents, Changes, Viewer, and Map panels; each appears only when its backing data is present. |

## Interaction checks

- Swipe left inside an opened chat/task to reveal the right sidebar: passed.
- Panel position and scrim opacity update during the drag: passed by slow-drag capture and implementation review.
- Release above the reveal threshold settles open; release below it returns closed: passed.
- Swipe right closes the panel without opening the left drawer or changing the active destination: passed (`qa/right-sidebar-closed-by-swipe.png`).
- Scrim tap and close button dismiss the panel: passed.
- Overview and Context tabs remain available: passed.
- Plan appears only for an active plan; Artifacts appears only when attachments/artifacts exist: passed by state-derivation review.
- Branches appears only when related chat lineage exists; selecting a related branch loads that chat: passed by protocol and implementation review.
- Subagents, Changes, Viewer, and Map tabs appear only when equivalent mobile protocol data exists: passed by state-derivation, protocol, and implementation review.
- Overview uses the desktop information hierarchy and exactly one context occupancy bar: passed (`qa/final-overview-verified-color.png`).
- Context exposes system prefix, temporary injection, per-role messages, Agent inbox, used tool packages, and chat statistics: passed (`qa/final-context-verified-color.png`).
- Final dark palette keeps all card text and semantic colors readable: passed.

## Engineering verification

- Kotlin debug compilation: passed.
- Desktop remote-command module Python compilation: passed.
- Full Android unit, lint, and APK verification is recorded in the final engineering check.

final result: passed

---

# 最近会话长按菜单 QA

## Source of truth

- Requested drawer state:
  `/var/folders/zm/qgh_rgw903j0b9t01yg2l0mc0000gn/T/codex-clipboard-3ac8d80e-0879-4748-a746-26a21f623256.png`.
- Long-press menu: `qa/longpress-menu.png`.
- Rename dialog: `qa/longpress-rename-dialog.png`.
- Delete confirmation: `qa/longpress-delete-dialog.png`.
- Post-delete drawer: `qa/longpress-delete-complete.png`.
- Interaction viewport: Android API 35, dark theme, live paired desktop.

## Visible findings and fixes

| Priority | Finding | Resolution |
| --- | --- | --- |
| P1 | Recent and all-conversation rows only supported tap-to-open. | Replaced chat rows with a combined click/long-click target while keeping task rows non-destructive. |
| P1 | There was no mobile path to rename a chat. | Added a localized long-press menu and a single-line 60-character rename dialog backed by `chats.update`. |
| P1 | There was no mobile path to delete a chat safely. | Added a localized destructive action, explicit permanent-deletion confirmation, and `chats.delete` handling. |
| P1 | Desktop remote commands could mutate a chat outside the paired project scope. | Both lifecycle commands now resolve and validate the chat against the authorized project before mutation. |

## Interaction checks

- Normal tap still opens the selected conversation: passed.
- Long press opens a menu containing “重命名” and “删除”: passed (`qa/longpress-menu.png`).
- Rename dialog is prefilled with the current title and rejects a blank value: passed.
- Confirming rename updates the server, refreshes the drawer, and updates the active title: passed.
- Delete requires a second confirmation explaining that the action is permanent: passed (`qa/longpress-delete-dialog.png`).
- Confirming deletion removes the conversation and selects the next available conversation or empty state: passed (`qa/longpress-delete-complete.png`).
- Task rows do not expose chat rename/delete actions: passed by implementation review.
- Cross-project update/delete requests are rejected by the desktop protocol: passed by protocol lifecycle test.

## Engineering verification

- Android Kotlin compilation, unit tests, and Debug APK assembly: passed.
- Live API 35 emulator flow (create → long press → rename → long press → delete): passed.
- Desktop remote chat lifecycle test, including cross-project rejection: passed.

final result: passed
