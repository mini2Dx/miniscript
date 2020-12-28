val scripts = bindings["scripts"] as org.mini2Dx.miniscript.core.EmbeddedScriptInvoker
scripts.invokeSync("invokeWithinScript.kts")
val stringValue: String = bindings["stringValue"] as String + "4"