---
name: self-review
description: Comprehensive self-review of git commits against CLAUDE.md project guidelines. Use when reviewing code changes, validating commits before push, or ensuring adherence to project architecture, conventions, tests, and documentation. Triggers on requests like "review my commit", "check my changes", "self-review", "validate against guidelines", or "code review".
---

# Self-Review Skill

Perform automated code review of commits against CLAUDE.md guidelines.

## Arguments

| Argument | Description | Examples |
|----------|-------------|----------|
| `<commit>` | Specific commit hash or reference | `HEAD`, `abc123`, `HEAD~3` |
| `*` | All commits on current branch not on main | Reviews entire feature branch |
| (none) | Defaults to `HEAD` | Reviews latest commit |

## Execution Flow

1. **Parse arguments** and determine commit range
2. **Load guidelines** from CLAUDE.md
3. **Gather commit data** (diff, message, changed files)
4. **Run build** and capture results
5. **Analyze code** for violations and anti-patterns
6. **Check test coverage** for new/modified code
7. **Verify documentation** (README, docs/, Javadoc)
8. **Validate API specs** (OpenAPI, Postman)
9. **Generate report** to `self-review.json`
10. **Address high-severity issues** interactively
11. **Present summary** with actionable next steps

---

## Step 0: Parse Commit Argument

Determine the commit range to review based on the argument provided.

```bash
# Usage: self-review [<commit>|*]
COMMIT_ARG="${1:-HEAD}"

if [[ "$COMMIT_ARG" == "*" ]]; then
  # All commits on branch not on main
  # Find the merge base with main
  MERGE_BASE=$(git merge-base main HEAD)
  COMMIT_RANGE="${MERGE_BASE}..HEAD"
  DIFF_BASE="$MERGE_BASE"
  
  echo "Reviewing all commits since divergence from main:"
  git log --oneline "$COMMIT_RANGE"
  COMMIT_COUNT=$(git rev-list --count "$COMMIT_RANGE")
  echo "Total commits to review: $COMMIT_COUNT"
  
  # For multi-commit review, we analyze cumulative changes
  REVIEW_MODE="branch"
else
  # Single commit review
  COMMIT_RANGE="$COMMIT_ARG"
  DIFF_BASE="${COMMIT_ARG}~1"
  REVIEW_MODE="single"
  
  echo "Reviewing single commit: $COMMIT_ARG"
  git log -1 --oneline "$COMMIT_ARG"
fi

# Store for later steps
echo "$REVIEW_MODE" > /tmp/review-mode.txt
echo "$DIFF_BASE" > /tmp/diff-base.txt
echo "$COMMIT_RANGE" > /tmp/commit-range.txt
```

### Branch Review Mode (`*`)

When reviewing all branch commits:
- Analyzes cumulative diff (all changes since diverging from main)
- Validates each commit message individually
- Produces single report covering all changes
- Useful before merging feature branch to main

### Single Commit Mode (default)

When reviewing a specific commit:
- Analyzes only that commit's changes
- Validates that commit's message
- Standard review workflow

---

## Step 1: Gather Commit Information

```bash
REVIEW_MODE=$(cat /tmp/review-mode.txt)
DIFF_BASE=$(cat /tmp/diff-base.txt)
COMMIT_RANGE=$(cat /tmp/commit-range.txt)

if [[ "$REVIEW_MODE" == "branch" ]]; then
  # Branch mode: cumulative changes
  echo "=== Branch Review: Changes since main ==="
  
  # All commit messages for validation
  git log --format="=== %H ===%n%s%n%b%n" "$COMMIT_RANGE" > /tmp/commit-messages.txt
  
  # Cumulative diff
  git diff "$DIFF_BASE" --unified=5 > /tmp/full-diff.txt
  
  # Changed files (cumulative)
  git diff "$DIFF_BASE" --name-only > /tmp/changed-files.txt
  git diff "$DIFF_BASE" --stat
  
else
  # Single commit mode
  echo "=== Single Commit Review ==="
  
  # Commit message
  git log -1 --format="Hash: %H%nSubject: %s%n%nBody:%n%b" "$COMMIT_RANGE" > /tmp/commit-messages.txt
  cat /tmp/commit-messages.txt
  
  # Diff
  git diff "$DIFF_BASE" --unified=5 > /tmp/full-diff.txt
  
  # Changed files
  git diff "$DIFF_BASE" --name-only > /tmp/changed-files.txt
  git show --stat "$COMMIT_RANGE"
fi

# Categorize changes
cat /tmp/changed-files.txt | grep -E "^.+\.java$" > /tmp/changed-java.txt || true
cat /tmp/changed-files.txt | grep -E "Test\.java$" > /tmp/changed-tests.txt || true
cat /tmp/changed-files.txt | grep -E "src/main/java" | grep -v "Test\.java$" > /tmp/changed-production.txt || true

echo ""
echo "Files changed: $(wc -l < /tmp/changed-files.txt)"
echo "Java files: $(wc -l < /tmp/changed-java.txt)"
echo "Test files: $(wc -l < /tmp/changed-tests.txt)"
echo "Production files: $(wc -l < /tmp/changed-production.txt)"
```

