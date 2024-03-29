val scala3Version = "3.4.0"
val zioVersion = "2.0.21"

lazy val root = project
  .in(file("."))
  .settings(
    name := "wordle",
    version := "0.1.0-SNAPSHOT",
    scalacOptions ++= Seq("-release","17"),

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-test" % zioVersion % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
