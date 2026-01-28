---
name: solid-review
description: Analyze Java codebase for SOLID principle violations. Use when reviewing code architecture, refactoring legacy code, or ensuring design quality. Triggers on requests like "check SOLID principles", "find design violations", "architecture review", "SOLID analysis", or "design patterns review".
---

# SOLID Review Skill

Analyze Java files for SOLID principle violations and produce a detailed report.

## Arguments

| Argument | Description | Examples |
|----------|-------------|----------|
| `<path>` | Directory or file to analyze | `src/main/java`, `./`, `MyClass.java` |
| `--changed` | Only analyze files changed since main | Useful for PR reviews |
| (none) | Defaults to `src/main/java` | Full codebase analysis |

## SOLID Principles Overview

| Principle | Description | Key Indicator of Violation |
|-----------|-------------|---------------------------|
| **S**ingle Responsibility | A class should have only one reason to change | Class doing too many things |
| **O**pen/Closed | Open for extension, closed for modification | Excessive switch/if-else on types |
| **L**iskov Substitution | Subtypes must be substitutable for base types | Overridden methods changing contracts |
| **I**nterface Segregation | Clients shouldn't depend on unused methods | Fat interfaces, empty implementations |
| **D**ependency Inversion | Depend on abstractions, not concretions | Direct instantiation, concrete dependencies |

---

## Step 1: Gather Files to Analyze

```bash
TARGET_PATH="${1:-src/main/java}"
ANALYZE_MODE="full"

if [[ "$1" == "--changed" ]]; then
  ANALYZE_MODE="changed"
  MERGE_BASE=$(git merge-base main HEAD 2>/dev/null || echo "HEAD~10")
  git diff "$MERGE_BASE" --name-only | grep "\.java$" > /tmp/solid-files.txt
  echo "Analyzing $(wc -l < /tmp/solid-files.txt) changed Java files"
else
  find "$TARGET_PATH" -name "*.java" -type f | grep -v "Test\.java$" > /tmp/solid-files.txt
  echo "Analyzing $(wc -l < /tmp/solid-files.txt) Java files in $TARGET_PATH"
fi

# Exclude test files for main analysis
grep -v "Test\.java$" /tmp/solid-files.txt > /tmp/solid-production.txt || true
```

---

## Step 2: Single Responsibility Principle (SRP)

**Violation:** A class has more than one reason to change.

### Detection Heuristics

| Indicator | Threshold | Severity |
|-----------|-----------|----------|
| Lines of code | > 300 lines | MEDIUM |
| Lines of code | > 500 lines | HIGH |
| Public methods | > 10 methods | MEDIUM |
| Public methods | > 15 methods | HIGH |
| Injected dependencies | > 5 dependencies | MEDIUM |
| Injected dependencies | > 8 dependencies | HIGH |
| Mixed concerns | Multiple unrelated domains | HIGH |

### Detection Script
```bash
echo "=== SRP Analysis ==="

for f in $(cat /tmp/solid-production.txt); do
  CLASS_NAME=$(basename "$f" .java)
  
  # Line count
  LINES=$(wc -l < "$f")
  if [[ $LINES -gt 500 ]]; then
    echo "HIGH: $f - $LINES lines (>500 suggests multiple responsibilities)"
  elif [[ $LINES -gt 300 ]]; then
    echo "MEDIUM: $f - $LINES lines (>300 may indicate too many responsibilities)"
  fi
  
  # Public method count
  PUBLIC_METHODS=$(grep -c "^\s*public\s\+[^c].*(" "$f" 2>/dev/null || echo 0)
  if [[ $PUBLIC_METHODS -gt 15 ]]; then
    echo "HIGH: $f - $PUBLIC_METHODS public methods (>15 suggests god class)"
  elif [[ $PUBLIC_METHODS -gt 10 ]]; then
    echo "MEDIUM: $f - $PUBLIC_METHODS public methods (>10 may be doing too much)"
  fi
  
  # Injected dependency count
  INJECT_COUNT=$(grep -c "@Inject" "$f" 2>/dev/null || echo 0)
  if [[ $INJECT_COUNT -gt 8 ]]; then
    echo "HIGH: $f - $INJECT_COUNT injected dependencies (>8 indicates too many collaborators)"
  elif [[ $INJECT_COUNT -gt 5 ]]; then
    echo "MEDIUM: $f - $INJECT_COUNT injected dependencies (>5 may indicate mixed concerns)"
  fi
  
  # Mixed concern detection (heuristic: multiple domain keywords)
  CONCERN_PATTERNS="Repository|Service|Controller|Handler|Processor|Validator|Mapper|Client|Provider"
  CONCERN_COUNT=$(grep -oE "$CONCERN_PATTERNS" "$f" | sort -u | wc -l)
  if [[ $CONCERN_COUNT -gt 3 ]]; then
    echo "MEDIUM: $f - References $CONCERN_COUNT different concern types"
  fi
done
```

