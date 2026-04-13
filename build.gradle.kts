plugins {
	kotlin("jvm") version "2.3.20"
	`maven-publish`
	java

	alias(libs.plugins.fabric.loom)
}

version = getModVersion()
group = "one.devos.nautical"

val fabricApiVersion = libs.fabric.api.get().version
val fabricLanguageKotlinVersion = libs.fabric.language.kotlin.get().version
val fabricLoaderVersion = libs.fabric.loader.get().version
val minecraftVersion = libs.minecraft.get().version
val javaVersion = rootProject.java.sourceCompatibility.majorVersion

repositories {
	maven("https://api.modrinth.com/maven")
	maven("https://maven.terraformersmc.com/releases")
	maven("https://maven.parchmentmc.org")
	maven("https://mvn.devos.one/snapshots")
}

//All dependencies and their versions are in ./gradle/libs.versions.toml
dependencies {

	minecraft(libs.minecraft)

	mappings(loom.layered {
		officialMojangMappings()
		parchment("org.parchmentmc.data:parchment-1.20.1:2023.09.03@zip")
	})

	modImplementation(libs.fabric.loader)
	modImplementation(libs.fabric.api)
	modImplementation(libs.fabric.language.kotlin)

	//Mods
	modImplementation(libs.bundles.dependencies)
	modLocalRuntime(libs.bundles.dev.mods)

	include(modImplementation("gay.asoji:fmw:1.0.0+build.8")!!) // just to avoid the basic long metadata calls
}

sourceSets {
	main {
		resources {
			srcDir("src/main/generated")
			exclude("src/main/generated/.cache")
		}
	}
}

loom {
	runs {
		register("datagen") {
			client()
			name("Data Generation")
			vmArgs(
				"-Dfabric-api.datagen",
				"-Dfabric-api.datagen.output-dir=${file("src/main/generated")}",
				"-Dfabric-api.datagen.modid=${project.name}"
			)
			runDir("build/datagen")
		}

		register("testModClient") {
			client()
			name("Test Mod Client")
			source(sourceSets.getByName("test"))
			runDir("run/test")
		}

		register("testModServer") {
			server()
			name("Test Mod Server")
			source(sourceSets.getByName("test"))
			runDir("run/test_server")
		}

		register("gametest") {
			server()
			name("Test")
			source(sourceSets.getByName("test"))
			vmArgs("-Dfabric-api.gametest")
			vmArgs("-Dfabric-api.gametest.report-file=${project.layout.buildDirectory}/junit.xml")
			runDir("run/gametest_server")
		}

		afterEvaluate {
			configureEach {
				vmArg("-javaagent:${configurations.compileClasspath.get().find { it.name.contains("sponge-mixin") }}")
				vmArg("-XX:+IgnoreUnrecognizedVMOptions") // in the case the below doesnt work bc that JVM doesnt have it
				vmArg("-XX:+AllowEnhancedClassRedefinition")
				property("mixin.hotSwap", "true")
				property("mixin.debug.export", "true")
			}
		}
	}
}

java {
	withSourcesJar()

	// SET YOUR IDE JAVA RUNTIME TO USE JAVA 21, THIS WILL STILL COMPILE FOR JAVA 17, BLAME LOOM.
	//
	// FILE [OR GEAR ICON] -> PROJECT STRUCTURE -> SDK -> SELECT THE DROPDOWN -> ADD SDK -> INSTALL JETBRAINS RUNTIME 21
	// FILE -> SETTINGS -> BUILD, EXECUTION, DEPLOYMENT -> BUILD TOOLS -> GRADLE -> MAKE SURE GRADLE JVM USES JBR-21
	toolchain.languageVersion = JavaLanguageVersion.of(17)
}

// Write the version to the fabric.mod.json
tasks.processResources {
	val properties: Map<String, Any> = mapOf(
		// mod vers
		"version" to project.version,

		// dependency vers
		"fabric_api" to ">=$fabricApiVersion",
		"fabric_language_kotlin" to ">=$fabricLanguageKotlinVersion",
		"fabric_loader" to ">=$fabricLoaderVersion",
		"java" to ">=$javaVersion",
		"minecraft" to "~$minecraftVersion",
	)

	inputs.properties(properties)

	filesMatching("fabric.mod.json") {
		expand(properties)
	}
}

tasks.jar {
	from("LICENSE") {
		rename { "${it}_${project.base.archivesName.get()}"}
	}
}

// TODO: Uncomment for a non template mod!
publishing {
//	publications {
//		create<MavenPublication>("mavenJava") {
//			from(components["java"])
//		}
//	}
//
//	repositories {
//		listOf("Releases", "Snapshots").forEach {
//			maven("https://mvn.devos.one/${it.lowercase()}") {
//				name = "devOS$it"
//				credentials(PasswordCredentials::class)
//			}
//		}
//	}
}

fun getModVersion(): String {
	val modVersion = project.property("mod_version").toString()
	val gitExitCode = providers.exec { commandLine("git", "--version"); isIgnoreExitValue = true }.result.get().exitValue

	if (gitExitCode == 0) { // 0 = git is installed, anything else, prob not.
		val buildId = providers.exec { commandLine("git", "rev-parse", "--short", "HEAD")}.standardOutput.asText.get().trim()
		val dirtyStateCmd = providers.exec { commandLine("git", "status", "--porcelain") }.standardOutput.asText.get().trim()

		fun dirtyStateText(): String {
			return if (dirtyStateCmd.isEmpty()) {
				""
			} else {
				"-dirty"
			}
		}

		return "$modVersion+rev.$buildId${dirtyStateText()}"
	} else {
		return "$modVersion+unknown"
	}
}