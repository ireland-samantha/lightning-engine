---
name: test-coverage
description: Analyze test coverage for Java codebase. Maps production files to test files, identifies missing tests, analyzes test quality, and produces detailed coverage report. Use when checking test coverage, finding untested code, reviewing test quality, or before releases. Triggers on requests like "check test coverage", "find missing tests", "test analysis", "coverage report", or "what needs tests".
---

# Test Coverage Skill

Analyze test coverage by mapping production files to tests and identifying gaps.

## Arguments

| Argument | Description | Examples |
|----------|-------------|----------|
| `<path>` | Directory to analyze | `src/main/java`, `engine-core/` |
| `--changed` | Only analyze files changed since main | For PR reviews |
| `--module <name>` | Analyze specific module | `engine-core`, `webservice` |
| `--strict` | Fail if coverage < 80% | For CI/CD gates |
| (none) | Defaults to full project analysis | Comprehensive report |

## Coverage Philosophy

This skill performs **structural coverage analysis** - mapping production code to test files. It does not execute tests or measure line/branch coverage (use JaCoCo for that).

### What We Measure
- **File coverage**: Does each production file have a test file?
- **Method coverage**: Do new public methods have corresponding tests?
- **Test quality signals**: Test naming, assertions, mocking patterns
- **Test organization**: Proper test directory structure

---

## Step 1: Gather Files

```bash
TARGET_PATH="${1:-./}"
ANALYZE_MODE="full"
TARGET_MODULE=""
STRICT_MODE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --changed)
      ANALYZE_MODE="changed"
      shift
      ;;
    --module)
      TARGET_MODULE="$2"
      shift 2
      ;;
    --strict)
      STRICT_MODE=true
      shift
      ;;
    *)
      TARGET_PATH="$1"
      shift
      ;;
  esac
done

echo "=== Test Coverage Analysis ==="
echo "Path: $TARGET_PATH"
echo "Mode: $ANALYZE_MODE"
echo "Module: ${TARGET_MODULE:-all}"
echo "Strict: $STRICT_MODE"
echo ""

# Gather production files
if [[ "$ANALYZE_MODE" == "changed" ]]; then
  MERGE_BASE=$(git merge-base main HEAD 2>/dev/null || echo "HEAD~10")
  git diff "$MERGE_BASE" --name-only | grep "\.java$" | grep -v "Test\.java$" | grep "src/main" > /tmp/production-files.txt
  echo "Analyzing $(wc -l < /tmp/production-files.txt) changed production files"
else
  if [[ -n "$TARGET_MODULE" ]]; then
    find "$TARGET_PATH" -path "*/$TARGET_MODULE/*" -name "*.java" -path "*/src/main/*" ! -name "*Test.java" > /tmp/production-files.txt
  else
    find "$TARGET_PATH" -name "*.java" -path "*/src/main/*" ! -name "*Test.java" 2>/dev/null > /tmp/production-files.txt
  fi
  echo "Found $(wc -l < /tmp/production-files.txt) production files"
fi

# Gather test files
find "$TARGET_PATH" -name "*Test.java" -path "*/src/test/*" 2>/dev/null > /tmp/test-files.txt
echo "Found $(wc -l < /tmp/test-files.txt) test files"
```

---

## Step 2: Map Production to Test Files

### Standard Mapping Rules

| Production Path | Expected Test Path |
|-----------------|-------------------|
| `src/main/java/com/example/Foo.java` | `src/test/java/com/example/FooTest.java` |
| `engine-core/src/main/.../Bar.java` | `engine-core/src/test/.../BarTest.java` |
| `FooService.java` | `FooServiceTest.java` |
| `FooServiceImpl.java` | `FooServiceImplTest.java` |

