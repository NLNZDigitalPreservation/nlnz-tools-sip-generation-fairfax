package nz.govt.natlib.tools.sip.generation.fairfax.processor

import groovyx.gpars.GParsExecutorsPool
import groovyx.gpars.GParsPool
import org.junit.Ignore
import org.junit.Test

class MiscellaneousProcessorTest {

    // Shows how a collection is processed by GParsExecutorsPool: the collection is processed in random order.
    @Test
    @Ignore
    void showParallelProcessingGParsPool() {

        List<String> someValueList = [ ]
        int maximum = 99
        int index = 0
        while (index++ < maximum) {
            someValueList.add("${index}_-_${Math.random()}")
        }

        GParsPool.withPool(4) {
            Random random = new Random()
            someValueList.eachParallel { String value ->
                int randomCount = random.nextDouble() * 350
                println("${value}, randomCount is ${randomCount}")
                File tempFile = File.createTempFile("Random-file-${value}_with_${randomCount}_lines_", ".txt")
                tempFile.deleteOnExit()
                tempFile.write(value)
                for (int i = 0; i < randomCount; i++) {
                    tempFile.write("${i}")
                }
            }
        }
    }

    // Shows how a collection is processed by GParsExecutorsPool: the collection is processed in sequence.
    @Test
    @Ignore
    void showParallelProcessingGParsExecutorsPool() {

        List<String> someValueList = [ ]
        int maximum = 99
        int index = 0
        while (index++ < maximum) {
            someValueList.add("${index}_-_${Math.random()}")
        }

        GParsExecutorsPool.withPool(4) {
            Random random = new Random()
            someValueList.eachParallel { String value ->
                int randomCount = random.nextDouble() * 350
                println("${value}, randomCount is ${randomCount}")
                File tempFile = File.createTempFile("Random-file-${value}_with_${randomCount}_lines_", ".txt")
                tempFile.deleteOnExit()
                tempFile.write(value)
                for (int i = 0; i < randomCount; i++) {
                    tempFile.write("${i}")
                }
            }
        }
    }
}
