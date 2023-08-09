import java.net.URI

plugins {
	id("fabric-loom") version "1.3-SNAPSHOT"
	id("maven-publish")
	id("org.ajoberstar.grgit") version "5.2.0"
}

val grgit = 

val archivesBaseName = "${project.archives_base_name}+mc${libs.versions.minecraft.get()}"
version = getVersion()
group = project.maven_group

repositories {
	maven { url = uri("https://api.modrinth.com/maven") }
	maven { url = uri("https://maven.terraformersmc.com/") }
}

dependencies {
	"minecraft"("libs.minecraft")

	"mappings"("loom.officialMojangMappings()")
	"modImplementation"("libs.fabric.loader")

	"modImplementation"("libs.fabric.api")
	"modImplementation"("libs.bundles.dependencies")
	"modLocalRuntime"("libs.bundles.dev.mods")
}

task("processResources") {
	inputs.property("version", project.version)

	filesMatching("fabric.mod.json") {
		expand "version": project.version
	}
}

tasks.withType(JavaCompile).configureEach {
	it.options.release = 17
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

jar {
	from("LICENSE") {
		rename { "${it}_${project.base.archivesName.get()}"}
	}
}

task("buildOrPublish") {
	group = "build"
	var mavenUser = System.getenv().get("MAVEN_USER")
	if (!mavenUser.isNullOrEmpty()) {
		dependsOn(tasks.getByName("publish"))
		println("prepared for publish")
	} else {
		dependsOn(tasks.getByName("build"))
		println("prepared for build")
	}
}

// TODO: Uncomment for a non template mod!
publishing {
	publications {
		mavenJava(MavenPublication) {
			groupId = project.maven_group
			artifactId = project.archives_base_name
			version = "${project.mod_version}-rev.${grgit.head().abbreviatedId}"

			from(components.get("java"))
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		maven {
			url = uri("https://mvn.devos.one/${System.getenv()["PUBLISH_SUFFIX"]}/")
			credentials {
				username = System.getenv()["MAVEN_USER"]
				password = System.getenv()["MAVEN_PASS"]
			}
			authentication { basic(BasicAuthentication) }
		}
	}
}

fun getVersion(): String {
	val mod_version = "project.mod_version"
	val build_id = System.getenv("GITHUB_RUN_NUMBER")

	// CI builds only
	if (build_id != null) {
		return "${mod_version}+build.${build_id}"
	}

	if (grgit != null) {
		val head = grgit.head()
		val id = head.abbreviatedId

		// Flag the build if the build tree is not clean
		if (!grgit.status().clean) {
			id += "-dirty"
		}

		return "${mod_version}+rev.${id}"
	}

	// No tracking information could be found about the build
	return "${mod_version}+unknown"

}