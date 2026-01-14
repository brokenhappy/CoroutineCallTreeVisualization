package com.woutwerkman.calltreevisualizer.publisher

import java.io.File
import kotlin.system.exitProcess

private fun printHelp() {
    println("""
        Coroutine Call Tree Visualization - Publisher

        USAGE:
            publisher <version> [OPTIONS]

        ARGUMENTS:
            <version>       Version to publish (e.g., 0.0.1)

        OPTIONS:
            --no-dry-run    Actually publish (by default, uses --dry-run)
            -h, --help      Show this help message

        DESCRIPTION:
            This script automates the publishing process for the project:

            1. Finds all compiler plugin version branches (pattern: <version>-X.Y.Z)
            2. Tests each version branch
            3. Publishes all modules from master
            4. Publishes gradle-plugin and compiler-plugin from each version branch

        EXAMPLES:
            publisher 0.0.1                    # Dry-run for version 0.0.1
            publisher 0.0.1 --no-dry-run       # Actually publish version 0.0.1

        NOTES:
            - Always runs in dry-run mode by default for safety
            - Requires confirmation before live publishing
            - Must be run from the project root directory
    """.trimIndent())
}

/**
 * Publishing script for Coroutine Call Tree Visualization project.
 *
 * This script:
 * 1. Checks out all compiler plugin version branches for a given version
 * 2. Runs tests on each branch
 * 3. Publishes from master
 * 4. Publishes gradle-plugin and compiler-plugin modules from each version branch
 */
fun main(args: Array<String>) {
    if ("-h" in args || "--help" in args) {
        printHelp()
        exitProcess(0)
    }

    val version = args.getOrNull(0)
    val isDryRun = "--no-dry-run" !in args

    if (version == null || version.startsWith("-")) {
        System.err.println("Error: Version argument is required\n")
        printHelp()
        exitProcess(1)
    }

    if (isDryRun) {
        println("\n⚠️  Running in DRY-RUN mode (no actual publishing will occur)")
    } else {
        println("""
            
            ⚠️ Running in LIVE mode (actual publishing will occur)
            Are you sure you want to continue? (yes/no): 
        """.trimIndent())
        if (readlnOrNull()?.lowercase() != "yes") {
            println("Aborted.")
            exitProcess(0)
        }
    }

    run(version, isDryRun, projectRoot = File(".").canonicalFile)
}

fun run(version: String, isDryRun: Boolean, projectRoot: File) {
    // Step 1: Find all version branches
    println("\n→ Finding version branches for $version...")
    val versionBranches = getVersionBranches(version, projectRoot)

    if (versionBranches.isEmpty()) {
        System.err.println("✗ No version branches found matching pattern $version-X.Y.Z")
        exitProcess(1)
    }

    val branchList = versionBranches.joinToString("\n") { "  - $it" }
    println("""
        ✓ Found ${versionBranches.size} version branch(es):
        $branchList
    """.trimIndent())

    // Step 2: Test all version branches
    println("""

        ${"=".repeat(60)}
        PHASE 1: Testing all version branches
        ${"=".repeat(60)}
    """.trimIndent())

    versionBranches.forEach { branch ->
        checkoutBranch(branch, projectRoot)
        runTests(projectRoot)
        println("✓ Tests passed for $branch")
    }

    // Step 3: Publish from master
    println("""

        ${"=".repeat(60)}
        PHASE 2: Publishing from master
        ${"=".repeat(60)}
    """.trimIndent())

    checkoutBranch("master", projectRoot)
    publishAll(isDryRun, projectRoot)
    println("✓ Master published successfully")

    // Step 4: Publish gradle-plugin and compiler-plugin from version branches
    println("""

        ${"=".repeat(60)}
        PHASE 3: Publishing gradle-plugin and compiler-plugin from version branches
        ${"=".repeat(60)}
    """.trimIndent())

    versionBranches.forEach { branch ->
        checkoutBranch(branch, projectRoot)
        publishSpecificModules(isDryRun, projectRoot)
        println("✓ Published gradle-plugin and compiler-plugin for $branch")
    }

    // Return to original branch
    checkoutBranch(versionBranches.firstOrNull() ?: "master", projectRoot)

    val dryRunNote = if (isDryRun) "\n\nℹ️  This was a dry-run. Re-run with --no-dry-run to actually publish." else ""
    println("""

        ${"=".repeat(60)}
        ✓ All publishing tasks completed successfully!
        ${"=".repeat(60)}$dryRunNote
    """.trimIndent())
}

private fun getVersionBranches(version: String, projectRoot: File): List<String> {
    val result = runCommand(listOf("git", "branch", "-a"), projectRoot)
    val branches = result.split("\n").map { it.trim() }

    // Pattern: version-X.Y.Z (e.g., 0.0.1-2.1.0, 0.0.1-2.2.20)
    val pattern = Regex("^\\*?\\s*($version-\\d+\\.\\d+\\.\\d+)$")

    return branches.mapNotNull { branch ->
        pattern.matchEntire(branch)?.groupValues?.get(1)
    }.sorted()
}

private fun checkoutBranch(branch: String, projectRoot: File) {
    println("""

        ${"=".repeat(60)}
        Checking out branch: $branch
        ${"=".repeat(60)}
    """.trimIndent())
    runCommand(listOf("git", "checkout", branch), projectRoot)
}

private fun runTests(projectRoot: File) {
    println("\n→ Running tests...")
    runCommand(listOf("./gradlew", "test"), projectRoot)
}

private fun publishAll(isDryRun: Boolean, projectRoot: File) {
    println("\n→ Publishing all modules from master...")
    val cmd = mutableListOf("./gradlew", "publish")
    if (isDryRun) cmd.add("--dry-run")
    runCommand(cmd, projectRoot)
}

private fun publishSpecificModules(isDryRun: Boolean, projectRoot: File) {
    println("\n→ Publishing gradle-plugin and compiler-plugin modules...")
    val cmd = mutableListOf(
        "./gradlew",
        ":gradle-plugin:publish", // TODO: Decouple gradle-plugin from compiler-plugin version, such that we only need to release it once per version
        ":compiler-plugin:publish",
    )
    if (isDryRun) cmd.add("--dry-run")
    runCommand(cmd, projectRoot)
}

private fun runCommand(command: List<String>, workingDirectory: File): String {
    println("\n→ Running: ${command.joinToString(" ")}")

    val process = ProcessBuilder(command)
        .directory(workingDirectory)
        .redirectErrorStream(true)
        .start()

    val output = process.inputStream.bufferedReader().use { it.readText() }
    val exitCode = process.waitFor()

    if (output.isNotEmpty()) {
        println(output)
    }

    if (exitCode != 0) {
        System.err.println("✗ Command failed with exit code $exitCode")
        exitProcess(1)
    }

    return output
}
