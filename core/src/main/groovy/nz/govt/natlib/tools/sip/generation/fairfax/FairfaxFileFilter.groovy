package nz.govt.natlib.tools.sip.generation.fairfax

import nz.govt.natlib.tools.sip.generation.FileFilter

class FairfaxFileFilter implements FileFilter {
    ResourceParameters resourceParameters

    FairfaxFileFilter(ResourceParameters resourceParameters) {
        this.resourceParameters = resourceParameters
    }

    boolean matches(File file) {
        String filename = file.getName()
        if (filename.startsWith(resourceParameters.filenamePrefix)) {
            return true
        }
        return false
    }
}
