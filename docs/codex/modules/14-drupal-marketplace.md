# Module 14 — Drupal Marketplace Website

## Dependencies

Module 12.

## Objective

Implement community UX consuming Marketplace API without owning package business data.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- Composer-managed Drupal;
- Marketplace API client module;
- browse/search/detail;
- publisher profile;
- publisher draft/version UI;
- presigned artifact upload UX;
- publication job status;
- moderation UI;
- reviews/community content;
- OAuth delegated user identity;
- no direct Marketplace DB mutation;
- no package execution;
- container/deployment.


## Mandatory Tests


- API error handling;
- scope/ownership;
- XSS/Markdown safety;
- upload flow;
- moderation;
- accessibility/browser E2E.


## Deployment / Release


- Drupal image if project-operated;
- Helm deployment/service/ingress/network/probes/resources;
- external Drupal DB/storage config.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
feat(marketplace): add Drupal community Marketplace
```
