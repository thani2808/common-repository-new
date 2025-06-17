package org.example

class CleanWorkspace implements Serializable {
    def steps

    CleanWorkspace(steps) {
        this.steps = steps
    }

    void clean() {
        steps.cleanWs()
    }
}
