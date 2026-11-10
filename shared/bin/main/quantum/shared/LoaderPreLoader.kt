package quantum.shared

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

enum class PreloadState {
	WAITING,
	LOADING,
	READY,
	FAILED
}

data class PreloadStatus(
	val state: PreloadState,
	val checked_count: Int,
	val total_count: Int
)

object LoaderPreloader {
	private const val worker_count = 8

	private val preload_loaders = Loader.entries.filter { loader -> LoaderVersionStore.hasVersionList(loader) }
	private val states = ConcurrentHashMap<Loader, PreloadState>()
	private val checked_counts = ConcurrentHashMap<Loader, AtomicInteger>()
	private val total_counts = ConcurrentHashMap<Loader, Int>()
	private val remaining_counts = ConcurrentHashMap<Loader, AtomicInteger>()
	private val pending_checks = ConcurrentHashMap<Loader, ConcurrentLinkedQueue<MavenArtifact>>()
	private val failed_checks = ConcurrentHashMap<Loader, ConcurrentLinkedQueue<MavenArtifact>>()
	private val next_loader_index = AtomicInteger(0)

	@Volatile
	private var priority_loader: Loader? = null

	@Volatile
	private var executor: ExecutorService? = null

	@Synchronized
	fun start() {
		if(executor != null) {
			return
		}

		val new_executor = Executors.newFixedThreadPool(worker_count)
		executor = new_executor

		preload_loaders.forEach { loader ->
			states[loader] = PreloadState.WAITING
			checked_counts[loader] = AtomicInteger(0)
			total_counts[loader] = 0
			remaining_counts[loader] = AtomicInteger(0)
			pending_checks[loader] = ConcurrentLinkedQueue()
			failed_checks[loader] = ConcurrentLinkedQueue()
		}

		new_executor.submit { runCatching { VersionStore.fetchManifest() } }
		preload_loaders.forEach { loader -> new_executor.submit { prepareLoader(loader) } }
	}

	@Synchronized
	fun stop() {
		val current_executor = executor ?: return
		executor = null
		current_executor.shutdownNow()
		current_executor.awaitTermination(5, TimeUnit.SECONDS)
	}

	fun prioritise(loader: Loader) {
		priority_loader = loader
	}

	fun status(loader: Loader): PreloadStatus {
		return PreloadStatus(
			state = states[loader] ?: PreloadState.WAITING,
			checked_count = checked_counts[loader]?.get() ?: 0,
			total_count = total_counts[loader] ?: 0
		)
	}

	fun retry(loader: Loader) {
		val current_executor = executor ?: return
		if(states[loader] != PreloadState.FAILED) {
			return
		}

		val failed_queue = failed_checks.getValue(loader)

		if(LoaderVersionStore.usesInstallerChecks(loader) && failed_queue.isNotEmpty()) {
			val retry_artifacts = generateSequence { failed_queue.poll() }.toList()
			states[loader] = PreloadState.LOADING
			remaining_counts.getValue(loader).set(retry_artifacts.size)
			pending_checks.getValue(loader).addAll(retry_artifacts)
			repeat(retry_artifacts.size) { current_executor.submit { runNextCheck() } }
		} else {
			states[loader] = PreloadState.WAITING
			current_executor.submit { prepareLoader(loader) }
		}
	}

	private fun prepareLoader(loader: Loader) {
		states[loader] = PreloadState.LOADING

		try {
			if(!LoaderVersionStore.usesInstallerChecks(loader)) {
				LoaderVersionStore.downloadSupportedGameVersions(loader)
				states[loader] = PreloadState.READY
				return
			}

			val artifacts = LoaderVersionStore.mavenArtifacts(loader)
			val checked_versions = LoaderVersionStore.checkedInstallerResults(loader).keys
			val unchecked_artifacts = artifacts.filter { artifact -> artifact.version !in checked_versions }

			total_counts[loader] = artifacts.size
			checked_counts.getValue(loader).set(artifacts.size - unchecked_artifacts.size)

			if(unchecked_artifacts.isEmpty()) {
				finishLoader(loader)
				return
			}

			remaining_counts.getValue(loader).set(unchecked_artifacts.size)
			pending_checks.getValue(loader).addAll(unchecked_artifacts)

			val current_executor = executor ?: return
			repeat(unchecked_artifacts.size) { current_executor.submit { runNextCheck() } }
		} catch(error: Exception) {
			states[loader] = PreloadState.FAILED
		}
	}

	private fun runNextCheck() {
		val next_check = takeNextCheck() ?: return
		val (loader, artifact) = next_check

		try {
			LoaderVersionStore.checkInstaller(loader, artifact)
			checked_counts.getValue(loader).incrementAndGet()
		} catch(error: InterruptedException) {
			Thread.currentThread().interrupt()
			failed_checks.getValue(loader).add(artifact)
		} catch(error: Exception) {
			failed_checks.getValue(loader).add(artifact)
		}

		if(remaining_counts.getValue(loader).decrementAndGet() == 0) {
			if(failed_checks.getValue(loader).isEmpty()) {
				finishLoader(loader)
			} else {
				states[loader] = PreloadState.FAILED
			}
		}
	}

	private fun takeNextCheck(): Pair<Loader, MavenArtifact>? {
		val current_priority = priority_loader
		if(current_priority != null) {
			val priority_artifact = pending_checks[current_priority]?.poll()
			if(priority_artifact != null) {
				return current_priority to priority_artifact
			}
		}

		repeat(preload_loaders.size) {
			val loader = preload_loaders[next_loader_index.getAndIncrement().mod(preload_loaders.size)]
			val artifact = pending_checks[loader]?.poll()
			if(artifact != null) {
				return loader to artifact
			}
		}

		return null
	}

	private fun finishLoader(loader: Loader) {
		try {
			LoaderVersionStore.finishInstallerChecks(loader)
			states[loader] = PreloadState.READY
		} catch(error: Exception) {
			states[loader] = PreloadState.FAILED
		}
	}
}