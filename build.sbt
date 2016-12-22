crossScalaVersions := Seq("2.11.8", "2.12.0")

val commonSettings = Seq(
  organization := "io.github.shogowada",
  name := "scala-json-rpc",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.0",
  licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
  homepage := Some(url("https://github.com/shogowada/scala-json-rpc"))
)

lazy val core = (crossProject in file("."))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,

        "org.scalatest" %%% "scalatest" % "3.+" % "test"
      )
    )
    .dependsOn(jsonSerializer)
    .dependsOn(upickleJsonSerializer % "test")

lazy val jvm = core.jvm
lazy val js = core.js

lazy val jsonSerializer = (crossProject in file("json-serializer"))
    .settings(commonSettings: _*)
    .settings(
      name += "-json-serializer"
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
      )
    )
    .dependsOn(jsonSerializer)

lazy val upickleJsonSerializerJvm = upickleJsonSerializer.jvm
lazy val upickleJsonSerializerJs = upickleJsonSerializer.js
