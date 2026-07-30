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
- Historical execution cards render when trace data exists; live run events are also supported. The currently paired desktop’s older saved conversation payload does not include historical trace content, which is a data-availability constraint rather than a missing mobile component.

## Engineering verification

- `testDebugUnitTest`: passed.
- `lintDebug`: passed.
- `assembleDebug`: passed.
- Desktop remote attachment receiver: Python syntax compilation and temporary-file round-trip/cleanup checks passed.
- Attachment limits: maximum 5 files, 8 MB per file, and 8 MB total; failed sends clean up persisted temporary uploads.

## Final result

Passed. No remaining actionable visual issues were found for the requested mobile conversation, task detail, and shared composer states.
