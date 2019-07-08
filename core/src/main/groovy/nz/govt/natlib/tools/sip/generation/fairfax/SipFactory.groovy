package nz.govt.natlib.tools.sip.generation.fairfax

import nz.govt.natlib.tools.sip.IEEntityType
import nz.govt.natlib.tools.sip.Sip
import nz.govt.natlib.tools.sip.SipFileWrapperFactory

import java.nio.file.Path

/**
 * Generates a {@link Sip} based on the given parameters.
 */
class SipFactory {
    static String TITLE_PARENT_KEY = "title_parent"
    static String TITLE_METS_KEY = "title_mets"
    static String ALMA_MMS_ID_KEY = "MMSID"
    static String POLICY_ID_KEY = "Access"
    static String PRESERVATION_TYPE = "PRESERVATION_MASTER"
    static String IS_MAGAZINE_KEY = "Magazine"
    static String DEFAULT_USAGE_TYPE = Sip.USAGE_TYPE_VIEW
    static Integer DEFAULT_REVISION_NUMBER = 1
    static Boolean DEFAULT_DIGITAL_ORIGINAL = true

    static Sip fromMap(Map<String, String> parameterMap, List<File> files = [ ], boolean useFilenameOnly = false,
                       boolean generateMD5Hash = false, String titleKey = TITLE_PARENT_KEY) {
        String title = parameterMap.get(titleKey)
        String almaMmsId = parameterMap.get(ALMA_MMS_ID_KEY)
        String policyId = parameterMap.get(POLICY_ID_KEY)

        Sip sip = new Sip()
        sip.title = title

        String magazineValue = parameterMap.get(IS_MAGAZINE_KEY)
        switch (magazineValue) {
            case ("1"):
                sip.ieEntityType = IEEntityType.MagazineIE
                break
            case ("0"):
                sip.ieEntityType = IEEntityType.NewspaperIE
                break
            default:
                sip.ieEntityType = IEEntityType.UNKNOWN
                break
        }
        sip.objectIdentifierType = Sip.OBJECT_IDENTIFIER_TYPE_ALMA_MMS
        sip.objectIdentifierValue = almaMmsId
        sip.policyId = policyId
        sip.preservationType = PRESERVATION_TYPE
        sip.usageType = DEFAULT_USAGE_TYPE
        sip.revisionNumber = DEFAULT_REVISION_NUMBER
        sip.digitalOriginal = DEFAULT_DIGITAL_ORIGINAL

        sip.fileWrappers = [ ]
        files.each { Path file ->
            Sip.FileWrapper fileWrapper = SipFileWrapperFactory.generate(file, useFilenameOnly, generateMD5Hash)
            sip.fileWrappers.add(fileWrapper)
        }

        return sip
    }
}
