val stringValue: String = bindings["stringValue"] as String + "123"
val intValue: Int = 101
val booleanValue: Boolean = !(bindings["booleanValue"] as Boolean)
val future = bindings["future"]
future.waitForCompletion()