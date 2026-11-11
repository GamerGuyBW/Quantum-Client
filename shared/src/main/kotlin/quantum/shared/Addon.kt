package quantum.shared

enum class Addon(val display_name: String, val base_loaders: List<Loader>) {
	LITELOADER("LiteLoader", listOf(Loader.VANILLA))
}