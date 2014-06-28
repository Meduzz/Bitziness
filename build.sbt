name := "Bitziness"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.1"

libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.3.3"

libraryDependencies += "com.typesafe.akka" % "akkka-camel" % "2.3.3"

libraryDependencies += "com.typesafe.akka" % "akka-cluster" % "2.3.3"

libraryDependencies += "com.typesafe.akka" % "akka-testkit" % "2.3.3" % "test"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"