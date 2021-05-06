name := "Lecture-8"

version := "0.1"

scalaVersion := "2.13.5"

libraryDependencies +=  "com.lihaoyi" %% "scalatags" % "0.9.4"
libraryDependencies +=  "com.lihaoyi" %% "cask" % "0.7.10"

libraryDependencies +=  "com.lihaoyi" %% "utest" % "0.7.9"
testFrameworks += new TestFramework("utest.runner.Framework")
libraryDependencies +=  "com.lihaoyi" %% "requests" % "0.6.8"

Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)