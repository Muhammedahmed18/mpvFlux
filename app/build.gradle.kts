import com.android.build.api.artifact.ArtifactTransformationRequest
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.BuiltArtifact
import com.android.build.api.variant.FilterConfiguration
import javax.inject.Inject
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose.compiler)
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.room)
}

android {
  namespace = "app.marlboroadvance.mpvex"
  compileSdk = 36

  defaultConfig {
    applicationId = "app.marlboroadvance.mpvflux"
    minSdk = 26
    targetSdk = 36
    versionCode = 143
    versionName = "1.4.3"

    vectorDrawables {
      useSupportLibrary = true
    }

    buildConfigField("String", "GIT_SHA", "\"${getCommitSha()}\"")
    buildConfigField("int", "GIT_COUNT", getCommitCount())
  }

  flavorDimensions += "distribution"

  productFlavors {
    create("standard") {
      dimension = "distribution"
      buildConfigField("boolean", "SCOPED_STORAGE_ONLY", "false")
    }

    create("playstore") {
      dimension = "distribution"
      versionNameSuffix = "-playstore"
      buildConfigField("boolean", "SCOPED_STORAGE_ONLY", "true")
    }

    create("fdroid") {
      dimension = "distribution"
      versionNameSuffix = "-fdroid"
      buildConfigField("boolean", "SCOPED_STORAGE_ONLY", "false")

      ndk {
        abiFilters += listOf("arm64-v8a", "x86_64")
      }
    }
  }

  dependenciesInfo {
    includeInApk = false
    includeInBundle = false
  }

  splits {
    abi {
      isEnable = true
      reset()
      include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
      isUniversalApk = true
    }
  }

  buildTypes {
    named("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
      ndk {
        debugSymbolLevel = "none"
      }
    }

    create("preview") {
      initWith(getByName("release"))
      signingConfig = null
      applicationIdSuffix = ".preview"
      versionNameSuffix = "-${getCommitCount()}"
    }

    named("debug") {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-${getCommitCount()}"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    viewBinding = true
    buildConfig = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "META-INF/DEPENDENCIES"
      excludes += "META-INF/LICENSE*"
      excludes += "META-INF/NOTICE*"
      excludes += "META-INF/*.kotlin_module"
      excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
    }
    jniLibs {
      useLegacyPackaging = true
    }
  }

  @Suppress("UnstableApiUsage")
  androidResources {
    generateLocaleConfig = true
  }
}

androidComponents {
  val abiCodes = mapOf(
    "armeabi-v7a" to 1,
    "arm64-v8a" to 2,
    "x86" to 3,
    "x86_64" to 4
  )

  onVariants { variant ->
    variant.outputs.forEach { output ->
      val abi = output.filters
        .find { it.filterType == FilterConfiguration.FilterType.ABI }
        ?.identifier

      output.versionCode.set(
        (output.versionCode.orNull ?: 0) * 10 + (abiCodes[abi] ?: 0)
      )
    }

    // Local release builds: the final APKs themselves are named mpvFlux-<abi>-v<version>.apk
    // (one set of files, in app/build/outputs/apk/<flavor>/release/ or the folder picked in
    // "Generate Signed APK"). Uses AGP 9's artifact transform API, since outputFileName no
    // longer exists. Skipped on CI (CI=true), where the workflows sign and rename by the
    // original app-<flavor>-<abi>-release names.
    if (variant.buildType == "release" && System.getenv("CI") == null) {
      val renameTask = tasks.register<RenameApksTask>("renameApksFor${variant.name.replaceFirstChar { it.uppercase() }}") {
        flavor.set(variant.flavorName ?: "")
      }
      val request = variant.artifacts.use(renameTask)
        .wiredWithDirectories(RenameApksTask::input, RenameApksTask::output)
        .toTransformMany(SingleArtifact.APK)
      renameTask.configure { transformationRequest.set(request) }
    }
  }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll(
      "-Xwhen-guards",
      "-Xcontext-parameters",
      "-Xannotation-default-target=param-property",
      "-opt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi",
      "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
      "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi"
    )
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

composeCompiler {
  includeSourceInformation = true

  // Phase 0 (recomposition audit): opt-in Compose compiler metrics + stability reports.
  // Enable with `-PcomposeCompilerReports=true`; output lands in
  // app/build/compose-compiler/{metrics,reports}. Off by default so normal builds are
  // unaffected. The *-classes.txt report lists which params are inferred unstable
  // (blocks skipping) and *-composables.txt shows skippable/restartable per composable.
  if (project.findProperty("composeCompilerReports") == "true") {
    val out = layout.buildDirectory.dir("compose-compiler")
    metricsDestination.set(out.map { it.dir("metrics") })
    reportsDestination.set(out.map { it.dir("reports") })
  }
}

room {
  schemaDirectory("$projectDir/schemas")
}

dependencies {
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.material3.android)
  implementation(libs.material)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.ui.tooling)
  implementation(libs.bundles.compose.navigation3)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.compose.constraintlayout)
  implementation(libs.androidx.preference.ktx)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.material3.icons.extended)
  implementation(libs.androidx.compose.animation.graphics)
  implementation(libs.mediasession)
  implementation(libs.androidx.documentfile)
  implementation(libs.saveable)

  implementation(platform(libs.koin.bom))
  implementation(libs.bundles.koin)

  implementation(libs.seeker)
  implementation(libs.compose.prefs)

  implementation(libs.accompanist.permissions)

  implementation(libs.room.runtime)
  ksp(libs.room.compiler)
  implementation(libs.room.ktx)

  implementation(libs.kotlinx.immutable.collections)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.okhttp)

  implementation(libs.truetype.parser)
  implementation(libs.fsaf)
  implementation(libs.mediainfo.lib)
  // mpv/libass/ffmpeg from Maven Central (was a 77.6 MB AAR in app/libs)
  implementation(libs.mpv.android)

  implementation(libs.lazycolumnscrollbar)
  implementation(libs.reorderable)
}