### Mapping Script
```bash
echo "=== Mapping Production to Tests ==="

TOTAL_PROD=0
WITH_TESTS=0
MISSING_TESTS=""

for prod_file in $(cat /tmp/production-files.txt); do
  TOTAL_PROD=$((TOTAL_PROD + 1))
  
  # Convert production path to expected test path
  test_file=$(echo "$prod_file" | sed 's|/src/main/java/|/src/test/java/|' | sed 's|\.java$|Test.java|')
  
  # Also check for alternative test naming (e.g., IT for integration tests)
  test_file_it=$(echo "$prod_file" | sed 's|/src/main/java/|/src/test/java/|' | sed 's|\.java$|IT.java|')
  
  if [[ -f "$test_file" ]]; then
    WITH_TESTS=$((WITH_TESTS + 1))
    echo "✓ $prod_file"
    echo "  → $test_file"
  elif [[ -f "$test_file_it" ]]; then
    WITH_TESTS=$((WITH_TESTS + 1))
    echo "✓ $prod_file (integration test)"
    echo "  → $test_file_it"
  else
    echo "✗ $prod_file"
    echo "  Expected: $test_file"
    MISSING_TESTS="$MISSING_TESTS$prod_file\n"
  fi
done

COVERAGE_PCT=$((WITH_TESTS * 100 / TOTAL_PROD))
echo ""
echo "File Coverage: $WITH_TESTS / $TOTAL_PROD ($COVERAGE_PCT%)"

# Save for report
echo "$COVERAGE_PCT" > /tmp/coverage-pct.txt
echo -e "$MISSING_TESTS" > /tmp/missing-tests.txt
```

---

## Step 3: Analyze Missing Test Criticality

Not all missing tests are equal. Prioritize based on file type.

### Priority Classification

| File Type | Priority | Rationale |
|-----------|----------|-----------|
| `*Service.java` | CRITICAL | Core business logic |
| `*ServiceImpl.java` | CRITICAL | Implementation must be tested |
| `*Repository.java` | HIGH | Data access layer |
| `*Resource.java` | HIGH | API endpoints |
| `*Controller.java` | HIGH | HTTP handling |
| `*Handler.java` | HIGH | Event/command handling |
| `*Mapper.java` | MEDIUM | Data transformation |
| `*Validator.java` | MEDIUM | Validation logic |
| `*Factory.java` | MEDIUM | Object creation |
| `*Config.java` | LOW | Usually tested via integration |
| `*Exception.java` | LOW | Simple POJOs |
| `*Dto.java` / `*Request.java` / `*Response.java` | LOW | Data classes |
| `*Constants.java` | SKIP | No behavior |
| `package-info.java` | SKIP | Metadata only |

### Prioritization Script
```bash
echo "=== Missing Tests by Priority ==="

CRITICAL_MISSING=""
HIGH_MISSING=""
MEDIUM_MISSING=""
LOW_MISSING=""

for f in $(cat /tmp/missing-tests.txt | grep -v "^$"); do
  filename=$(basename "$f")
  
  # Skip non-testable files
  if [[ "$filename" == "package-info.java" ]] || [[ "$filename" == *"Constants.java" ]]; then
    continue
  fi
  
  # Classify
  if [[ "$filename" == *"Service.java" ]] || [[ "$filename" == *"ServiceImpl.java" ]]; then
    CRITICAL_MISSING="$CRITICAL_MISSING$f\n"
  elif [[ "$filename" == *"Repository.java" ]] || [[ "$filename" == *"Resource.java" ]] || \
       [[ "$filename" == *"Controller.java" ]] || [[ "$filename" == *"Handler.java" ]]; then
    HIGH_MISSING="$HIGH_MISSING$f\n"
  elif [[ "$filename" == *"Mapper.java" ]] || [[ "$filename" == *"Validator.java" ]] || \
       [[ "$filename" == *"Factory.java" ]]; then
    MEDIUM_MISSING="$MEDIUM_MISSING$f\n"
  elif [[ "$filename" == *"Config.java" ]] || [[ "$filename" == *"Exception.java" ]] || \
       [[ "$filename" == *"Dto.java" ]] || [[ "$filename" == *"Request.java" ]] || \
       [[ "$filename" == *"Response.java" ]]; then
    LOW_MISSING="$LOW_MISSING$f\n"
  else
    MEDIUM_MISSING="$MEDIUM_MISSING$f\n"
  fi
done

echo ""
echo "CRITICAL (must have tests):"
echo -e "$CRITICAL_MISSING" | grep -v "^$" | while read f; do echo "  - $f"; done

echo ""
echo "HIGH (should have tests):"
echo -e "$HIGH_MISSING" | grep -v "^$" | while read f; do echo "  - $f"; done

echo ""
echo "MEDIUM (recommended to have tests):"
echo -e "$MEDIUM_MISSING" | grep -v "^$" | while read f; do echo "  - $f"; done

echo ""
echo "LOW (optional tests):"
echo -e "$LOW_MISSING" | grep -v "^$" | while read f; do echo "  - $f"; done
```