---

## Step 2: Validate Commit Message(s)

Format: `type(scope): subject`

### Checklist (per commit)
- [ ] **Type valid:** `feat|fix|docs|style|refactor|test|chore`
- [ ] **Scope present:** Module or component name (e.g., `container`, `match`, `ecs`)
- [ ] **Subject format:** Imperative mood, lowercase start, no trailing period
- [ ] **Subject length:** ≤ 72 characters
- [ ] **Body content:** Explains "what" and "why" (if present)

### Validation Script
```bash
REVIEW_MODE=$(cat /tmp/review-mode.txt)
COMMIT_RANGE=$(cat /tmp/commit-range.txt)

validate_commit_message() {
  local hash="$1"
  local subject="$2"
  local issues=""
  
  # Check type
  if ! echo "$subject" | grep -qE "^(feat|fix|docs|style|refactor|test|chore)\("; then
    issues="${issues}Invalid or missing type; "
  fi
  
  # Check scope
  if ! echo "$subject" | grep -qE "^[a-z]+\([a-z-]+\):"; then
    issues="${issues}Invalid or missing scope; "
  fi
  
  # Check subject format (lowercase after colon, no period)
  if echo "$subject" | grep -qE "\):\s*[A-Z]"; then
    issues="${issues}Subject should start lowercase; "
  fi
  if echo "$subject" | grep -qE "\.$"; then
    issues="${issues}Subject should not end with period; "
  fi
  
  # Check length
  if [[ ${#subject} -gt 72 ]]; then
    issues="${issues}Subject exceeds 72 chars; "
  fi
  
  if [[ -n "$issues" ]]; then
    echo "❌ $hash: $issues"
    echo "   Subject: $subject"
  else
    echo "✓ $hash: Valid"
  fi
}

if [[ "$REVIEW_MODE" == "branch" ]]; then
  echo "=== Validating All Commit Messages ==="
  git log --format="%H %s" "$COMMIT_RANGE" | while read hash subject; do
    validate_commit_message "$hash" "$subject"
  done
else
  echo "=== Validating Commit Message ==="
  read hash subject <<< $(git log -1 --format="%H %s" "$COMMIT_RANGE")
  validate_commit_message "$hash" "$subject"
fi
```

### Examples
```
❌ "Fixed the bug"              → fix(container): resolve null pointer in lifecycle start
❌ "feat: Add new feature."     → feat(match): add spectator mode support
❌ "updated stuff"              → refactor(ecs): extract component store interface
❌ "FEAT(Match): Added thing"   → feat(match): add spectator join endpoint
```

**Severity:** MEDIUM - Commit hygiene

---

## Step 3: Run Build Verification

```bash
./build.sh all 2>&1 | tee /tmp/build-output.log
BUILD_EXIT=$?
echo "Build exit code: $BUILD_EXIT"

# Extract key metrics
grep -c "ERROR" /tmp/build-output.log
grep -c "WARNING" /tmp/build-output.log
grep -E "Tests run:.+Failures: [1-9]" /tmp/build-output.log
```

### Capture
- **Exit code:** 0 = pass, non-zero = fail
- **Compilation errors:** Lines containing `ERROR`
- **Test failures:** `FAILURE|FAILED` patterns
- **Warning count:** For information

### On Failure
Stop the review. Present build errors and offer to diagnose/fix before continuing.

**Severity:** CRITICAL - Build must pass

---

## Step 4: Layer Violation Detection

Verify clean architecture boundaries in changed files.

### Import Rules

| Source Layer | Prohibited Imports |
|--------------|-------------------|
| `engine-core/` | `io.quarkus.*`, `jakarta.*` (except `jakarta.validation.*`), `com.mongodb.*`, `engine-internal.*`, `webservice.*` |
| `engine-internal/` | `webservice.*` |

