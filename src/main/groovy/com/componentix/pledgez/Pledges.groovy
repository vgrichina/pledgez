package com.componentix.pledgez

class Pledges {
    String name
    def contexts = []

    static Pledges describe(String name) {
        return new Pledges(name: name)
    }

    Pledges addBatch(Map batch) {
        contexts.addAll(new PledgeContext(batch).childNodes)

        return this
    }

    Pledges run() {
        return run({ results, updatedPaths ->
            println "results = $resultsUpdated, updatedPaths = $updatedPaths"
        } as PledgesRunHandler);
    }

    Pledges run(PledgesRunHandler handler) {
        context.each { it.run(null, [:], handler) }
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

    PledgeContext(String name, Map map) {
        this.name = name
        if (map.containsKey("topic")) {
            this.topicSpecified = true
            this.topic = map.remove("topic")
        }
        this.childNodes = map.collect { key, value ->
            if (value instanceof Closure) {
                return new Pledge(name: key, closure: value)
            } else {
                return new PledgeContext(key, value)
            }
        }
    }

    def getPath() {
        (parentContext.path ?: []) + name
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
            topic = this.topic
        }

        childNodes.each { node ->
            if (node instanceof Pledge) {
                def result = null
                try {
                    result = node.closure.run()
                } catch (Exception e) {
                    result = e
                }

                def path = this.path + node.name
                updateResults(results, path, result)
                handler.resultsUpdated(topic, results, path)
            } else {
                node.run(results, handler)
            }
        }
    }
}

