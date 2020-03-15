val scripts = bindings["scripts"] as org.mini2Dx.miniscript.core.EmbeddedScriptInvoker
scripts.invokeSync("default.kts")
val intValue2: Int = 102