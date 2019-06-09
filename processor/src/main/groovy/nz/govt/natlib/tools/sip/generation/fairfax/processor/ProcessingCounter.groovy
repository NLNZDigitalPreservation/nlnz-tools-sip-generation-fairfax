package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovy.transform.Canonical
import groovy.util.logging.Log4j2

import java.util.concurrent.locks.ReentrantLock

@Log4j2
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
