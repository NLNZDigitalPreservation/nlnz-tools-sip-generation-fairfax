package nz.govt.natlib.tools.sip.generation.fairfax.processor

class ProcessorException extends RuntimeException {
    ProcessorException() {
        super()
    }

    ProcessorException(String message) {
        super(message)
    }

    ProcessorException(String message, Throwable cause) {
        super(message, cause)
    }

    ProcessorException(Throwable cause) {
        super(cause)
    }

    protected ProcessorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace)
    }
}
