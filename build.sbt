name := "Bitziness"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.1"

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "rediscala" at "http://dl.bintray.com/etaty/maven"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.3"

libraryDependencies += "com.typesafe.akka" %% "akka-camel" % "2.3.3"

libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.3.3"

libraryDependencies += "com.typesafe.akka" %% "akka-agent" % "2.3.3"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.3.3" % "test"

libraryDependencies += "com.rabbitmq" % "amqp-client" % "3.2.2"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.0" % "test"

libraryDependencies += "io.spray" % "spray-can_2.11" % "1.3.1"

libraryDependencies += "com.etaty.rediscala" %% "rediscala" % "1.4.0"