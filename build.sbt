val scala3Version = "3.3.4"

enablePlugins(VcpkgNativePlugin, ScalaNativePlugin, BindgenPlugin)

vcpkgDependencies := VcpkgDependencies(
  "portaudio"
)


import scala.scalanative.build._

nativeConfig ~= {
  _.withLTO(LTO.full)
  .withMode(Mode.releaseFull)
  .withGC(GC.immix)
}

vcpkgNativeConfig ~= {
  _.withRenamedLibraries(Map("portaudio" -> "portaudio-2.0"))
}

import bindgen.interface.Binding

bindgenBindings := {
  val configurator = vcpkgConfigurator.value
  Seq(
    Binding(
        configurator.includes("portaudio") / "portaudio.h",
        "portaudio"
      )
      .withLinkName("portaudio")
  )
}

lazy val root = project
  .in(file("."))
  .settings(
    name := "pedalboard",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies += "org.typelevel" %%% "cats-effect" % "3.7-2eee55d",
    libraryDependencies += "co.fs2"        %%% "fs2-core"    % "3.12-2d1232c-20250201T130258Z-SNAPSHOT",
    Compile / scalacOptions -= "-Xfatal-warnings"
  )
