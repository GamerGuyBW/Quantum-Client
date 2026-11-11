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
		return states[loader.display_name] ?: SetupTaskState.WAITING
	}

	fun retry(loader: Loader) {
		val loader_state = loaderState(loader)

		if(loader_state == SetupTaskState.FAILED || loader_state == SetupTaskState.DONE) {
			submitTask(loader.display_name)
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