rootProject.name = "easytpa"

// Composite build — use local PurrCore if present (for `depend: [PurrCore]`)
// CI will checkout PurrCore to ../PurrCore; local dev also expects it there
if (file("../PurrCore").exists()) {
    includeBuild("../PurrCore")
} else if (file("PurrCore").exists()) {
    includeBuild("PurrCore")
}
