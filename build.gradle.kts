// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    configurations.matching { it.name == "classpath" }.all {
        // Force patched commons-io due AGP 8.5.2 transitive dependency vulnerability.
        resolutionStrategy.force("commons-io:commons-io:2.14.0")
        // Force patched protobuf-java due AGP/UTP transitive dependency vulnerability.
        resolutionStrategy.force("com.google.protobuf:protobuf-java:3.25.5")
        // Force patched jdom2 due AGP transitive dependency vulnerability.
        resolutionStrategy.force("org.jdom:jdom2:2.0.6.1")
        // Force patched netty-codec due AGP/UTP transitive dependency vulnerability.
        resolutionStrategy.force("io.netty:netty-codec:4.1.129.Final")
        // Force patched netty-codec-http due AGP/UTP transitive dependency vulnerability.
        resolutionStrategy.force("io.netty:netty-codec-http:4.1.129.Final")
        // Force patched netty-codec-http2 due AGP/UTP transitive dependency vulnerability.
        resolutionStrategy.force("io.netty:netty-codec-http2:4.1.129.Final")
        // Force patched netty-handler due AGP/UTP transitive dependency vulnerability.
        resolutionStrategy.force("io.netty:netty-handler:4.1.129.Final")
        // Force patched jose4j due AGP transitive dependency vulnerability.
        resolutionStrategy.force("org.bitbucket.b_c:jose4j:0.9.6")
        // Force patched commons-compress due AGP transitive dependency vulnerability.
        resolutionStrategy.force("org.apache.commons:commons-compress:1.26.0")
        // Force patched commons-lang3 due AGP transitive dependency vulnerability.
        resolutionStrategy.force("org.apache.commons:commons-lang3:3.18.0")
        // Force patched bouncycastle modules due AGP transitive dependency vulnerabilities.
        resolutionStrategy.force("org.bouncycastle:bcpkix-jdk18on:1.79")
        resolutionStrategy.force("org.bouncycastle:bcprov-jdk18on:1.79")
        resolutionStrategy.force("org.bouncycastle:bcutil-jdk18on:1.79")
    }
}

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
}
