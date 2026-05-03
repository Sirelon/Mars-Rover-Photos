# WASM Web Support - Analysis and Requirements

## Current Status: WASM Disabled

WASM/Web target has been **temporarily disabled** due to architectural incompatibilities with Room Database.

## The Challenge

### Room Database Incompatibility
- **Room 2.8.4** supports: Android ✅, iOS ✅, Desktop/JVM ✅, **WASM ❌**
- Room provides no WASM artifacts, making it impossible to compile database code for web targets
- Database entities (MarsImage, Rover, FactDisplay) use Room annotations throughout the codebase

### Scope of Impact
The following components depend on Room database entities:
- **Data Layer**: DAOs, Database, Repositories (15+ files)
- **Domain Layer**: Repository interfaces, Domain models (8+ files)
- **Network Layer**: API mappers, Response models (3+ files)
- **Presentation Layer**: ViewModels, UI models, Grid transformers (10+ files)
- **DI Layer**: DatabaseModule, RepositoryModule (2 files)
- **Paging**: RemoteMediators (2 files)

**Total affected files**: ~40 files would need refactoring

## Solutions for WASM Web Support

### Option A: Dual Storage Strategy (Recommended)
**Effort**: 2-3 days | **Complexity**: Medium

**Approach**:
1. Create storage abstraction layer using `expect/actual`
2. Implement Room storage for Android/iOS/Desktop
3. Implement IndexedDB or in-memory storage for WASM
4. Share business logic, separate persistence

**Pros**:
- Clean separation of concerns
- Native storage for each platform
- Maintains all existing features

**Cons**:
- Requires refactoring repository layer
- Two storage implementations to maintain
- WASM features may differ (e.g., limited offline support)

**Structure**:
```
commonMain/
  ├── domain/repositories/     (interfaces - unchanged)
  └── domain/models/            (data classes - remove Room annotations)

roomSupportedMain/ (Android, iOS, Desktop)
  ├── data/database/            (Room implementations)
  └── data/repositories/        (Room-based repos)

wasmJsMain/
  ├── data/storage/             (IndexedDB or in-memory)
  └── data/repositories/        (Web storage repos)
```

### Option B: Full Abstraction with expect/actual
**Effort**: 3-4 days | **Complexity**: High

**Approach**:
1. Define `expect` interfaces for all database operations in commonMain
2. Implement `actual` Room-based classes in Android/iOS/Desktop
3. Implement `actual` web storage classes in WASM
4. Refactor all repository/ViewModel code to use abstractions

**Pros**:
- Most "pure KMP" approach
- Complete platform independence
- Maximum flexibility

**Cons**:
- Significant refactoring required
- Large amount of boilerplate code
- Risk of breaking existing functionality

### Option C: Simplified Web Version
**Effort**: 1-2 days | **Complexity**: Low

**Approach**:
1. Keep existing codebase for Android/iOS/Desktop
2. Create minimal WASM app with:
   - API calls only (no local storage)
   - Session-based state management
   - Limited feature set

**Pros**:
- Minimal changes to existing code
- Fast implementation
- Clear separation of platforms

**Cons**:
- WASM version has fewer features
- No offline support
- Duplicate UI code needed

### Option D: Wait for Room WASM Support
**Effort**: 0 days | **Complexity**: None

**Approach**:
- Monitor Room roadmap for WASM support
- Delay web version until Room supports WASM
- Focus on other platforms

**Pros**:
- No refactoring needed
- Consistent architecture across all platforms

**Cons**:
- Unknown timeline (Room WASM may never happen)
- Delays web platform delivery
- No control over implementation

## Recommended Path Forward

**Phase 1: Decide on Strategy**
- Review options above
- Choose based on timeline and requirements
- Consider WASM feature requirements

**Phase 2: If proceeding with Option A** (recommended):
1. Create storage abstraction layer
   - `expect interface ImageStorage`
   - `expect interface RoverStorage`
   - `expect interface FactStorage`
2. Move Room entities to separate module
3. Implement `actual` Room storage (reuse existing code)
4. Implement `actual` IndexedDB storage for WASM
5. Refactor repositories to use storage abstractions
6. Update DI modules for multi-storage
7. Test all platforms
8. Enable WASM target

**Estimated Timeline**: 2-3 days of focused work

## Current Platform Support

| Platform | Status | Database | Notes |
|----------|--------|----------|-------|
| Android | ✅ Working | Room 2.8.4 | Full features |
| iOS | ✅ Working | Room 2.8.4 | Full features |
| Desktop | ✅ Working | Room 2.8.4 | Full features |
| Web/WASM | ❌ Disabled | N/A | Requires refactoring |

## Files to Review for WASM Work

If proceeding with refactoring:

**Critical** (must change):
- `shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/data/database/` (all files)
- `shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/data/repositories/` (all files)
- `shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/di/DatabaseModule.kt`

**Important** (likely to change):
- `shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/domain/models/` (remove Room annotations)
- `shared/src/commonMain/kotlin/com/sirelon/marsroverphotos/data/network/Mappers.kt`

**May need updating**:
- All ViewModels (if storage interfaces change)
- FirebasePhotos implementations
- Image operations

## Next Steps

1. **Decision needed**: Choose WASM strategy (A, B, C, or D)
2. If proceeding:
   - Create detailed implementation plan
   - Set up feature flags for WASM-specific behavior
   - Begin refactoring with Option A as recommended approach
3. If not proceeding:
   - Keep WASM disabled
   - Focus on other platform improvements
   - Revisit when Room WASM support is available

## Related Changes This Session

- ✅ Fixed root `build.gradle.kts` clean task conflict (was blocking WASM plugin)
- ✅ Investigated Room WASM compatibility
- ✅ Created hierarchical source set structure (reverted)
- ✅ Documented WASM limitations and requirements
- ❌ WASM target disabled temporarily (see line 36-44 in `shared/build.gradle.kts`)

---

**Generated**: 2025-12-31
**Project**: Mars Rover Photos KMP
**Kotlin**: 2.3.0
**Room**: 2.8.4 (no WASM support)
