# Git & Branching Strategy

## 1. Branching Model (Trunk-Based / Scaled GitFlow)

We adhere to a Trunk-Based Development model with short-lived feature branches to ensure continuous integration, fast feedback cycles, and reliable deployments.

```
 main (Protected / Production Ready)
   │
   ├── feature/payment-state-machine ───┐ (PR + CI + Peer Review)
   │                                    │
   ▼                                    ▼
 main (Automated Tagging & Release)
```

### 1.1 Branch Naming Conventions
- `main`: Production-ready, always buildable and deployable code.
- `feature/<step-number>-<feature-name>`: New capabilities (e.g., `feature/04-parent-maven-pom`, `feature/14-payment-state-machine`).
- `bugfix/<issue-id>-<description>`: Fixes for non-blocking issues.
- `hotfix/<description>`: Urgent production fixes branching off `main`.
- `chore/<description>`: Maintenance, dependency updates, CI/CD pipeline tweaks.

---

## 2. Commit Message Guidelines (Conventional Commits)

All commit messages must adhere to the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) standard:

```
<type>(<scope>): <short summary>

[optional body explaining WHY the change was made]

[optional footer(s) such as Closes #123]
```

### Allowed Types
- `feat`: A new feature or domain capability
- `fix`: A bug fix
- `docs`: Documentation changes only
- `style`: Code style/formatting changes that do not affect logic
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `perf`: Performance optimizations
- `test`: Adding missing tests or correcting existing tests
- `chore`: Build process, dependency updates, tooling configuration
- `ci`: CI/CD configuration files and scripts

### Examples
- `feat(payment): implement payment state machine with optimistic locking`
- `docs(arch): define service boundaries and double-entry invariants`
- `test(ledger): add double-entry balance verification test`

---

## 3. Pull Request & Quality Gates

Every Pull Request must satisfy the following checklist before merge:
1. **Compilation**: `mvn clean verify` passes with zero errors.
2. **Tests**: All unit and Testcontainers integration tests pass.
3. **No Secrets**: No API keys, credentials, or private certificates committed.
4. **Code Quality**: Clean code principles, meaningful package boundaries, constructor injection.
5. **Documentation**: Updates to relevant documentation in `docs/` and progress tracking.
