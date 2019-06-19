package nz.govt.natlib.tools.sip.generation.fairfax.special

import nz.govt.natlib.tools.sip.utils.FileUtils

class PageUnavailableWriter {
    static final String PAGE_NOT_AVAILABLE_PDF_RESOURCE = "page-unavailable.pdf"
    static final String TEMPORARY_DIRECTORY_PREFIX = "For-Page-Unavailable-Pdf-File_"

    static File writeToToTemporaryDirectory(String filename = PAGE_NOT_AVAILABLE_PDF_RESOURCE,
                                            File parentDirectory = null) {
        String resourcePath = ""
        boolean deleteOnExit = true
        File tempFile = FileUtils.writeResourceToTemporaryDirectory(filename, TEMPORARY_DIRECTORY_PREFIX, resourcePath,
                PAGE_NOT_AVAILABLE_PDF_RESOURCE, parentDirectory, deleteOnExit)

        return tempFile
    }
}
