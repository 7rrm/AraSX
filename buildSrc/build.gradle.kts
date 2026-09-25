plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        // Nagram 12.10.2+: generates org.telegram.localization.LocalizationUtils,
        // which org.telegram.messenger.LocaleController imports directly.
        register("telegramBuildPlugin") {
            id = "org.telegram.build-plugin"
            implementationClass = "org.telegram.plugin.TelegramBuildPlugin"
        }
        // Nagram 12.10.2+: builds the localization_*.bin assets that
        // LocalizationUtils/LocaleController load, the lottie_meta.bin asset read
        // by ResLottieMeta, the string-resource-id asset and the aapt stable-ids file.
        register("telegramBuildAppPlugin") {
            id = "org.telegram.build-app-plugin"
            implementationClass = "org.telegram.plugin.TelegramBuildAppPlugin"
        }
    }
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    // MeeroX keeps AGP 9.3.1 (see rule: keep our build) - Nagram 12.10.4 uses 8.13.2.
    implementation("com.android.tools.build:gradle:9.3.1")
    implementation("com.google.code.gson:gson:2.14.0")
}