---

## Step 4: Analyze New Public Methods

For files that DO have tests, check if new public methods are covered.

```bash
echo "=== New Public Methods Analysis ==="

if [[ "$ANALYZE_MODE" == "changed" ]]; then
  MERGE_BASE=$(cat /tmp/diff-base.txt 2>/dev/null || git merge-base main HEAD)
  
  # Extract new public methods from diff
  git diff "$MERGE_BASE" --unified=0 -- "*.java" | \
    grep -E "^\+\s*public\s+\w+\s+\w+\s*\(" | \
    grep -v "^\+\+\+" > /tmp/new-public-methods.txt
  
  echo "New public methods found:"
  cat /tmp/new-public-methods.txt
  
  # For each new method, check if corresponding test exists
  echo ""
  echo "Checking test coverage for new methods..."
  
  # This is a heuristic - we look for test methods with similar names
  cat /tmp/new-public-methods.txt | while read method_line; do
    # Extract method name
    method_name=$(echo "$method_line" | grep -oE "\w+\s*\(" | sed 's/\s*($//')
    
    if [[ -n "$method_name" ]]; then
      # Search for test methods containing this name
      TEST_EXISTS=$(grep -rl "void.*$method_name\|test.*$method_name\|$method_name.*Test" --include="*Test.java" . 2>/dev/null | head -1)
      
      if [[ -n "$TEST_EXISTS" ]]; then
        echo "✓ $method_name - likely tested in $TEST_EXISTS"
      else
        echo "✗ $method_name - no obvious test found"
      fi
    fi
  done
fi
```

---

## Step 5: Test Quality Analysis

Analyze existing test files for quality signals.

### Quality Indicators

| Indicator | Good Sign | Warning Sign |
|-----------|-----------|--------------|
| Assertions per test | ≥1 | 0 (test does nothing) |
| Test method naming | Descriptive (`should_*`, `when_*`) | Generic (`test1`, `testMethod`) |
| Mocking | Used appropriately | Mocking everything (over-isolation) |
| Test size | Focused, small | Very long test methods |
| Setup/Teardown | Shared setup for related tests | Duplicate setup code |

### Quality Script
```bash
echo "=== Test Quality Analysis ==="

for test_file in $(cat /tmp/test-files.txt | head -50); do  # Limit for performance
  filename=$(basename "$test_file")
  
  # Count test methods
  TEST_METHODS=$(grep -c "@Test" "$test_file" 2>/dev/null || echo 0)
  
  # Count assertions
  ASSERTIONS=$(grep -cE "assert|verify|should|expect" "$test_file" 2>/dev/null || echo 0)
  
  # Check for empty tests (test annotation but no assertions)
  if [[ $TEST_METHODS -gt 0 ]] && [[ $ASSERTIONS -eq 0 ]]; then
    echo "⚠ $filename - $TEST_METHODS tests but no assertions found"
  fi
  
  # Check for poor naming
  POOR_NAMES=$(grep -c "@Test" -A1 "$test_file" | grep -cE "void test[0-9]|void testMethod" 2>/dev/null || echo 0)
  if [[ $POOR_NAMES -gt 0 ]]; then
    echo "⚠ $filename - $POOR_NAMES poorly named test methods"
  fi
  
  # Check for very long test methods (>50 lines)
  # This is a rough heuristic
  LONG_TESTS=$(awk '/@Test/{start=NR} /^\s*\}/ && start{if(NR-start>50) print "long"; start=0}' "$test_file" | wc -l)
  if [[ $LONG_TESTS -gt 0 ]]; then
    echo "⚠ $filename - $LONG_TESTS very long test methods (>50 lines)"
  fi
  
  # Assertions per test ratio
  if [[ $TEST_METHODS -gt 0 ]]; then
    RATIO=$((ASSERTIONS / TEST_METHODS))
    if [[ $RATIO -lt 1 ]]; then
      echo "⚠ $filename - Low assertion ratio ($ASSERTIONS assertions / $TEST_METHODS tests)"
    fi
  fi
done
```