### Common SRP Violations
- **God classes**: Classes that know too much or do too much
- **Manager/Util classes**: Often a dumping ground for unrelated methods
- **Mixed I/O and business logic**: Reading files AND processing data
- **UI + Business logic**: Handling HTTP AND domain operations

---

## Step 3: Open/Closed Principle (OCP)

**Violation:** Code requires modification to add new behavior instead of extension.

### Detection Heuristics

| Indicator | Detection | Severity |
|-----------|-----------|----------|
| Type-checking switches | `switch` on enum/type with `instanceof` | MEDIUM |
| Long if-else chains | > 4 branches checking types | MEDIUM |
| Frequent modifications | Same class modified for each new feature | HIGH |
| Missing strategy pattern | Hardcoded algorithms | MEDIUM |

### Detection Script
```bash
echo "=== OCP Analysis ==="

for f in $(cat /tmp/solid-production.txt); do
  # Switch statements (potential type checking)
  SWITCH_COUNT=$(grep -c "switch\s*(" "$f" 2>/dev/null || echo 0)
  if [[ $SWITCH_COUNT -gt 2 ]]; then
    echo "MEDIUM: $f - $SWITCH_COUNT switch statements (consider polymorphism)"
    # Show what's being switched on
    grep -n "switch\s*(" "$f" | head -5
  fi
  
  # instanceof checks
  INSTANCEOF_COUNT=$(grep -c "instanceof" "$f" 2>/dev/null || echo 0)
  if [[ $INSTANCEOF_COUNT -gt 3 ]]; then
    echo "HIGH: $f - $INSTANCEOF_COUNT instanceof checks (violates OCP)"
    grep -n "instanceof" "$f" | head -5
  fi
  
  # Long if-else chains
  IF_ELSE_CHAIN=$(grep -c "else if" "$f" 2>/dev/null || echo 0)
  if [[ $IF_ELSE_CHAIN -gt 4 ]]; then
    echo "MEDIUM: $f - $IF_ELSE_CHAIN else-if branches (consider strategy/factory)"
  fi
  
  # Enum-based type checking in switches
  if grep -q "switch.*getType\|switch.*\.type" "$f"; then
    echo "MEDIUM: $f - Switch on type field detected"
  fi
done
```

### Common OCP Violations
- **Type switches**: `switch(entity.getType())` requiring modification for new types
- **Feature flags**: Hardcoded if-else for features instead of pluggable modules
- **Hardcoded algorithms**: Direct implementation instead of strategy injection

---

## Step 4: Liskov Substitution Principle (LSP)

**Violation:** Subclass changes the behavior contract of its parent.

### Detection Heuristics

| Indicator | Detection | Severity |
|-----------|-----------|----------|
| Empty override methods | Method body is empty or just `return` | HIGH |
| Throwing in overrides | Override throws `UnsupportedOperationException` | HIGH |
| Weakened preconditions | Override has stricter null checks than parent | MEDIUM |
| Strengthened postconditions | Override returns narrower types unexpectedly | MEDIUM |

