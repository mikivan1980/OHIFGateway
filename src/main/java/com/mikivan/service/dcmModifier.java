package com.mikivan.service;


import java.io.File;
        import java.io.FileInputStream;
        import java.io.IOException;


        import org.apache.commons.cli.Options;
        //import org.apache.commons.cli.ParseException;
        import org.dcm4che3.data.Attributes;
        import org.dcm4che3.data.Attributes.*;
        import org.dcm4che3.data.Tag;
        //import org.dcm4che3.data.VR;
        import org.dcm4che3.io.DicomInputStream;
        import org.dcm4che3.io.DicomOutputStream;
        import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
        import org.dcm4che3.tool.common.CLIUtils;
        import org.dcm4che3.util.SafeClose;


public class dcmModifier{

    private Attributes overrideAttrs = new Attributes();


     public dcmModifier(String[] overrideAttrsStr) {

        CLIUtils.addAttributes(this.overrideAttrs, overrideAttrsStr);
    }

    public boolean doModifier(String inFile, String outFile)  throws IOException {

        File seedFile   = new File(inFile);
        File outputFile = new File(outFile);

        Attributes seedAttrs = null;
        Attributes fmiOld = null;
        Attributes fmi = null;

        DicomInputStream din = null;

        try {
            din = new DicomInputStream(seedFile);
            din.setIncludeBulkData(IncludeBulkData.URI);

            seedAttrs = din.readDataset(-1, -1);
            fmiOld = din.readFileMetaInformation();

            if (fmiOld == null || !fmiOld.containsValue(Tag.TransferSyntaxUID)
                    || !fmiOld.containsValue(Tag.MediaStorageSOPClassUID)
                    || !fmiOld.containsValue(Tag.MediaStorageSOPInstanceUID))
                fmiOld = seedAttrs.createFileMetaInformation(din.getTransferSyntax());
        }
        catch(Exception e) {
            e.printStackTrace();
            SafeClose.close(din);
            return false;
        }
        finally {
            SafeClose.close(din);
        }

        DicomOutputStream dout = null;
        Attributes modified = new Attributes();

        System.out.println("[start]     seedAttrs.size() = " + seedAttrs.size());
        System.out.println("        overrideAttrs.size() = " + overrideAttrs.size());

//        if(!modified.isEmpty()) {
//            if(modified.contains(Tag.StudyInstanceUID)) {
//                studyIUID = modified.getString(Tag.StudyInstanceUID);
//          String seriesuid = UIDUtils.createUID();
//          seedAttrs.setString(Tag.SeriesInstanceUID, VR.UI, seriesuid);

        try {

            if(!overrideAttrs.isEmpty())
                seedAttrs.update(UpdatePolicy.OVERWRITE, overrideAttrs, modified );

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        System.out.println("             modified.size() = " + modified.size());
        System.out.println("[end]       seedAttrs.size() = " + seedAttrs.size());


        try {

            dout = new DicomOutputStream(outputFile);
            fmi = seedAttrs.createFileMetaInformation(fmiOld.getString(Tag.TransferSyntaxUID));
            dout.writeDataset(fmi, seedAttrs);

        } catch (IOException e) {
            e.printStackTrace();
            SafeClose.close(dout);
            return false;
        }
        finally{
            SafeClose.close(dout);
        }

        return true;


    }


    public void updateOverrideAttrs(String[] overrideAttrsStr) {

        this.overrideAttrs.removePrivateAttributes();

        CLIUtils.addAttributes(this.overrideAttrs, overrideAttrsStr);

    }


    public static void main(String[] args) {

        try {



            String seedPathFile = "_docs/test-dicom-file.dcm";
            String outputPathFile = "_docs/MODIFIER_DICOM_FILE.dcm";


            String[] override = { "StudyDate", "20171004", "00080060", "MR"};//m

            dcmModifier main = new dcmModifier(override);

            System.out.println("Result of modifier is -> " + main.doModifier(seedPathFile, outputPathFile));



            String[] override2 = { "StudyDate", "20040826", "00080060", "CT", "00100010","Anon12345"};//m

            main.updateOverrideAttrs(override2);

            System.out.println("Result of modifier is -> " + main.doModifier(seedPathFile, outputPathFile));



        } catch (Exception e) {
            System.err.println("[ERROR] dcmModifier: " + e.getMessage());
            System.exit(2);
        }
    }


}