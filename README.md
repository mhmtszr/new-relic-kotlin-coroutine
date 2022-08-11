# new-relic-kotlin-coroutine

New Relic Kotlin Instrumentation for Kotlin Coroutine. It successfully handles thread changes in suspend states.


## Usage

1- Update the context of your coroutine by using new relic coroutine context elements
```kotlin
suspend fun <T> withMdcContext(
    block: suspend CoroutineScope.() -> T,
): T {
    val txn = NewRelic.getAgent().transaction
    val token = txn.token
    return withContext(
        MDCContext() + NewRelicTransaction(txn) + NewRelicToken(token),
    ) {
        return@withContext try {
            block.invoke(this)
        } finally {
            token.expire()
        }
    }
}

@Trace(dispatcher = true)
suspend fun testEndpoint() = withMdcContext {
    ...
}
```

2- Add segment to trace specific block of the code
```kotlin
override suspend fun findAll(): List<Any> =
        withNewRelicSegment("findAll", "Couchbase", "yourDb") {
            val query =
                "SELECT * FROM yourDb"
            ...
        }
```