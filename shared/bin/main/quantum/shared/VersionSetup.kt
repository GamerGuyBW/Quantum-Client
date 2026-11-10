package quantum.shared

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

enum class SetupTaskState {
	WAITING,
	RUNNING,
	DONE,
	FAILED
}

data class SetupTaskStatus(
	val name: String,
	val state: SetupTaskState,
	val checked_count: Int,
	val total_count: Int
)

object VersionSetup {
	private const val workers_per_loader = 16

	private val vanilla_task_name = Loader.VANILLA.display_name
	private val setup_loaders = Loader.entries.filter { loader -> LoaderVersionStore.hasVersionList(loader) }
	private val task_names = listOf(vanilla_task_name) + setup_loaders.map { loader -> loader.display_name }
	private val states = ConcurrentHashMap<String, SetupTaskState>()
	private val checked_counts = ConcurrentHashMap<String, AtomicInteger>()
	private val total_counts = ConcurrentHashMap<String, Int>()

	@Volatile
	private var executor: ExecutorService? = null

	@Synchronized
	fun start() {
		if(executor != null) {
			return
		}

		executor = Executors.newCachedThreadPool()

		task_names.forEach { task_name ->
			states[task_name] = SetupTaskState.WAITING
			checked_counts[task_name] = AtomicInteger(0)
			total_counts[task_name] = 0
		}

		submitVanillaTask()
		setup_loaders.forEach { loader -> submitLoaderTask(loader) }
	}

	@Synchronized
	fun stop() {
		val current_executor = executor ?: return
		executor = null
		current_executor.shutdownNow()
		current_executor.awaitTermination(5, TimeUnit.SECONDS)
	}

	fun statuses(): List<SetupTaskStatus> {
		return task_names.map { task_name ->
			SetupTaskStatus(
				name = task_name,
				state = states[task_name] ?: SetupTaskState.WAITING,
				checked_count = checked_counts[task_name]?.get() ?: 0,
				total_count = total_counts[task_name] ?: 0
			)
		}
	}

	fun isComplete(): Boolean {
		return task_names.all { task_name -> states[task_name] == SetupTaskState.DONE }
	}

	fun retryFailed() {
		if(states[vanilla_task_name] == SetupTaskState.FAILED) {
			submitVanillaTask()
		}

		setup_loaders
			.filter { loader -> states[loader.display_name] == SetupTaskState.FAILED }
			.forEach { loader -> submitLoaderTask(loader) }
	}

	private fun submitVanillaTask() {
		val current_executor = executor ?: return
		states[vanilla_task_name] = SetupTaskState.WAITING

		current_executor.submit {
			states[vanilla_task_name] = SetupTaskState.RUNNING
			states[vanilla_task_name] = try {
				VersionStore.fetchManifest()
				SetupTaskState.DONE
			} catch(error: Exception) {
				SetupTaskState.FAILED
			}
		}
	}

	private fun submitLoaderTask(loader: Loader) {
		val current_executor = executor ?: return
		states[loader.display_name] = SetupTaskState.WAITING

		current_executor.submit {
			states[loader.display_name] = SetupTaskState.RUNNING
			states[loader.display_name] = try {
				if(prepareLoader(loader)) SetupTaskState.DONE else SetupTaskState.FAILED
			} catch(error: Exception) {
				SetupTaskState.FAILED
			}
		}
	}

	private fun prepareLoader(loader: Loader): Boolean {
		if(!LoaderVersionStore.usesInstallerChecks(loader)) {
			LoaderVersionStore.downloadSupportedGameVersions(loader)
			return true
		}

		val task_name = loader.display_name
		val artifacts = LoaderVersionStore.mavenArtifacts(loader)

		VersionDatabase.prune(loader, artifacts.map { artifact -> artifact.version }.toSet())

		val known_versions = VersionDatabase.load(loader).keys
		val unchecked_artifacts = artifacts.filter { artifact -> artifact.version !in known_versions }

		total_counts[task_name] = artifacts.size
		checked_counts.getValue(task_name).set(artifacts.size - unchecked_artifacts.size)

		val all_checked = checkArtifacts(loader, unchecked_artifacts)

		if(all_checked) {
			LoaderVersionStore.writeSupportedFromDatabase(loader)
		}

		return all_checked
	}

	private fun checkArtifacts(loader: Loader, artifacts: List<MavenArtifact>): Boolean {
		if(artifacts.isEmpty()) {
			return true
		}

		val current_executor = executor ?: return false
		val pending_artifacts = ConcurrentLinkedQueue(artifacts)
		val failure_count = AtomicInteger(0)
		val checked_counter = checked_counts.getValue(loader.display_name)

		val workers = List(minOf(workers_per_loader, artifacts.size)) {
			current_executor.submit {
				var artifact = pending_artifacts.poll()

				while(artifact != null && !Thread.currentThread().isInterrupted) {
					try {
						LoaderVersionStore.checkInstaller(loader, artifact)
						checked_counter.incrementAndGet()
					} catch(error: InterruptedException) {
						Thread.currentThread().interrupt()
						failure_count.incrementAndGet()
					} catch(error: Exception) {
						failure_count.incrementAndGet()
					}

					artifact = pending_artifacts.poll()
				}
			}
		}

		workers.forEach { worker -> worker.get() }
		return failure_count.get() == 0 && pending_artifacts.isEmpty()
	}
}