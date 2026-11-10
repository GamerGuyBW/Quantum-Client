package quantum.shared

import kotlinx.serialization.Serializable

data class LoaderVersion(
	val version: String,
	val display_name: String,
	val is_stable: Boolean
)

@Serializable
internal data class MavenArtifact(
	val version: String,
	val installer_url: String
)

@Serializable
internal data class InstallerResult(
	val version: String,
	val minecraft_version: String?
)

@Serializable
internal data class LoaderGameVersion(
	val version: String
)

@Serializable
internal data class FabricLoaderEntry(
	val loader: FabricLoaderInfo
)

@Serializable
internal data class FabricLoaderInfo(
	val version: String,
	val stable: Boolean
)

@Serializable
internal data class QuiltLoaderEntry(
	val loader: QuiltLoaderInfo
)

@Serializable
internal data class QuiltLoaderInfo(
	val version: String
)