### Detection Commands
```bash
DIFF_BASE=$(cat /tmp/diff-base.txt)

# Framework leakage in engine-core
for f in $(cat /tmp/changed-files.txt | grep "engine-core/.*\.java$"); do
  echo "Checking: $f"
  grep -Hn "import io\.quarkus" "$f" && echo "  ❌ Quarkus import in engine-core"
  grep -Hn "import jakarta\." "$f" | grep -v "jakarta.validation" && echo "  ❌ Jakarta import in engine-core"
  grep -Hn "import com\.mongodb" "$f" && echo "  ❌ MongoDB import in engine-core"
done

# Upward dependency in engine-internal
for f in $(cat /tmp/changed-files.txt | grep "engine-internal/.*\.java$"); do
  echo "Checking: $f"
  grep -Hn "import.*webservice" "$f" && echo "  ❌ Webservice import in engine-internal"
done
```

**Severity:** HIGH - Architecture violation

---

## Step 5: Anti-Pattern Detection

Scan changed Java files for prohibited patterns.

### HIGH Severity (Must Fix)

| Pattern | Detection | Required Action |
|---------|-----------|-----------------|
| `@Deprecated` | `grep -n "@Deprecated"` | Remove; apply full migration instead |
| Framework annotations in core | `@Inject`, `@ApplicationScoped` in `engine-core/` | Move to implementation layer |
| Direct instantiation of services | `new \w+Service\(\)` or `new \w+Repository\(\)` | Use dependency injection |
| Raw UUID for entity IDs | `UUID \w+Id` without wrapper | Use strongly-typed ID (`MatchId`, `ContainerId`) |

### MEDIUM Severity (Should Fix)

| Pattern | Detection | Required Action |
|---------|-----------|-----------------|
| Optional as parameter | `Optional<.+>\s+\w+[,)]` in method params | Use overloads or nullable with annotation |
| Missing `final` on injected fields | `@Inject\s+\w+\s+\w+;` without `final` | Add `final` modifier |
| Null return in Optional context | `return null` where `Optional` expected | Return `Optional.empty()` |
| DTO as class instead of record | `public class \w+(Request|Response)` | Convert to `record` |
| Missing validation annotations | Request DTO without `@NotNull`, `@NotBlank`, etc. | Add appropriate validation |

### Detection Script
```bash
for f in $(cat /tmp/changed-java.txt); do
  echo "=== Checking: $f ==="
  
  # HIGH severity
  grep -Hn "@Deprecated" "$f"
  grep -Hn "new \w\+Service\s*(" "$f"
  grep -Hn "new \w\+Repository\s*(" "$f"
  
  # Check if in engine-core with framework annotations
  if [[ "$f" == *"engine-core"* ]]; then
    grep -Hn "@Inject\|@ApplicationScoped\|@Singleton" "$f"
  fi
  
  # MEDIUM severity
  grep -Hn "Optional<.*>\s\+\w\+[,)]" "$f"
  grep -Hn "return null" "$f"
done
```

---

## Step 6: Test Coverage Verification

Every production file must have a corresponding test file.

### Mapping Rules
```
src/main/java/com/example/Foo.java      → src/test/java/com/example/FooTest.java
engine-core/.../MatchService.java       → engine-core/.../MatchServiceTest.java
engine-internal/.../MatchServiceImpl.java → engine-internal/.../MatchServiceImplTest.java
webservice/.../MatchResource.java       → webservice/.../MatchResourceTest.java
```

### Detection Script
```bash
echo "=== Test Coverage Analysis ==="
MISSING_TESTS=""

for prod_file in $(cat /tmp/changed-production.txt); do
  # Convert to expected test path
  test_file=$(echo "$prod_file" | sed 's|src/main/java|src/test/java|' | sed 's|\.java$|Test.java|')
  
  if [[ ! -f "$test_file" ]]; then
    echo "MISSING TEST: $prod_file"
    echo "  Expected: $test_file"
    MISSING_TESTS="$MISSING_TESTS$prod_file\n"
  else
    echo "✓ Has test: $prod_file"
  fi
done

if [[ -n "$MISSING_TESTS" ]]; then
  echo ""
  echo "Files missing tests:"
  echo -e "$MISSING_TESTS"
fi
```

