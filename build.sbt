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
  version := "0.3.0-SNAPSHOT",
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
    .disablePlugins(AssemblyPlugin)
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
    .disablePlugins(AssemblyPlugin)
    .settings(commonSettings: _*)
    .settings(
      name += "-json-serializer",
      publishArtifact := true
    )

lazy val jsonSerializerJvm = jsonSerializer.jvm
lazy val jsonSerializerJs = jsonSerializer.js

lazy val upickleJsonSerializer = (crossProject in file("upickle-json-serializer"))
    .disablePlugins(AssemblyPlugin)
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

// Examples

lazy val JettyVersion = "9.+"

lazy val exampleCommonSettings = Seq(
  name += "-example",
  libraryDependencies ++= Seq(
    "com.softwaremill.macwire" %% "macros" % "2.+"
  ),
  publishArtifact := false
)

lazy val exampleJvmCommonSettings = Seq(
  pipelineStages in Assets := Seq(scalaJSDev),
  WebKeys.packagePrefix in Assets := "public/",
  managedClasspath in Runtime += (packageBin in Assets).value,
  libraryDependencies ++= Seq(
    "org.eclipse.jetty" % "jetty-webapp" % JettyVersion,

    "org.scalatra" %% "scalatra" % "2.5.+"
  )
)

lazy val exampleJsCommonSettings = Seq(
  persistLauncher := true,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.+"
  )
)

lazy val exampleE2e = (crossProject in file("examples/e2e"))
    .settings(commonSettings: _*)
    .settings(exampleCommonSettings: _*)
    .settings(
      name += "-e2e"
    )
    .dependsOn(core, upickleJsonSerializer)

lazy val exampleE2eJvm = exampleE2e.jvm
    .enablePlugins(SbtWeb)
    .settings(exampleJvmCommonSettings: _*)
    .settings(
      scalaJSProjects := Seq(exampleE2eJs),
      unmanagedResourceDirectories in Assets ++= Seq(
        (baseDirectory in exampleE2eJs).value / "src" / "main" / "public"
      ),
      mainClass := Option("io.github.shogowada.scala.jsonrpc.example.e2e.Main")
    )
lazy val exampleE2eJs = exampleE2e.js
    .enablePlugins(ScalaJSPlugin)
    .disablePlugins(AssemblyPlugin)
    .settings(exampleJsCommonSettings: _*)

lazy val exampleE2eWebSocket = (crossProject in file("examples/e2eWebSocket"))
    .settings(commonSettings: _*)
    .settings(exampleCommonSettings: _*)
    .settings(
      name += "-e2e-websocket"
    )
    .dependsOn(core, upickleJsonSerializer)

lazy val exampleE2eWebSocketJvm = exampleE2eWebSocket.jvm
    .enablePlugins(SbtWeb)
    .settings(exampleJvmCommonSettings: _*)
    .settings(
      scalaJSProjects := Seq(exampleE2eWebSocketJs),
      unmanagedResourceDirectories in Assets ++= Seq(
        (baseDirectory in exampleE2eWebSocketJs).value / "src" / "main" / "public"
      ),
      libraryDependencies ++= Seq(
        "org.eclipse.jetty.websocket" % "websocket-api" % JettyVersion,
        "org.eclipse.jetty.websocket" % "websocket-server" % JettyVersion
      )
    )
lazy val exampleE2eWebSocketJs = exampleE2eWebSocket.js
    .enablePlugins(ScalaJSPlugin)
    .disablePlugins(AssemblyPlugin)
    .settings(exampleJsCommonSettings: _*)
