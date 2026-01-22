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

    private var myState: State = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }
}