### New Public Methods Check
For modified files with existing tests, verify new public methods have test coverage:
```bash
# Extract new public methods from diff
git diff HEAD~1 --unified=0 -- "*.java" | grep "^+.*public.*(" | grep -v "^+++"
```

**Severity:** HIGH - Tests required for all production code

---

## Step 7: Documentation Verification

### 7a: README.md Updates

If the commit introduces:
- New features or endpoints
- New modules or components
- Configuration changes
- New dependencies
- Breaking changes

Then README.md should be updated.

```bash
# Check if README was modified
git diff HEAD~1 --name-only | grep -i "readme"

# Check for new endpoints (likely need README update)
git diff HEAD~1 -- "*.java" | grep -E "^\+.*@(GET|POST|PUT|DELETE|PATCH|Path)"
```

**Checklist:**
- [ ] New endpoints documented in REST API table
- [ ] New modules listed in Project Structure
- [ ] New configuration properties documented
- [ ] Breaking changes noted

### 7b: docs/ Directory Updates

Check if changes warrant documentation updates:
```bash
# List docs files
ls -la docs/ 2>/dev/null || echo "No docs/ directory"

# Check if docs were modified
git diff HEAD~1 --name-only | grep "^docs/"
```

**When docs/ update is required:**
- New architectural decisions → Update architecture docs
- New modules → Update module documentation
- API changes → Update API documentation
- Configuration changes → Update setup/config docs

### 7c: Javadoc Verification

All public interfaces must have complete Javadoc.

```bash
# Find public interfaces in changed files
for f in $(cat /tmp/changed-java.txt); do
  if grep -q "public interface" "$f"; then
    echo "=== Checking Javadoc: $f ==="
    
    # Check for class-level Javadoc
    if ! grep -B5 "public interface" "$f" | grep -q "/\*\*"; then
      echo "MISSING: Class-level Javadoc"
    fi
    
    # Find public methods without Javadoc
    grep -n "^\s*[A-Za-z].*(.*);" "$f" | while read line; do
      line_num=$(echo "$line" | cut -d: -f1)
      prev_line=$((line_num - 1))
      if ! sed -n "${prev_line}p" "$f" | grep -q "\*/\|^\s*\*"; then
        echo "MISSING: Javadoc for method at line $line_num"
      fi
    done
  fi
done
```

**Javadoc requirements:**
- [ ] Class/interface-level description
- [ ] `@param` for each parameter
- [ ] `@return` for non-void methods
- [ ] `@throws` for declared exceptions

**Severity:** MEDIUM - Documentation completeness

---

## Step 8: OpenAPI Specification (openapi.yaml)

If REST endpoints are added or modified, `openapi.yaml` must be updated.

### Detection
```bash
DIFF_BASE=$(cat /tmp/diff-base.txt)

# Check for new/modified endpoints
ENDPOINT_CHANGES=$(git diff "$DIFF_BASE" -- "*.java" | grep -E "^\+.*@(GET|POST|PUT|DELETE|PATCH|Path)\(")

if [[ -n "$ENDPOINT_CHANGES" ]]; then
  echo "Endpoint changes detected:"
  echo "$ENDPOINT_CHANGES"
  
  # Check if openapi.yaml was updated
  if ! cat /tmp/changed-files.txt | grep -q "openapi.yaml"; then
    echo "⚠ WARNING: Endpoints changed but openapi.yaml not updated"
  else
    echo "✓ openapi.yaml was updated"
  fi
fi
```

### Validation
```bash
# Validate OpenAPI spec syntax (requires swagger-cli or similar)
npx @apidevtools/swagger-cli validate openapi.yaml 2>&1
```

### Checklist
- [ ] New endpoints added to `paths:`
- [ ] Request/response schemas added to `components/schemas:`
- [ ] Path parameters documented
- [ ] Query parameters documented
- [ ] Response codes (200, 201, 400, 404, 500) documented
- [ ] Examples provided for request/response bodies

**Severity:** HIGH - API contract must be accurate

---

## Step 9: Postman Collection

If REST endpoints are added or modified, the Postman collection must be updated.

### Detection
```bash
DIFF_BASE=$(cat /tmp/diff-base.txt)

# Find Postman collection file
POSTMAN_FILE=$(find . -name "*.postman_collection.json" -o -name "postman_collection.json" 2>/dev/null | head -1)

if [[ -n "$ENDPOINT_CHANGES" ]] && [[ -n "$POSTMAN_FILE" ]]; then
  echo "Postman collection: $POSTMAN_FILE"
  
  if ! cat /tmp/changed-files.txt | grep -q "postman"; then
    echo "⚠ WARNING: Endpoints changed but Postman collection not updated"
  else
    echo "✓ Postman collection was updated"
  fi
fi
```