---

## Step 6: Test Organization Analysis

Verify tests follow project structure conventions.

```bash
echo "=== Test Organization Analysis ==="

# Check for tests outside standard location
MISPLACED=$(find . -name "*Test.java" ! -path "*/src/test/*" ! -path "*/target/*" ! -path "*/.git/*" 2>/dev/null)
if [[ -n "$MISPLACED" ]]; then
  echo "⚠ Tests outside src/test/:"
  echo "$MISPLACED"
fi

# Check for test utilities/helpers
TEST_UTILS=$(find . -path "*/src/test/*" -name "*Fixture*.java" -o -name "*TestHelper*.java" -o -name "*TestUtil*.java" 2>/dev/null | wc -l)
echo "Test utilities found: $TEST_UTILS"

# Check for test resources
TEST_RESOURCES=$(find . -path "*/src/test/resources/*" -type f 2>/dev/null | wc -l)
echo "Test resource files: $TEST_RESOURCES"

# Module coverage breakdown
echo ""
echo "=== Coverage by Module ==="
for module_dir in $(find . -maxdepth 2 -type d -name "src" -exec dirname {} \; 2>/dev/null | sort -u); do
  module_name=$(basename "$module_dir")
  prod_count=$(find "$module_dir" -path "*/src/main/*" -name "*.java" ! -name "*Test.java" 2>/dev/null | wc -l)
  test_count=$(find "$module_dir" -path "*/src/test/*" -name "*Test.java" 2>/dev/null | wc -l)
  
  if [[ $prod_count -gt 0 ]]; then
    ratio=$((test_count * 100 / prod_count))
    echo "$module_name: $test_count tests / $prod_count production files ($ratio%)"
  fi
done
```

---

## Step 7: Generate Report

Create `test-coverage.json`:

```json
{
  "meta": {
    "analyzedAt": "<ISO timestamp>",
    "path": "<analyzed path>",
    "mode": "full|changed",
    "module": "<module or null>"
  },
  "summary": {
    "productionFiles": 0,
    "filesWithTests": 0,
    "filesMissingTests": 0,
    "fileCoveragePercent": 0,
    "strictModePassed": true
  },
  "missingTests": {
    "critical": [
      {
        "file": "<filepath>",
        "type": "Service",
        "expectedTestPath": "<path>",
        "publicMethods": 5
      }
    ],
    "high": [],
    "medium": [],
    "low": []
  },
  "newMethods": {
    "total": 0,
    "likelyCovered": 0,
    "potentiallyUncovered": [
      {
        "file": "<filepath>",
        "method": "<methodName>",
        "signature": "<full signature>"
      }
    ]
  },
  "testQuality": {
    "totalTestFiles": 0,
    "totalTestMethods": 0,
    "totalAssertions": 0,
    "avgAssertionsPerTest": 0.0,
    "warnings": [
      {
        "file": "<filepath>",
        "issue": "No assertions found",
        "severity": "high"
      }
    ]
  },
  "byModule": [
    {
      "module": "<name>",
      "productionFiles": 0,
      "testFiles": 0,
      "coveragePercent": 0
    }
  ],
  "grade": {
    "overall": "A-F",
    "breakdown": {
      "fileCoverage": "A-F",
      "criticalCoverage": "A-F",
      "testQuality": "A-F"
    }
  },
  "recommendations": [
    {
      "priority": 1,
      "action": "Add tests for UserService",
      "file": "<filepath>",
      "effort": "medium",
      "impact": "high"
    }
  ]
}
```