/* ---------------- Git helpers ---------------- */

/**
 * Renames each built APK to "mpvFlux-<abi>-v<version>.apk" (standard flavor) or
 * "mpvFlux-<flavor>-<abi>-v<version>.apk" (playstore / fdroid). Universal APKs use "universal".
 * Android Studio and the output-metadata.json see only the renamed files.
 * Pattern from Google's gradle-recipes "workerEnabledTransformation" (AGP 9.1).
 */
abstract class RenameApksTask @Inject constructor(
  private val workers: WorkerExecutor,
) : DefaultTask() {
  @get:InputFiles
  abstract val input: DirectoryProperty

  @get:OutputDirectory
  abstract val output: DirectoryProperty

  @get:Internal
  abstract val transformationRequest: Property<ArtifactTransformationRequest<RenameApksTask>>

  @get:Input
  abstract val flavor: Property<String>

  @TaskAction
  fun rename() {
    val flavorName = flavor.get()
    val prefix = if (flavorName.isEmpty() || flavorName == "standard") "mpvFlux" else "mpvFlux-$flavorName"

    // Remove APKs from earlier versions (e.g. mpvFlux-arm64-v8a-v1.3.5.apk) so the folder
    // only holds the current build. Gradle/Android Studio never delete them because the
    // version is part of the file name.
    output.get().asFile.listFiles { file ->
      file.name.startsWith("mpvFlux") && file.name.endsWith(".apk")
    }?.forEach { it.delete() }

    transformationRequest.get().submit(
      this,
      workers.noIsolation(),
      CopyApkWork::class.java,
    ) { apk: BuiltArtifact, outputLocation: Directory, params: CopyApkParams ->
      val abi = apk.filters
        .find { it.filterType == FilterConfiguration.FilterType.ABI }
        ?.identifier ?: "universal"
      // versionName carries the flavor suffix ("1.3.5-playstore"); strip it for the file name.
      val version = (apk.versionName ?: "").removeSuffix("-$flavorName")
      params.inputApkFile.set(File(apk.outputFile))
      params.outputApkFile.set(File(outputLocation.asFile, "$prefix-$abi-v$version.apk"))
      params.outputApkFile.get().asFile
    }
  }
}

interface CopyApkParams : WorkParameters, java.io.Serializable {
  val inputApkFile: RegularFileProperty
  val outputApkFile: RegularFileProperty
}

abstract class CopyApkWork @Inject constructor(
  private val params: CopyApkParams,
) : WorkAction<CopyApkParams> {
  override fun execute() {
    val target = params.outputApkFile.get().asFile
    target.delete()
    params.inputApkFile.get().asFile.copyTo(target)
  }
}

fun getCommitCount(): String =
  runCommand("git rev-list --count HEAD") ?: "0"

fun getCommitSha(): String =
  runCommand("git rev-parse --short HEAD") ?: "unknown"

fun runCommand(command: String): String? =
  try {
    val parts = command.split(' ')
    val process = ProcessBuilder(parts)
      .redirectErrorStream(true)
      .start()

    val output = process.inputStream
      .bufferedReader()
      .readText()
      .trim()

    process.waitFor()
    output.ifEmpty { null }
  } catch (_: Exception) {
    null
  }
