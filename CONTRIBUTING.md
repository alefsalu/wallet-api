# Contributing to Wallet API

Thank you for your interest in contributing to the `wallet-api` project!

## Commit Conventions

We follow [Conventional Commits](https://www.conventionalcommits.org/):
- `feat:` New features
- `fix:` Bug fixes
- `test:` Adding or updating tests
- `chore:` Tooling, dependency updates, and maintenance
- `docs:` Documentation improvements
- `ci:` CI/CD pipeline changes
- `refactor:` Code improvements without altering external behavior

## Development Workflow

1. Fork and create a topic branch (`feat/amazing-feature`).
2. Verify code quality and architecture boundaries:
   ```bash
   make lint
   make test-unit
   make test-integration
   ```
3. Submit a Pull Request targeting `main`. Ensure all CI checks pass.
