name := "bookstore"

version := "0.1"

scalaVersion := "2.12.6"

resolvers += Resolver.bintrayRepo("hseeberger", "maven")
lazy val akkaHttpVersion = "10.1.11"
lazy val akkaActorVersion = "2.6.4"
libraryDependencies ++= Seq (
  "com.nimbusds" % "nimbus-jose-jwt" % "3.10",
  "org.codehaus.janino" % "janino" % "2.7.8",
  "joda-time" % "joda-time" % "2.10.5",
   "com.typesafe" % "config" % "1.4.0",
  "com.github.etaty" %% "rediscala" % "1.9.0",
  //Akka dependencies
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaActorVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaActorVersion,
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.34.0"

)
