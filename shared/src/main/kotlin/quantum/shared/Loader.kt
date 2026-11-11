package quantum.shared

enum class Loader(val display_name: String, val family: LoaderFamily) {
	VANILLA("Vanilla", LoaderFamily.VANILLA),
	BABRIC("Babric", LoaderFamily.FABRIC),
	FABRIC("Fabric", LoaderFamily.FABRIC),
	FORGE("Forge", LoaderFamily.FORGE),
	LEGACY_FABRIC("Legacy Fabric", LoaderFamily.FABRIC),
	NEOFORGE("NeoForge", LoaderFamily.NEOFORGE),
	ORNITHE_FABRIC("Ornithe", LoaderFamily.FABRIC),
	ORNITHE_QUILT("Ornithe", LoaderFamily.QUILT),
	QUILT("Quilt", LoaderFamily.QUILT);

	val full_name: String
		get() = if(display_name == family.display_name) display_name else "${family.display_name} ($display_name)"
}