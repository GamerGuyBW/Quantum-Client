package quantum.client.ui

import quantum.shared.LoaderVersion

fun loaderVersionOptions(loader_versions: List<LoaderVersion>): List<DropdownOption<String?>> {
	return loader_versions.map { loader_version ->
		val label = if(loader_version.is_stable) loader_version.display_name else "${loader_version.display_name} (unstable)"
		DropdownOption<String?>(loader_version.version, label)
	}
}

fun defaultLoaderVersion(loader_versions: List<LoaderVersion>): LoaderVersion? {
	return loader_versions.firstOrNull { loader_version -> loader_version.is_stable } ?: loader_versions.firstOrNull()
}