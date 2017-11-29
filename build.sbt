crossScalaVersions := Seq("2.12.2")

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
  version := "0.9.1",
  scalaVersion := "2.12.2",
  logBuffered in Test := false,
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

        "org.scalatest" %%% "scalatest" % "3.+" % Test
      ),
      publishArtifact := true
    )
    .dependsOn(jsonSerializer)
    .dependsOn(upickleJSONSerializer % "test")

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

lazy val upickleJSONSerializer = (crossProject in file("upickle-json-serializer"))
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

lazy val upickleJSONSerializerJvm = upickleJSONSerializer.jvm
lazy val upickleJSONSerializerJs = upickleJSONSerializer.js

// Examples

lazy val JettyVersion = "9.+"

lazy val exampleCommonSettings = Seq(
  name += "-example",
  publishArtifact := false
)

lazy val exampleJvmCommonSettings = Seq(
  pipelineStages in Assets := Seq(scalaJSDev),
  WebKeys.packagePrefix in Assets := "public/",
  managedClasspath in Runtime += (packageBin in Assets).value,
  libraryDependencies ++= Seq(
    "org.eclipse.jetty" % "jetty-webapp" % JettyVersion,
    "org.scalatra" %% "scalatra" % "2.5.+",

    "org.seleniumhq.selenium" % "selenium-java" % "[3.4.0,4.0.0[" % "it",
    "org.scalatest" %% "scalatest" % "3.+" % "it"
  )
)

lazy val exampleJsCommonSettings = Seq(
  libraryDependencies ++= Seq(
    "io.github.shogowada" %%% "scalajs-reactjs" % "0.11.+",
    "org.scala-js" %%% "scalajs-dom" % "0.9.+"
  )
)

// HTTP example

lazy val exampleE2e = (crossProject in file("examples/e2e"))
    .settings(commonSettings: _*)
    .settings(exampleCommonSettings: _*)
    .settings(
      name += "-e2e"
    )
    .dependsOn(core, upickleJSONSerializer)

lazy val exampleE2eJvm = exampleE2e.jvm
    .enablePlugins(SbtWeb, WebScalaJSBundlerPlugin)
    .configs(IntegrationTest)
    .settings(exampleJvmCommonSettings: _*)
    .settings(Defaults.itSettings: _*)
    .settings(
      scalaJSProjects := Seq(exampleE2eJs),
      unmanagedResourceDirectories in Assets ++= Seq(
        (baseDirectory in exampleE2eJs).value / "src" / "main" / "public"
      ),
      (fork in IntegrationTest) := true,
      (javaOptions in IntegrationTest) ++= Seq(
        s"-DjarLocation=${assembly.value}"
      )
    )
    .dependsOn(exampleTestUtils % "it")

lazy val exampleE2eJs = exampleE2e.js
    .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
    .disablePlugins(AssemblyPlugin)
    .settings(exampleJsCommonSettings: _*)
    .settings(
      scalaJSUseMainModuleInitializer := true
    )

// WebSocket example

lazy val exampleE2eWebSocket = (crossProject in file("examples/e2e-web-socket"))
    .settings(commonSettings: _*)
    .settings(exampleCommonSettings: _*)
    .settings(
      name += "-e2e-websocket"
    )
    .dependsOn(core, upickleJSONSerializer)

lazy val exampleE2eWebSocketJvm = exampleE2eWebSocket.jvm
    .enablePlugins(SbtWeb, WebScalaJSBundlerPlugin)
    .configs(IntegrationTest)
    .settings(exampleJvmCommonSettings: _*)
    .settings(Defaults.itSettings: _*)
    .settings(
      scalaJSProjects := Seq(exampleE2eWebSocketJs),
      unmanagedResourceDirectories in Assets ++= Seq(
        (baseDirectory in exampleE2eWebSocketJs).value / "src" / "main" / "public"
      ),
      libraryDependencies ++= Seq(
        "org.eclipse.jetty.websocket" % "websocket-api" % JettyVersion,
        "org.eclipse.jetty.websocket" % "websocket-server" % JettyVersion
      ),
      (fork in IntegrationTest) := true,
      (javaOptions in IntegrationTest) ++= Seq(
        s"-DjarLocation=${assembly.value}"
      )
    )
    .dependsOn(exampleTestUtils % "it")

lazy val exampleE2eWebSocketJs = exampleE2eWebSocket.js
    .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
    .disablePlugins(AssemblyPlugin)
    .settings(exampleJsCommonSettings: _*)
    .settings(
      scalaJSUseMainModuleInitializer := true
    )

// Test Utils

lazy val exampleTestUtils = (project in file("examples/test-utils"))
    .disablePlugins(AssemblyPlugin)
    .settings(commonSettings: _*)
    .settings(exampleCommonSettings: _*)
    .settings(
      name += "-test-utils",
      libraryDependencies ++= Seq(
        "org.apache.httpcomponents" % "httpclient" % "4.+"
      )
    )