### Checklist
- [ ] New requests added for new endpoints
- [ ] Request bodies match OpenAPI schemas
- [ ] Environment variables used for base URL, auth tokens
- [ ] Example responses documented
- [ ] Folder structure organized by resource
- [ ] Pre-request scripts for auth (if needed)
- [ ] Tests added for response validation

**Severity:** MEDIUM - Developer experience

---

## Step 10: Exception Handling Verification

New exceptions must follow the project pattern.

### Requirements
- [ ] Extends `EngineException` (not `RuntimeException` directly)
- [ ] Has error code in constructor
- [ ] Located in `engine-core/{component}/exception/`
- [ ] Mapped in `EngineExceptionMapper`

```bash
DIFF_BASE=$(cat /tmp/diff-base.txt)

# Find new exception classes
git diff "$DIFF_BASE" -- "*.java" | grep -E "^\+.*class \w+Exception"

# Verify they extend EngineException
for f in $(cat /tmp/changed-files.txt | grep "Exception.java$"); do
  if ! grep -q "extends EngineException" "$f"; then
    echo "⚠ WARNING: $f does not extend EngineException"
  else
    echo "✓ $f extends EngineException"
  fi
done
```

**Severity:** MEDIUM - Exception consistency

---

## Step 11: DTO Validation

All DTOs must follow project conventions.

### Request DTOs
- [ ] Is a Java `record`
- [ ] Has validation annotations (`@NotNull`, `@NotBlank`, `@Min`, `@Max`, etc.)
- [ ] Name ends with `Request`

### Response DTOs
- [ ] Is a Java `record`
- [ ] Name ends with `Response`
- [ ] Has `static from(DomainObject)` factory method

```bash
# Find DTO files in changed files
for f in $(cat /tmp/changed-java.txt | grep -E "(Request|Response)\.java$"); do
  echo "=== Checking DTO: $f ==="
  
  # Check if it's a record
  if ! grep -q "public record" "$f"; then
    echo "WARNING: DTO is not a record"
  fi
  
  # Check for validation (Request only)
  if [[ "$f" == *"Request.java" ]]; then
    if ! grep -q "@NotNull\|@NotBlank\|@Valid\|@Min\|@Max\|@Size" "$f"; then
      echo "WARNING: Request DTO missing validation annotations"
    fi
  fi
  
  # Check for from() method (Response only)
  if [[ "$f" == *"Response.java" ]]; then
    if ! grep -q "static.*from(" "$f"; then
      echo "WARNING: Response DTO missing from() factory method"
    fi
  fi
done
```

**Severity:** MEDIUM - DTO conventions

---

## Step 12: Generate Report

Create `self-review.json` in project root:

```json
{
  "meta": {
    "reviewMode": "single|branch",
    "commitRange": "<hash or range>",
    "commits": [
      {
        "hash": "<hash>",
        "subject": "<subject>",
        "messageValid": true
      }
    ],
    "date": "<ISO timestamp>",
    "filesChanged": 0,
    "reviewedAt": "<ISO timestamp>"
  },
  "build": {
    "passed": true,
    "exitCode": 0,
    "errors": 0,
    "warnings": 0,
    "testFailures": 0
  },
  "findings": [
    {
      "id": "F001",
      "severity": "critical|high|medium|low",
      "category": "architecture|pattern|testing|documentation|api-spec|convention",
      "file": "<filepath>",
      "line": null,
      "rule": "<rule name>",
      "description": "<what's wrong>",
      "recommendation": "<how to fix>",
      "autoFixable": false
    }
  ],
  "coverage": {
    "productionFiles": 0,
    "filesWithTests": 0,
    "filesMissingTests": [],
    "estimatedCoverage": "unknown"
  },
  "documentation": {
    "readmeUpdated": false,
    "readmeUpdateNeeded": false,
    "docsUpdated": false,
    "docsUpdateNeeded": false,
    "javadocComplete": true,
    "missingJavadoc": []
  },
  "apiSpec": {
    "openapiUpdated": false,
    "openapiUpdateNeeded": false,
    "openapiValid": true,
    "postmanUpdated": false,
    "postmanUpdateNeeded": false
  },
  "grade": {
    "overall": "A",
    "breakdown": {
      "build": "A",
      "architecture": "A",
      "codeQuality": "A",
      "testCoverage": "A",
      "documentation": "A",
      "apiSpec": "A"
    }
  },
  "summary": {
    "critical": 0,
    "high": 0,
    "medium": 0,
    "low": 0,
    "passed": true
  },
  "recommendations": []
}
```

