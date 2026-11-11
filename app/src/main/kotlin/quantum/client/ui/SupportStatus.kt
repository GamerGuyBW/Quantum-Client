package quantum.client.ui

import quantum.shared.Addon
import quantum.shared.Loader
import quantum.shared.LoaderFamily
import quantum.shared.LoaderVersionStore
import quantum.shared.VersionManifest
import quantum.shared.VersionRanges

enum class SupportStatus {
	SUPPORTED,
	UNSUPPORTED,
	LOADING,
	FAILED
}

data class SupportData(
	val loader_versions: Map<Loader, Set<String>> = emptyMap(),
	val failed_loaders: Set<Loader> = emptySet(),
	val addon_support: Map<Addon, Map<Loader, List<String>>> = emptyMap(),
	val failed_addons: Set<Addon> = emptySet()
) {
	fun loaderStatus(loader: Loader, game_version_id: String): SupportStatus {
		if(!LoaderVersionStore.hasVersionList(loader)) {
			return SupportStatus.SUPPORTED
		}

		val supported_versions = loader_versions[loader]

		return if(supported_versions != null) {
			if(game_version_id in supported_versions) SupportStatus.SUPPORTED else SupportStatus.UNSUPPORTED
		} else if(loader in failed_loaders) {
			SupportStatus.FAILED
		} else {
			SupportStatus.LOADING
		}
	}

	fun familyStatus(family: LoaderFamily, game_version_id: String): SupportStatus {
		val project_statuses = projectsOf(family).map { loader -> loaderStatus(loader, game_version_id) }

		return if(SupportStatus.SUPPORTED in project_statuses) {
			SupportStatus.SUPPORTED
		} else if(SupportStatus.LOADING in project_statuses) {
			SupportStatus.LOADING
		} else if(SupportStatus.FAILED in project_statuses) {
			SupportStatus.FAILED
		} else {
			SupportStatus.UNSUPPORTED
		}
	}

	fun loaderRanges(loader: Loader, manifest: VersionManifest): List<String>? {
		if(!LoaderVersionStore.hasVersionList(loader)) {
			return VersionRanges.describe(manifest.versions.map { version -> version.id }, manifest)
		}

		return loader_versions[loader]?.let { supported_versions -> VersionRanges.describe(supported_versions, manifest) }
	}
}

fun projectsOf(family: LoaderFamily): List<Loader> {
	return Loader.entries.filter { loader -> loader.family == family }
}

fun statusText(status: SupportStatus, game_version_id: String): String {
	return when(status) {
		SupportStatus.SUPPORTED -> "Available"
		SupportStatus.UNSUPPORTED -> "Not available for `$game_version_id`"
		SupportStatus.LOADING -> "Loading…"
		SupportStatus.FAILED -> "Could not load"
	}
}