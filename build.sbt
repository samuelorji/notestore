name := "bookstore"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq (
  "com.nimbusds" % "nimbus-jose-jwt" % "3.10",
  "org.codehaus.janino" % "janino" % "2.7.8",
  "joda-time" % "joda-time" % "2.10.5",
   "com.typesafe" % "config" % "1.4.0",
  "com.typesafe.akka" %% "akka-slf4j" % "2.6.8"
)
