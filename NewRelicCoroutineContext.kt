import com.newrelic.api.agent.*
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class NewRelicTransaction(val txn: Transaction) :
    AbstractCoroutineContextElement(NewRelicTransaction) {
    companion object Key : CoroutineContext.Key<NewRelicTransaction>
}

class NewRelicSegment(val segment: Segment) :
    AbstractCoroutineContextElement(NewRelicSegment) {
    companion object Key : CoroutineContext.Key<NewRelicSegment>
}

class NewRelicToken(val token: Token) : AbstractCoroutineContextElement(NewRelicToken) {
    companion object Key : CoroutineContext.Key<NewRelicToken>
}

suspend fun <T> withNewRelicSegment(
    name: String,
    productName: String,
    collectionName: String,
    block: suspend () -> T
): T {
    return runInAsyncTrace {
        val token = coroutineContext[NewRelicToken]
        token?.token?.link()
        val segment = coroutineContext[NewRelicTransaction]?.let {
            NewRelicSegment(it.txn.startSegment(name))
        }
        return@runInAsyncTrace if (token != null && segment != null) {
            withContext(segment) {
                try {
                    runInAsyncTrace {
                        val response = block()
                        token.token.link()
                        response
                    }
                } finally {
                    segment.segment.reportAsExternal(
                        DatastoreParameters
                            .product(productName)
                            .collection(collectionName)
                            .operation(name)
                            .noInstance()
                            .databaseName(collectionName)
                            .build()
                    )
                    segment.segment.endAsync()
                }
            }
        } else {
            block()
        }
    }
}

@Trace(async = true)
private suspend fun <T> runInAsyncTrace(block: suspend () -> T): T = block()