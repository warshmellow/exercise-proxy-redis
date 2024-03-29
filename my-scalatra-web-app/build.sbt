val ScalatraVersion = "2.6.5"

organization := "com.example"

name := "My Scalatra Web App"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.6"

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % ScalatraVersion,
  "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
  "org.scalamock" %% "scalamock" % "4.3.0" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.4.9.v20180320" % "container;compile",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "com.google.guava" % "guava" % "28.0-jre",
  "com.google.guava" % "guava-testlib" % "28.0-jre" % "test",
  "net.debasishg" %% "redisclient" % "3.10",
  "org.scalatra" %% "scalatra-json" % ScalatraVersion,
  "org.json4s"   %% "json4s-jackson" % "3.5.2",
  "com.typesafe.akka" %% "akka-actor" % "2.5.3",
  "net.databinder.dispatch" %% "dispatch-core" % "0.13.1",
  "com.typesafe" % "config" % "1.3.4"
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

enablePlugins(SbtTwirl)
enablePlugins(ScalatraPlugin)
