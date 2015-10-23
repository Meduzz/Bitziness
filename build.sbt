name := "Bitziness"

version := "0.6"

scalaVersion := "2.11.1"

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "rediscala" at "http://dl.bintray.com/etaty/maven"

resolvers += "clojars" at "http://clojars.org/repo"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.7"

libraryDependencies += "com.typesafe.akka" %% "akka-camel" % "2.3.7"

libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.3.7"

libraryDependencies += "com.typesafe.akka" %% "akka-agent" % "2.3.7"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.3.7"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.3.7" % "test"

libraryDependencies += "com.rabbitmq" % "amqp-client" % "3.2.2"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.0" % "test"

libraryDependencies += "io.spray" % "spray-can_2.11" % "1.3.1"

libraryDependencies += "com.etaty.rediscala" %% "rediscala" % "1.4.0"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.10"

libraryDependencies += "org.scalatra.scalate" %% "scalate-core" % "1.7.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.13"

libraryDependencies += "de.neuland-bfi" % "jade4j" % "0.4.2"

libraryDependencies += "com.typesafe.akka" %% "akka-stream-experimental" % "1.0"

libraryDependencies += "com.typesafe.akka" %% "akka-http-core-experimental" % "1.0"

libraryDependencies += "com.aphyr" % "riemann-java-client" % "0.4.0"
