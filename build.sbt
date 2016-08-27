name := "Bitziness"

version := "0.7.5"

scalaVersion := "2.11.7"

resolvers += "clojars" at "http://clojars.org/repo"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.4"

libraryDependencies += "com.typesafe.akka" %% "akka-camel" % "2.4.4"

libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.4.4"

libraryDependencies += "com.typesafe.akka" %% "akka-agent" % "2.4.4"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.4.4"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.4.4" % "test"

libraryDependencies += "com.rabbitmq" % "amqp-client" % "3.2.2"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.0" % "test"

libraryDependencies += "com.github.etaty" %% "rediscala" % "1.6.0"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.10"

libraryDependencies += "org.scalatra.scalate" %% "scalate-core" % "1.7.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.13"

libraryDependencies += "de.neuland-bfi" % "jade4j" % "0.4.2"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.4.4"

libraryDependencies += "com.typesafe.akka" %% "akka-http-core" % "2.4.4"

libraryDependencies += "com.aphyr" % "riemann-java-client" % "0.4.0"

libraryDependencies += "com.couchbase.client" % "java-client" % "2.0.3"

libraryDependencies += "net.fwbrasil" % "activate-core_2.11" % "1.7"

libraryDependencies += "org.neo4j" % "neo4j" % "2.3.0"

libraryDependencies += "com.google.guava" % "guava" % "19.0"

libraryDependencies += "io.grpc" % "grpc-netty" % "0.15.0"

libraryDependencies += "io.grpc" % "grpc-protobuf" % "0.15.0"

libraryDependencies += "io.grpc" % "grpc-stub" % "0.15.0"
