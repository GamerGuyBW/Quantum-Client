package quantum.shared

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

enum class SetupTaskState {
	WAITING,
	RUNNING,
	DONE,
	FAILED
}

object VersionSetup {
	private val tasks: Map<String, () -> Unit> = buildMap {
		put(Loader.VANILLA.display_name) { VersionStore.fetchManifest() }

		Loader.entries
			.filter { loader -> LoaderVersionStore.hasVersionList(loader) }
			.forEach { loader -> put(loader.display_name) { LoaderVersionStore.prepareLoader(loader) } }

		Addon.entries.forEach { addon -> put(addonTaskName(addon)) { AddonVersionStore.prepareAddon(addon) } }
	}

	private val states = ConcurrentHashMap<String, SetupTaskState>()

	@Volatile
	private var executor: ExecutorService? = null

	@Synchronized
	fun start() {
		if(executor != null) {
			return
		}

		executor = Executors.newCachedThreadPool()

		tasks.keys.forEach { task_name -> states[task_name] = SetupTaskState.WAITING }
		tasks.keys.forEach { task_name -> submitTask(task_name) }
	}

	@Synchronized
	fun stop() {
		val current_executor = executor ?: return
		executor = null
		current_executor.shutdownNow()
		current_executor.awaitTermination(5, TimeUnit.SECONDS)
	}

	fun loaderState(loader: Loader): SetupTaskState {
		return taskState(loader.display_name)
	}

	fun addonState(addon: Addon): SetupTaskState {
		return taskState(addonTaskName(addon))
	}

	fun retry(loader: Loader) {
		retryTask(loader.display_name)
	}

	fun retryAddon(addon: Addon) {
		retryTask(addonTaskName(addon))
	}

	private fun addonTaskName(addon: Addon): String {
		return "${addon.display_name} add-on"
	}

	private fun taskState(task_name: String): SetupTaskState {
		return states[task_name] ?: SetupTaskState.WAITING
	}

	private fun retryTask(task_name: String) {
		val task_state = taskState(task_name)

		if(task_state == SetupTaskState.FAILED || task_state == SetupTaskState.DONE) {
			submitTask(task_name)
		}
	}

	private fun submitTask(task_name: String) {
		val current_executor = executor ?: return
		val task_work = tasks.getValue(task_name)
		states[task_name] = SetupTaskState.WAITING

		current_executor.submit {
			states[task_name] = SetupTaskState.RUNNING
			states[task_name] = try {
				task_work()
				SetupTaskState.DONE
			} catch(error: Exception) {
				SetupTaskState.FAILED
			}
		}
	}
}