name := """ciandt-test-case"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in Compile)

libraryDependencies ++= Seq(
  javaJpa,
  javaJdbc,
  "org.hibernate" % "hibernate-entitymanager" % "4.3.9.Final", 
  "com.jayway.restassured" % "rest-assured" % "1.7" % "test",
  "org.xerial" % "sqlite-jdbc" % "3.7.2"
)     

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator


// fork in run := true