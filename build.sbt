import sbt.Keys._
import sbt._
//import com.github.play2war.plugin._
lazy val root = (project in file(".")).enablePlugins(PlayScala)

name := "sprint2"

version := "0.1.0-SNAPSHOT"

organization := "net.piki-ds"

scalaVersion := "2.11.6"

aggregate in runMain := true

val sprayV = "1.3.3"


//Play2WarKeys.servletVersion := "3.0"


//Play2WarPlugin.play2WarSettings

//net.virtualvoid.sbt.graph.Plugin.graphSettings

val hadoopversion = "2.6.0"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
"org.apache.hadoop" % "hadoop-hdfs" % hadoopversion exclude("commons-daemon", "commons-daemon") excludeAll ExclusionRule(organization = "javax.servlet") excludeAll(ExclusionRule(organization = "org.eclipse.jetty")),
  "org.apache.hadoop" % "hadoop-auth" % hadoopversion exclude("commons-daemon", "commons-daemon")  excludeAll ExclusionRule(organization = "javax.servlet") excludeAll(ExclusionRule(organization = "org.eclipse.jetty")),
  "org.apache.hadoop" % "hadoop-client" % hadoopversion exclude("commons-daemon", "commons-daemon") excludeAll ExclusionRule(organization = "javax.servlet")  excludeAll(ExclusionRule(organization = "org.eclipse.jetty")),
  "org.slf4j" % "slf4j-api" % "1.7.2",
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.9",
  "com.google.code.findbugs" % "jsr305" % "2.0.3",
  "com.quantifind" %% "wisp" % "0.0.4",
  "c3p0" % "c3p0" % "0.9.1.2",
  "redis.clients" % "jedis" % "2.2.1",
  "org.webjars" % "bootstrap" % "2.3.1",
  "org.webjars" % "flot" % "0.8.0",
  "com.google.guava" % "guava" % "18.0",
  "mysql" % "mysql-connector-java" % "5.1.32",
  "com.alibaba" % "fastjson" % "1.1.41",
  "org.json4s" %% "json4s-jackson" % "3.2.11",
  "org.json4s" %% "json4s-ext" % "3.2.11",
  "org.json4s" %% "json4s-native" % "3.2.11",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "com.typesafe.slick" %% "slick-codegen" % "2.1.0",
  "org.scalatest" %% "scalatest" % "2.1.6" % "test",
  "cc.factorie" %% "factorie" % "1.1.1",
  "io.continuum.bokeh" %% "bokeh" % "0.5",
  "org.feijoas" %% "mango" % "0.11",
  "org.scalanlp" %% "breeze-viz" % "0.11.2",
  "nz.ac.waikato.cms.weka" % "weka-dev" % "3.7.12",
  "com.twitter.penguin" % "korean-text" % "4.0",
  "org.apache.spark" %% "spark-core" % "1.3.1",
  "org.apache.spark" %% "spark-streaming" % "1.3.1",
  "org.apache.spark" %% "spark-sql" % "1.3.1",
  "org.apache.spark" %% "spark-mllib" % "1.3.1",
  "org.apache.spark" %% "spark-repl" % "1.3.1",
  "org.apache.spark" %% "spark-yarn" % "1.3.1",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.4" % "test"
)


resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "Scala Tools Snapshots" at "http://scala-tools.org/repo-snapshots/",
  "spray repo" at "http://repo.spray.io/"
)

resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"


javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

retrieveManaged := true

//publishTo := Some("daum" at "http://maven.daumcorp.com/content/groups/daum-sqt-group/")

publishMavenStyle := true


publishTo := Some("daum snapshot" at "http://maven.daumcorp.com/content/repositories/daum-sqt-snapshots")

libraryDependencies ~= { _.map(_.excludeAll(
  ExclusionRule("org.slf4j", "slf4j*")
))
}


pomExtra := (
  // <distributionManagement>
  //        <repository>
  //                <id>daum</id>
  //                <name>Daum Repository</name>
  //                <url>http://maven.daumcorp.com/content/repositories/daum</url>
  //        </repository>
  //        <snapshotRepository>
  //                <id>daum-snapshots</id>
  //                <name>Daum Snapshot Repository</name>
  //                <url>http://maven.daumcorp.com/content/repositories/daum-snapshots</url>
  //        </snapshotRepository>
  //    </distributionManagement>
  <scm>
    <url>http://digit.daumcorp.com/badend/arfapi</url>
    <connection>scm:git:git@dgit.co:badend/arfapi.git</connection>
  </scm>
    <developers>
      <developer>
        <id>badend</id>
        <name>badend</name>
      </developer>
    </developers>)