---

## Step 13: Handle High-Severity Issues

For each CRITICAL or HIGH severity finding:

1. **Present the issue** clearly to the user
2. **Offer to fix** if auto-fixable
3. **Wait for user decision** before proceeding
4. **Track resolution** in the report

### Auto-Fixable Issues
- Missing `final` on injected fields
- DTO class → record conversion (simple cases)
- Missing `@Override` annotations
- Import organization

### Require Manual Fix
- Architecture violations
- Missing tests
- Missing documentation
- API spec updates

---

## Step 14: Present Summary

```bash
REVIEW_MODE=$(cat /tmp/review-mode.txt)
COMMIT_RANGE=$(cat /tmp/commit-range.txt)
```

### Single Commit Summary
```
## Self-Review Summary

**Commit:** <hash> - <subject>
**Grade:** <overall grade>

### Build Status
✓ Build passed | ✗ Build failed

### Findings
- Critical: <count>
- High: <count>
- Medium: <count>  
- Low: <count>

### Coverage
- Production files: <count>
- Files with tests: <count>
- Missing tests: <list or "None">

### Documentation
- README: ✓ Updated | ⚠ Update needed | ✓ No update needed
- docs/: ✓ Updated | ⚠ Update needed | ✓ No update needed
- Javadoc: ✓ Complete | ⚠ Incomplete
- OpenAPI: ✓ Updated | ⚠ Update needed | ✓ No update needed
- Postman: ✓ Updated | ⚠ Update needed | ✓ No update needed

### Top Recommendations
1. <highest priority action>
2. <second priority action>
3. <third priority action>

### Verdict
✓ Ready to push | ⚠ Address <N> issues before push | ✗ Do not push
```

### Branch Review Summary (when using `*`)
```
## Branch Self-Review Summary

**Branch:** <current branch>
**Commits:** <count> commits since main
**Range:** <merge-base>..<HEAD>
**Grade:** <overall grade>

### Commits Reviewed
| Hash | Subject | Message Valid |
|------|---------|---------------|
| abc123 | feat(x): add feature | ✓ |
| def456 | fix(y): fix bug | ✓ |

### Build Status
✓ Build passed | ✗ Build failed

### Cumulative Changes
- Files changed: <count>
- Lines added: <count>
- Lines removed: <count>

### Findings (across all commits)
- Critical: <count>
- High: <count>
- Medium: <count>  
- Low: <count>

### Coverage
- Production files: <count>
- Files with tests: <count>
- Missing tests: <list or "None">

### Documentation
- README: ✓ Updated | ⚠ Update needed | ✓ No update needed
- docs/: ✓ Updated | ⚠ Update needed | ✓ No update needed
- Javadoc: ✓ Complete | ⚠ Incomplete
- OpenAPI: ✓ Updated | ⚠ Update needed | ✓ No update needed
- Postman: ✓ Updated | ⚠ Update needed | ✓ No update needed

### Top Recommendations
1. <highest priority action>
2. <second priority action>
3. <third priority action>

### Verdict
✓ Ready to merge to main | ⚠ Address <N> issues before merge | ✗ Do not merge
```

---

## Grading Rubric

| Grade | Criteria |
|-------|----------|
| **A** | Build passes, no high/critical findings, tests present, docs updated, API specs current |
| **B** | Build passes, ≤2 medium findings, minor doc gaps |
| **C** | Build passes, some test gaps, documentation incomplete |
| **D** | Build passes but significant issues (missing tests, outdated specs) |
| **F** | Build fails OR critical/high findings OR major gaps |

---

## Quick Reference: Must-Pass Checks

Before declaring "ready to push":

- [ ] `./build.sh all` exits 0
- [ ] No `@Deprecated` annotations in new code
- [ ] No framework annotations in `engine-core/`
- [ ] All production files have corresponding test files
- [ ] All public interfaces have Javadoc
- [ ] OpenAPI spec updated for endpoint changes
- [ ] Postman collection updated for endpoint changes
- [ ] README.md updated for user-facing changes
- [ ] docs/ updated for architectural changes
- [ ] Commit message follows conventional format