### Detection Script
```bash
echo "=== LSP Analysis ==="

for f in $(cat /tmp/solid-production.txt); do
  # Check for UnsupportedOperationException in overrides
  if grep -q "@Override" "$f"; then
    if grep -A5 "@Override" "$f" | grep -q "UnsupportedOperationException\|throw new.*NotImplemented"; then
      echo "HIGH: $f - Override throws UnsupportedOperationException (LSP violation)"
      grep -B1 -A5 "UnsupportedOperationException" "$f" | head -10
    fi
  fi
  
  # Empty override methods
  if grep -A3 "@Override" "$f" | grep -qE "^\s*\}\s*$" ; then
    # More precise: find @Override followed by method with empty body
    awk '/@Override/{found=1} found && /\{\s*\}/' "$f" | head -5
    if [[ $? -eq 0 ]]; then
      echo "MEDIUM: $f - Potentially empty override method"
    fi
  fi
  
  # Override that just calls super (may be pointless or hiding something)
  if grep -A3 "@Override" "$f" | grep -q "return super\\."; then
    echo "LOW: $f - Override just delegates to super (review if necessary)"
  fi
  
  # Null returns where parent might not expect
  if grep -q "@Override" "$f" && grep -A10 "@Override" "$f" | grep -q "return null"; then
    echo "MEDIUM: $f - Override returns null (verify parent contract allows this)"
  fi
done
```

### Common LSP Violations
- **Square/Rectangle problem**: Subclass can't fulfill parent's contract
- **NotImplementedException**: Interface method throws instead of implementing
- **Collection subclass restrictions**: Subclass doesn't support all operations

---

## Step 5: Interface Segregation Principle (ISP)

**Violation:** Clients forced to depend on methods they don't use.

### Detection Heuristics

| Indicator | Detection | Severity |
|-----------|-----------|----------|
| Large interfaces | > 7 methods | MEDIUM |
| Large interfaces | > 10 methods | HIGH |
| Empty implementations | Implements interface but leaves methods empty | HIGH |
| Default method overuse | > 3 default methods in interface | MEDIUM |

### Detection Script
```bash
echo "=== ISP Analysis ==="

# Find interfaces and count methods
for f in $(cat /tmp/solid-production.txt); do
  if grep -q "^public interface\|^ *interface " "$f"; then
    INTERFACE_NAME=$(grep -oE "interface\s+\w+" "$f" | head -1 | awk '{print $2}')
    
    # Count abstract methods (excluding default and static)
    METHOD_COUNT=$(grep -E "^\s+[a-zA-Z].*\(.*\);" "$f" | grep -v "default\|static" | wc -l)
    
    if [[ $METHOD_COUNT -gt 10 ]]; then
      echo "HIGH: $f ($INTERFACE_NAME) - $METHOD_COUNT methods (fat interface)"
    elif [[ $METHOD_COUNT -gt 7 ]]; then
      echo "MEDIUM: $f ($INTERFACE_NAME) - $METHOD_COUNT methods (consider splitting)"
    fi
    
    # Count default methods
    DEFAULT_COUNT=$(grep -c "default\s\+" "$f" 2>/dev/null || echo 0)
    if [[ $DEFAULT_COUNT -gt 3 ]]; then
      echo "MEDIUM: $f - $DEFAULT_COUNT default methods (interface doing too much)"
    fi
  fi
done

# Find classes with empty/stub implementations
echo ""
echo "=== Checking for stub implementations ==="
for f in $(cat /tmp/solid-production.txt); do
  if grep -q "implements" "$f"; then
    # Look for empty method bodies after implements
    EMPTY_IMPL=$(grep -cE "public\s+\w+\s+\w+\(.*\)\s*\{\s*\}" "$f" 2>/dev/null || echo 0)
    if [[ $EMPTY_IMPL -gt 0 ]]; then
      echo "HIGH: $f - $EMPTY_IMPL empty method implementations (ISP violation)"
    fi
    
    # Methods that just return null/0/false
    STUB_RETURNS=$(grep -cE "return (null|0|false|Collections\.empty);" "$f" 2>/dev/null || echo 0)
    if [[ $STUB_RETURNS -gt 2 ]]; then
      echo "MEDIUM: $f - $STUB_RETURNS stub return statements"
    fi
  fi
done
```

