# Aura Flow Security Specification (Zero-Trust ABAC)

## 1. Data Invariants
1. **Strict Tenant Isolation**: Every document under `/users/{userId}/{collection}/{docId}` must only be readable, listable, creatable, updatable, or deletable by an authenticated user whose `request.auth.uid == userId` AND whose document `userId` field equals `request.auth.uid`.
2. **Schema & Key Allowlisting**: Every document write (`create` and `update`) must pass a dedicated `isValid[Entity](incoming())` helper enforcing `keys().hasAll(...)` and `keys().hasOnly(...)` with strict string `.size()` bounds, integer range checks, enum allowlists, and timestamp upper bounds (`<= request.time`).
3. **Immutable Metadata**: `userId` and `createdAt` are immutable across all updates (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).
4. **Path ID Hardening**: Document IDs on single-document operations (`get`, `create`, `update`, `delete`) must match `^[a-zA-Z0-9_\-]+$` with length `1..128`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read/List**: Calling `get` or `list` on `/users/alice_123/inbox` with `request.auth == null`.
2. **Cross-User Read (PII / Inbox Leak)**: Authenticated user `bob_456` reading `/users/alice_123/inbox/msg_1`.
3. **Cross-User List Scraping**: Authenticated user `bob_456` listing `/users/alice_123/briefings`.
4. **Identity Spoofing on Create**: User `alice_123` creating `/users/alice_123/inbox/msg_1` with `userId: "bob_456"`.
5. **Shadow / Ghost Field Injection on Create**: Creating an `english_tasks` document with an extra field `"isAdmin": true`.
6. **Shadow / Ghost Field Injection on Update**: Updating an `inbox` document with an extra field `"verified": true` not in `affectedKeys().hasOnly(...)`.
7. **Immortal Field Mutation**: Updating `createdAt` or `userId` on an existing `briefings` document.
8. **Value Poisoning (Oversized String DoW)**: Writing a 5,000-character string to `sender` or `subject` in `inbox`.
9. **Enum Bypass**: Writing `channel: "WHATSAPP"` when only `["EMAIL", "MESSAGE"]` are allowed.
10. **Future Timestamp Spoofing**: Writing `createdAt` set to a timestamp in the future (`> request.time`).
11. **Out-of-Bounds Numeric Value**: Writing `masteryScore: 150` on `english_tasks` or `dailyGoalMinutes: 500` on `settings`.
12. **Malformed Path ID Injection**: Creating a document with special characters or >128 chars in `{docId}`.

## 3. Red Team Audit & Conflict Report
- **Inbox (`/users/{userId}/inbox/{itemId}`)**: Verified against Identity Spoofing, Ghost Fields, Value Poisoning, and Cross-User reads. `isValidInboxItem(incoming())` wraps both `create` and `update`.
- **Briefings (`/users/{userId}/briefings/{briefingId}`)**: Verified against Identity Spoofing, Ghost Fields, Value Poisoning, and Cross-User reads. `isValidNewsBriefing(incoming())` wraps both `create` and `update`.
- **English Tasks (`/users/{userId}/english_tasks/{taskId}`)**: Verified against Identity Spoofing, Ghost Fields, Value Poisoning, and Cross-User reads. `isValidEnglishTask(incoming())` wraps both `create` and `update`.
- **Settings (`/users/{userId}/settings/{settingId}`)**: Verified against Identity Spoofing, Ghost Fields, Value Poisoning, and Cross-User reads. `isValidAssistantSettings(incoming())` wraps both `create` and `update`.
