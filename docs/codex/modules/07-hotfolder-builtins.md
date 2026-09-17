# Module 07 — HotFolder and Safe Built-in Tools

## Dependencies

Module 06 plus Gateway authorization.

## Objective

Provide immediately useful safe local tools and HotFolder.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- canonical path handling;
- traversal/symlink defenses;
- configurable HotFolder;
- safe built-in tool registry;
- list/read/write/append/mkdir/search/hash/move/copy/info/events as scoped;
- calculator/text/json/hash/system info;
- web search only after configured provider credential;
- delete absent/default BLOCK;
- every protected call validates authorization.


## Mandatory Tests


- traversal;
- symlink;
- extension/size limits;
- unauthorized path;
- concurrent writes;
- Gateway unavailable protected-mode failure;
- tool contract tests.


## Deployment / Release


- Quickstart/client packaging receives built-ins;
- no Helm direct requirement beyond server policy exposure.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
feat(client): add HotFolder and safe built-in tools
```
