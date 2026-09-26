package com.johnchourp.learnbyzantinemusic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every release is signed by the release key, and every path that publishes one checks it first.
 *
 * Android updates an installed app only when the new APK carries the same signing certificate.
 * v1.17.0 was signed by a machine-local upload key instead, so no v1.16.0 install could update, and
 * uninstalling lost the user's data: Google's Auto Backup rejects a backup made under another
 * certificate and Android then clears the app (measured on a phone, 2026-09-26). Both keys had the same
 * DN, which is why "apksigner → CN=LearnByzantineMusic" proved nothing.
 *
 * `scripts/check-release-signer.sh` compares the certificate's SHA-256 with a pin (its branches are
 * tested by `scripts/tests/test_check_release_signer.sh`). This test keeps the pin and its callers:
 * - the pin is the certificate that signed v1.16.0 — changing it strands every installed copy;
 * - `release-and-tag.sh` checks the keystore before the version bump, and the APK after the build and
 *   before the commit and the tag;
 * - the tag workflow checks the APK before either publish step;
 * - `setup-release-signing.sh` checks the keystore before it overwrites the GitHub secrets.
 *
 * Lines are matched whole and comments are dropped first, so neither a commented-out call nor a
 * renamed one (`check-release-signer.sh-off`) counts.
 */
class ReleaseSignerPinTest {

    /** The code lines of a repository file, trimmed, without comment lines. */
    private fun code(path: String): List<String> {
        val file = listOf(File(path), File("../$path")).firstOrNull { it.isFile }
            ?: error("$path not found from ${File("").absolutePath}")
        return file.readLines().map { it.trim() }.filterNot { it.isEmpty() || it.startsWith("#") }
    }

    /** Indices of the lines equal to [line] — or, with [prefix], starting with it. */
    private fun find(lines: List<String>, line: String, prefix: Boolean = false): List<Int> =
        lines.indices.filter { if (prefix) lines[it].startsWith(line) else lines[it] == line }

    private fun single(lines: List<String>, line: String): Int {
        val hits = find(lines, line)
        assertEquals("expected exactly one line `$line`, found at $hits", 1, hits.size)
        return hits.single()
    }

    @Test
    fun thePinIsTheCertificateThatSignedV1_16_0() {
        single(code(CHECK), "EXPECTED_CERT_SHA256=\"$RELEASE_CERT_SHA256\"")
    }

    @Test
    fun theReleaseScriptChecksTheKeystoreBeforeTheBumpAndTheApkBeforeCommitAndTag() {
        val lines = code("scripts/release-and-tag.sh")
        single(lines, "SIGNER_CHECK_SCRIPT=\"\$SCRIPT_DIR/check-release-signer.sh\"")
        val keystoreCheck = single(
            lines,
            "\"\$BASH\" \"\$SIGNER_CHECK_SCRIPT\" --keystore \"\$ANDROID_SIGNING_STORE_FILE\" \"\$ANDROID_SIGNING_KEY_ALIAS\"",
        )
        val bump = single(lines, "mapfile -t bump_output < <(\"\$BUMP_SCRIPT\" \"\${BUMP_ARGS[@]}\")")
        val build = single(lines, "./gradlew clean assembleRelease bundleRelease")
        val apkCheck = single(lines, "\"\$BASH\" \"\$SIGNER_CHECK_SCRIPT\" \"\$APK_PATH\"")
        val commit = single(lines, "git commit -m \"\$COMMIT_MESSAGE\"")
        val tag = single(lines, "git tag -a \"\$TAG\" -m \"Release \$TAG\"")

        assertTrue("the keystore is checked before the version bump", keystoreCheck < bump)
        assertTrue("the APK is checked after the build", build < apkCheck)
        assertTrue("the APK is checked before the commit and the tag", apkCheck < commit && apkCheck < tag)
    }

    @Test
    fun theTagWorkflowChecksTheApkBeforeEitherPublishStep() {
        val lines = code(".github/workflows/android-release.yml")
        val found = single(lines, "APK_PATH=\"\$(find app/build/outputs/apk/release -maxdepth 1 -type f -name '*.apk' ! -name '*-unsigned.apk' | sort | tail -n1 || true)\"")
        val check = single(lines, "./scripts/check-release-signer.sh \"\$APK_PATH\"")
        val publishes = find(lines, "uses: softprops/action-gh-release@", prefix = true)

        assertEquals("both publish steps (notes and fallback notes)", 2, publishes.size)
        assertTrue("the APK is checked after it is found", found < check)
        assertTrue("the APK is checked before every publish step", publishes.all { check < it })
    }

    @Test
    fun theSetupScriptChecksTheKeystoreBeforeItOverwritesTheGitHubSecrets() {
        val lines = code("scripts/setup-release-signing.sh")
        val check = single(
            lines,
            "ANDROID_SIGNING_STORE_PASSWORD=\"\$STORE_PASSWORD\" \"\$BASH\" \"\$SCRIPT_DIR/check-release-signer.sh\" --keystore \"\$KEYSTORE_PATH\" \"\$KEY_ALIAS\"",
        )
        val secrets = find(lines, "gh secret set ", prefix = true)

        assertEquals("the four signing secrets", 4, secrets.size)
        assertTrue("the keystore is checked before every secret is written", secrets.all { check < it })
    }

    private companion object {
        const val CHECK = "scripts/check-release-signer.sh"

        /** `apksigner verify --print-certs` of the published v1.16.0 `apk-release.apk`, 2026-09-26. */
        const val RELEASE_CERT_SHA256 = "f14d6379ddc512cc1b05de57553144c9ae373ef7e09edeb8256b4191bb4537de"
    }
}