### Common ISP Violations
- **Blob interfaces**: One interface for all operations
- **Partial implementations**: Classes implement interface but throw/return null for some methods
- **Marker interfaces with methods**: Should be separate concerns

---

## Step 6: Dependency Inversion Principle (DIP)

**Violation:** High-level modules depend on low-level modules instead of abstractions.

### Detection Heuristics

| Indicator | Detection | Severity |
|-----------|-----------|----------|
| Direct instantiation | `new ConcreteClass()` for services | HIGH |
| Concrete field types | Field is `ArrayList` not `List` | MEDIUM |
| Concrete parameters | Method takes `HashMap` not `Map` | MEDIUM |
| Missing interface | Service class with no interface | MEDIUM |
| Framework coupling | Business logic imports framework classes | HIGH |

### Detection Script
```bash
echo "=== DIP Analysis ==="

for f in $(cat /tmp/solid-production.txt); do
  CLASS_NAME=$(basename "$f" .java)
  
  # Direct instantiation of services/repositories (should be injected)
  NEW_SERVICE=$(grep -n "new \w\+\(Service\|Repository\|Client\|Provider\|Handler\)\s*(" "$f" 2>/dev/null)
  if [[ -n "$NEW_SERVICE" ]]; then
    echo "HIGH: $f - Direct instantiation of service/repository:"
    echo "$NEW_SERVICE" | head -5
  fi
  
  # Concrete collection types in fields
  CONCRETE_COLLECTIONS=$(grep -n "private\s\+\(ArrayList\|HashMap\|HashSet\|LinkedList\)\s*<" "$f" 2>/dev/null)
  if [[ -n "$CONCRETE_COLLECTIONS" ]]; then
    echo "MEDIUM: $f - Concrete collection types (use List/Map/Set):"
    echo "$CONCRETE_COLLECTIONS" | head -3
  fi
  
  # Concrete types in method parameters
  CONCRETE_PARAMS=$(grep -n "public.*(\(ArrayList\|HashMap\|HashSet\)<" "$f" 2>/dev/null)
  if [[ -n "$CONCRETE_PARAMS" ]]; then
    echo "MEDIUM: $f - Concrete collection in parameters:"
    echo "$CONCRETE_PARAMS" | head -3
  fi
  
  # Service without interface (in engine-internal, should have interface in engine-core)
  if [[ "$f" == *"ServiceImpl.java" ]]; then
    INTERFACE_NAME=$(echo "$CLASS_NAME" | sed 's/Impl$//')
    if ! find . -name "${INTERFACE_NAME}.java" -path "*/engine-core/*" 2>/dev/null | grep -q .; then
      echo "MEDIUM: $f - No corresponding interface found in engine-core"
    fi
  fi
  
  # Check for framework imports in core domain classes
  if [[ "$f" == *"engine-core"* ]]; then
    FRAMEWORK_IMPORTS=$(grep -n "import\s\+\(io\.quarkus\|org\.springframework\|jakarta\.\)" "$f" 2>/dev/null | grep -v "jakarta.validation")
    if [[ -n "$FRAMEWORK_IMPORTS" ]]; then
      echo "HIGH: $f - Framework imports in core module (DIP violation):"
      echo "$FRAMEWORK_IMPORTS"
    fi
  fi
done

# Check for missing abstractions
echo ""
echo "=== Services without interfaces ==="
for f in $(find . -name "*Service.java" -path "*/src/main/*" ! -name "*Interface*" 2>/dev/null); do
  if grep -q "^public class" "$f"; then
    if ! grep -q "implements\s\+\w\+Service" "$f"; then
      echo "MEDIUM: $f - Service class without interface"
    fi
  fi
done
```

### Common DIP Violations
- **New in constructors**: Creating dependencies instead of injecting
- **Static utility calls**: `Utils.doSomething()` instead of injected service
- **Concrete return types**: Returning `ArrayList` instead of `List`
- **Framework in domain**: Spring/Quarkus annotations in core business logic

---

## Step 7: Generate Report

