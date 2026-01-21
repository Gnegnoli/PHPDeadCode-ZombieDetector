package com.zombiedetector.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "PhpDeadZombieDetectorSettings",
    storages = [Storage("php-dead-zombie-detector.xml")]
)
class DeadCodeSettings : PersistentStateComponent<DeadCodeSettings.State> {

    data class State(
        var includeTests: Boolean = true
    )

    var state: State = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }
}

