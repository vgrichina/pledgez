package com.componentix.pledgez

class Pledges {
    String name
    def contexts = []

    static Pledges describe(String name) {
        return new Pledges(name: name)
    }

    Pledges addBatch(Map batch) {
        def newContexts = new PledgeContext(null, "", batch).childNodes
        newContexts.each { it.parentContext = null }
        contexts.addAll(newContexts)

        return this
    }

    Pledges run() {
        return run({ results, updatedPath ->
            println "results = $results, updatedPath = $updatedPath"
        } as PledgesRunHandler);
    }

    Pledges run(PledgesRunHandler handler) {
        contexts.each { it.run(null, [:], handler) }

        return this
    }
}

class Pledge {
    String name
    Closure closure
}

interface PledgesRunHandler {
    void resultsUpdated(Map results, List updatedPath);
}

class PledgeContext {
    PledgeContext parentContext
    String name
    boolean topicSpecified = false
    def topic
    def childNodes = []

    PledgeContext(PledgeContext parentContext, String name, Map map) {
        this.parentContext = parentContext
        this.name = name
        if (map.containsKey("topic")) {
            this.topicSpecified = true
            this.topic = map.remove("topic")
        }
        this.childNodes = map.collect { key, value ->
            if (value instanceof Closure) {
                return new Pledge(name: key, closure: value)
            } else {
                return new PledgeContext(this, key, value)
            }
        }
    }

    def getPath() {
        (parentContext?.path ?: []) + name
    }

    void updateResults(Map results, List path, def result) {
        assert path.size() > 0

        if (path.size() == 1) {
            results[path.head()] = result
        } else {
            if (results[path.head()]) {
                assert results[path.head()] instanceof Map
            } else {
                results[path.head()] = [:]
            }

            updateResults(results[path.head()], path.tail(), result)
        }
    }

    void run(def topic, Map results, PledgesRunHandler handler) {
        if (this.topicSpecified) {
            topic = this.topic.call()
        }

        childNodes.each { node ->
            if (node instanceof Pledge) {
                def result = null
                try {
                    result = node.closure.call(topic)
                } catch (Exception e) {
                    result = e
                }

                def path = this.path + node.name
                updateResults(results, path, result)
                handler.resultsUpdated(results, path)
            } else {
                node.run(topic, results, handler)
            }
        }
    }
}

