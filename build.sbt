crossScalaVersions := Seq("2.11.8", "2.12.1")

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  isSnapshot.value match {
    case true => Some("snapshots" at nexus + "content/repositories/snapshots")
    case false => Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
}
publishArtifact := false

val commonSettings = Seq(
  organization := "io.github.shogowada",
  name := "scala-json-rpc",
  version := "0.2.2-SNAPSHOT",
  scalaVersion := "2.12.1",
  licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
  homepage := Some(url("https://github.com/shogowada/scala-json-rpc")),
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    isSnapshot.value match {
      case true => Some("snapshots" at nexus + "content/repositories/snapshots")
      case false => Some("releases" at nexus + "service/local/staging/deploy/maven2")
    }
  },
  publishArtifact := false,
  pomExtra := <scm>
    <url>git@github.com:shogowada/scala-json-rpc.git</url>
    <connection>scm:git:git@github.com:shogowada/scala-json-rpc.git</connection>
  </scm>
      <developers>
        <developer>
          <id>shogowada</id>
          <name>Shogo Wada</name>
          <url>https://github.com/shogowada</url>
        </developer>
      </developers>
)

lazy val core = (crossProject in file("."))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,

        "org.scalatest" %%% "scalatest" % "3.+" % "test"
      ),
      publishArtifact := true
    )
    .dependsOn(jsonSerializer)
    .dependsOn(upickleJsonSerializer % "test")

lazy val jvm = core.jvm
lazy val js = core.js

lazy val jsonSerializer = (crossProject in file("json-serializer"))
    .settings(commonSettings: _*)
    .settings(
      name += "-json-serializer",
      publishArtifact := true
    )

lazy val jsonSerializerJvm = jsonSerializer.jvm
lazy val jsonSerializerJs = jsonSerializer.js

lazy val upickleJsonSerializer = (crossProject in file("upickle-json-serializer"))
    .settings(commonSettings: _*)
    .settings(
      name += "-upickle-json-serializer",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,

        "com.lihaoyi" %%% "upickle" % "0.4.+"
      ),
      publishArtifact := true
    )
    .dependsOn(jsonSerializer)

lazy val upickleJsonSerializerJvm = upickleJsonSerializer.jvm
lazy val upickleJsonSerializerJs = upickleJsonSerializer.js
