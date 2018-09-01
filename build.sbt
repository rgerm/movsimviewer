import Dependencies._
import sbt.Keys.resolvers

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.10.7",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "movscala",
    libraryDependencies += scalaTest % Test,
    resolvers += Resolver.mavenLocal,
    libraryDependencies += "org.movsim" % "MovsimViewer" % "1.7.0-SNAPSHOT",
    libraryDependencies += "org.scala-lang" % "scala-swing" % "2.10.7"
  )