Create `solid-review.json`:

```json
{
  "meta": {
    "analyzedAt": "<ISO timestamp>",
    "path": "<analyzed path>",
    "mode": "full|changed",
    "filesAnalyzed": 0,
    "totalFindings": 0
  },
  "summary": {
    "srp": { "high": 0, "medium": 0, "low": 0 },
    "ocp": { "high": 0, "medium": 0, "low": 0 },
    "lsp": { "high": 0, "medium": 0, "low": 0 },
    "isp": { "high": 0, "medium": 0, "low": 0 },
    "dip": { "high": 0, "medium": 0, "low": 0 }
  },
  "findings": [
    {
      "id": "SRP-001",
      "principle": "SRP|OCP|LSP|ISP|DIP",
      "severity": "high|medium|low",
      "file": "<filepath>",
      "line": null,
      "indicator": "<what was detected>",
      "description": "<explanation>",
      "recommendation": "<how to fix>",
      "metrics": {
        "lines": null,
        "methods": null,
        "dependencies": null
      }
    }
  ],
  "hotspots": [
    {
      "file": "<filepath>",
      "violations": ["SRP", "DIP"],
      "totalFindings": 3,
      "recommendation": "Priority refactoring target"
    }
  ],
  "grade": {
    "overall": "A-F",
    "breakdown": {
      "srp": "A-F",
      "ocp": "A-F", 
      "lsp": "A-F",
      "isp": "A-F",
      "dip": "A-F"
    }
  },
  "recommendations": [
    {
      "priority": 1,
      "principle": "DIP",
      "action": "Extract interface for XService",
      "files": ["XService.java"],
      "effort": "small"
    }
  ]
}
```

---

## Step 8: Present Summary

```
## SOLID Review Summary

**Path:** <analyzed path>
**Files Analyzed:** <count>
**Grade:** <overall grade>

### Findings by Principle

| Principle | High | Medium | Low | Grade |
|-----------|------|--------|-----|-------|
| Single Responsibility | 0 | 0 | 0 | A |
| Open/Closed | 0 | 0 | 0 | A |
| Liskov Substitution | 0 | 0 | 0 | A |
| Interface Segregation | 0 | 0 | 0 | A |
| Dependency Inversion | 0 | 0 | 0 | A |

### Hotspots (Files with Multiple Violations)
1. **SomeService.java** - 3 violations (SRP, DIP) - Priority refactor
2. **AnotherClass.java** - 2 violations (OCP, ISP)

### Top Recommendations
1. [DIP] Extract interface for PaymentService
2. [SRP] Split UserService into UserAuthService and UserProfileService
3. [OCP] Replace type switch in OrderProcessor with strategy pattern

### Detailed Findings
<expandable list of all findings>
```

---

## Grading Rubric

| Grade | Criteria |
|-------|----------|
| **A** | No high findings, ≤5 medium findings |
| **B** | No high findings, ≤10 medium findings |
| **C** | ≤2 high findings, ≤15 medium findings |
| **D** | ≤5 high findings, any medium findings |
| **F** | >5 high findings |

---

## Refactoring Patterns

### SRP Violations
- **Extract Class**: Move related methods to new class
- **Extract Module**: Split large classes into modules
- **Facade Pattern**: Hide complexity behind simple interface

### OCP Violations
- **Strategy Pattern**: Replace type switches with pluggable strategies
- **Factory Pattern**: Encapsulate object creation
- **Plugin Architecture**: Allow extension without modification

### LSP Violations
- **Composition over Inheritance**: Replace inheritance with delegation
- **Interface extraction**: Create smaller, focused interfaces
- **Template Method**: Define skeleton in base, let subclasses fill in

### ISP Violations
- **Interface Splitting**: Break large interface into role interfaces
- **Adapter Pattern**: Adapt fat interface to slim one
- **Default Methods**: Provide sensible defaults (use sparingly)

### DIP Violations
- **Dependency Injection**: Inject abstractions via constructor
- **Interface Extraction**: Create interface for concrete dependency
- **Factory/Provider**: Abstract object creation