---

## Step 8: Present Summary

```
## Test Coverage Report

**Path:** <analyzed path>
**Mode:** <full|changed>
**Grade:** <overall grade>

### File Coverage
| Metric | Value |
|--------|-------|
| Production Files | 100 |
| Files with Tests | 85 |
| Coverage | 85% |

### Missing Tests by Priority

| Priority | Count | Files |
|----------|-------|-------|
| CRITICAL | 2 | UserService, PaymentService |
| HIGH | 5 | UserRepo, OrderResource, ... |
| MEDIUM | 8 | ... |
| LOW | 12 | ... |

### Module Breakdown
| Module | Tests | Production | Coverage |
|--------|-------|------------|----------|
| engine-core | 45 | 50 | 90% |
| engine-internal | 30 | 40 | 75% |
| webservice | 20 | 35 | 57% |

### Test Quality Warnings
- ⚠ UserServiceTest.java - 5 tests but only 2 assertions
- ⚠ OrderProcessorTest.java - poorly named test methods

### New Methods (Changed Files Only)
- ✓ createUser() - likely covered
- ✗ processRefund() - no test found
- ✗ validatePayment() - no test found

### Top Recommendations
1. **[CRITICAL]** Add UserServiceTest.java - core business logic untested
2. **[CRITICAL]** Add PaymentServiceTest.java - financial operations untested  
3. **[HIGH]** Add tests for OrderResource endpoints

### Verdict
✓ Coverage acceptable (85%) | ⚠ Critical files need tests | ✗ Below threshold (strict mode)
```

---

## Grading Rubric

### File Coverage Grade
| Grade | Criteria |
|-------|----------|
| **A** | ≥90% file coverage, no critical missing |
| **B** | ≥80% file coverage, ≤1 critical missing |
| **C** | ≥70% file coverage, ≤3 critical missing |
| **D** | ≥60% file coverage |
| **F** | <60% file coverage |

### Critical Coverage Grade
| Grade | Criteria |
|-------|----------|
| **A** | All services and handlers tested |
| **B** | ≤1 critical file missing tests |
| **C** | ≤3 critical files missing tests |
| **D** | ≤5 critical files missing tests |
| **F** | >5 critical files missing tests |

### Test Quality Grade
| Grade | Criteria |
|-------|----------|
| **A** | No quality warnings |
| **B** | ≤3 quality warnings |
| **C** | ≤7 quality warnings |
| **D** | ≤15 quality warnings |
| **F** | >15 quality warnings |

---

## CI/CD Integration

### Strict Mode (`--strict`)

When `--strict` is passed:
- Exit code 1 if overall coverage < 80%
- Exit code 1 if any CRITICAL files missing tests
- Exit code 0 otherwise

```bash
if [[ "$STRICT_MODE" == "true" ]]; then
  COVERAGE=$(cat /tmp/coverage-pct.txt)
  CRITICAL_COUNT=$(echo -e "$CRITICAL_MISSING" | grep -c -v "^$" || echo 0)
  
  if [[ $COVERAGE -lt 80 ]]; then
    echo "❌ STRICT MODE FAILED: Coverage $COVERAGE% < 80%"
    exit 1
  fi
  
  if [[ $CRITICAL_COUNT -gt 0 ]]; then
    echo "❌ STRICT MODE FAILED: $CRITICAL_COUNT critical files missing tests"
    exit 1
  fi
  
  echo "✓ STRICT MODE PASSED"
  exit 0
fi
```

### GitHub Actions Example
```yaml
- name: Check Test Coverage
  run: |
    claude "test-coverage --strict"
```
