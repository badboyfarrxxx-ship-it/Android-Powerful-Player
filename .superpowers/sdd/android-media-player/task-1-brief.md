# Task 1: Local Media Infrastructure

**Files:**
- Create: `data/local/MediaEntity.kt`, `data/local/MediaDatabase.kt`, `data/local/MediaDao.kt`, `data/repository/MediaRepository.kt`
- Test: `tests/data/MediaRepositoryTest.kt`

**Interfaces:**
- Produces: `MediaRepository.getMediaFiles(): Flow<List<MediaEntity>>`

**Steps:**
1. Implement Room Entity and DAO.
2. Implement MediaRepository scanner using MediaStore API and persistableUriPermission logic for Android 11+.
3. Write failing test for file discovery.
4. Verify scan results in mock directory.
5. Commit.

**Global Constraints (from Spec):**
- Target Platform: Android (API 21+).
- Storage: Must use Scoped Storage / MediaStore API for Android 11+ compatibility.
- Memory: C++ memory must be explicitly managed; no leaks in JNI buffers.
- UI: Responsive gesture-based overlay for volume, brightness, and seeking.
- Audio: High-fidelity 5-band EQ and loudness normalization.
