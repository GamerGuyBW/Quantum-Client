package quantum.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class LoaderVersion(
	val version: String,
	val display_name: String,
	val is_stable: Boolean,
	val installer_url: String? = null,
	val profile_url: String? = null
) {
	val is_verifiable: Boolean
		get() = installer_url != null || profile_url != null
}

data class LoaderGenerations(
	val available: List<Int>,
	val stable: Int
)

@Serializable
internal data class MavenArtifact(
	val version: String,
	val installer_url: String
)

@Serializable
internal data class ResolvedLoaderVersion(
	val version: String,
	val game_version: String?,
	val installer_url: String
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

@Serializable
internal data class OrnitheGenerations(
	@SerialName("latestIntermediaryGeneration") val latest_generation: Int,
	@SerialName("stableIntermediaryGeneration") val stable_generation: Int
)

@Serializable
internal data class OrnitheGameVersion(
	val version: String,
	val environment: String
)