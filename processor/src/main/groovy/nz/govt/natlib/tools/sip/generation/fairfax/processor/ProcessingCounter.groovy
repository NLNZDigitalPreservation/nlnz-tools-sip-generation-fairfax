package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.transform.Canonical
import groovy.util.logging.Slf4j

import java.util.concurrent.locks.ReentrantLock

@Slf4j
@Canonical
class ProcessingCounter {
    long currentCount = 0
    long total = 0

    ReentrantLock counterLock = new ReentrantLock()

    long incrementCounter() {
        counterLock.lock()
        try {
            currentCount += 1
            total += 1
        } finally {
            counterLock.unlock()
        }
    }
}
