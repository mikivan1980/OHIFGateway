package com.mikivan.service;


        import java.io.File;
        import java.io.IOException;
        import java.util.ArrayList;

        import org.dcm4che3.data.Attributes;
        import org.dcm4che3.data.Attributes.*;
        import org.dcm4che3.data.Tag;
        //import org.dcm4che3.data.VR;
        import org.dcm4che3.io.DicomInputStream;
        import org.dcm4che3.io.DicomOutputStream;
        import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
        import org.dcm4che3.tool.common.CLIUtils;
        import org.dcm4che3.util.AttributesFormat;
        import org.dcm4che3.util.SafeClose;



public class dcmModifier{

    private File storageDirectory;
    private AttributesFormat patternPathOverrideFile;
    private Attributes overrideAttributes = new Attributes();


    public dcmModifier(String[] overrideAttributesStr, String storageDirectoryStr, String patternPathOverrideFileStr) {

        CLIUtils.addAttributes(this.overrideAttributes, overrideAttributesStr);
        this.patternPathOverrideFile = new AttributesFormat(patternPathOverrideFileStr);
        storageDirectory = new File( storageDirectoryStr );
    }

    public boolean doModifier( File seedFile )  throws IOException {

        //File seedFile   = new File(inFile);
        //File outputFile = new File(outFile);

        Attributes seedAttributes = null;
        //Attributes metaAttributes = null;
        String tsuid;
        String iuid;
        //String cuid;

        //Attributes fmiOld = null;
        //Attributes fmi = null;

        DicomInputStream inDicomObject = null;

        try {
            inDicomObject = new DicomInputStream(seedFile); // here inDicomObject === inDicomFile
            inDicomObject.setIncludeBulkData(IncludeBulkData.URI);

            seedAttributes = inDicomObject.readDataset(-1, -1);
            //metaAttributes = inDicomObject.readFileMetaInformation();

            tsuid = inDicomObject.getTransferSyntax();
            iuid  = seedAttributes.getString(Tag.AffectedSOPInstanceUID);
            //cuid  = seedAttributes.getString(Tag.AffectedSOPClassUID);


//            fmiOld = din.readFileMetaInformation();
//
//            if (fmiOld == null || !fmiOld.containsValue(Tag.TransferSyntaxUID)
//                    || !fmiOld.containsValue(Tag.MediaStorageSOPClassUID)
//                    || !fmiOld.containsValue(Tag.MediaStorageSOPInstanceUID))
//                fmiOld = seedAttrs.createFileMetaInformation(din.getTransferSyntax());
        }
        catch(Exception e) {
            e.printStackTrace();
            SafeClose.close(inDicomObject);
            return false;
        }
        finally {
            SafeClose.close(inDicomObject);
        }

        DicomOutputStream outDicomObject = null;
        Attributes modified = new Attributes();

        System.out.println("[start]     seedAttrs.size() = " + seedAttributes.size());
        System.out.println("        overrideAttrs.size() = " + overrideAttributes.size());

//        if(!modified.isEmpty()) {
//            if(modified.contains(Tag.StudyInstanceUID)) {
//                studyIUID = modified.getString(Tag.StudyInstanceUID);
//          String seriesuid = UIDUtils.createUID();
//          seedAttrs.setString(Tag.SeriesInstanceUID, VR.UI, seriesuid);

        try {

            if(!overrideAttributes.isEmpty())
                seedAttributes.update(UpdatePolicy.OVERWRITE, overrideAttributes, modified );

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        System.out.println("             modified.size() = " + modified.size());
        System.out.println("[end]       seedAttrs.size() = " + seedAttributes.size());


        try {

            File readyFile = new File( storageDirectory,
                    patternPathOverrideFile == null
                            ? iuid
                            : patternPathOverrideFile.format( seedAttributes ));

            if( readyFile.getParentFile().mkdirs() ) {

                outDicomObject = new DicomOutputStream(readyFile);
                Attributes meta = seedAttributes.createFileMetaInformation(tsuid);
                outDicomObject.writeDataset(meta, seedAttributes);
            } else{
                System.out.println( "Error created directory..." + readyFile.getParent() );
            }

        } catch (IOException e) {
            e.printStackTrace();
            SafeClose.close(outDicomObject);
            return false;
        }
        finally{
            SafeClose.close(outDicomObject);
        }

        return true;
    }


    public void updateOverrideAttrs(String[] overrideAttrsStr) {

        this.overrideAttributes.removePrivateAttributes();
        CLIUtils.addAttributes(this.overrideAttributes, overrideAttrsStr);
    }


    public ArrayList<File> scanDirectory(String scanDirStr){
        ArrayList<File> findDirs  = new ArrayList<>();
        ArrayList<File> findFiles = new ArrayList<>();

        try {
            File root = new File(scanDirStr);

            if(root.isDirectory()) {
                findDirs.add(root);
            } else {
                System.out.println("[Error, root is NOT directory]/> " + scanDirStr );
                return findFiles;
            }
        } catch (Exception e){
            System.out.println( "[Error of root directory]/> " + scanDirStr + "  | e: " + e.toString() );
            return findFiles;
        }

        int count = 0;
        int ignorDirs =0;
        while ( count < findDirs.size() ){

            try {

                File[] buffer = findDirs.get(count).listFiles();

                for(File test : buffer){

                    if( test.isDirectory() ) findDirs.add(test);
                    if( test.isFile() )      findFiles.add(test);

                };
            } catch ( Exception e ){
                ignorDirs++;
                //System.out.println("[Error read of list files]" + findDirs.get(count).getAbsolutePath());
            }

            System.out.println( findDirs.size() + " | " + findFiles.size() + " | " + ignorDirs );

            count++;
        }



        return findFiles;
    }




    public static void main(String[] args) {

        try {

            String directory = "D://dop//java//OHIFGateway//";

            String seedPathFile = "_docs/test-dicom-file.dcm";
            //String outputPathFile = "_docs/MODIFIER_DICOM_FILE.dcm";


            String filepath = "_docs//{00080060}{00080020}//[{00100010}]-[ID_{00100020}]//{00080018}.dcm";


            String[] override = { "StudyDate", "20171004", "00080060", "MR"};//m

            dcmModifier main = new dcmModifier( override, directory, filepath);

            System.out.println( "Result of modifier is -> " + main.doModifier( new File(seedPathFile)) );



            String[] override2 = { "StudyDate", "20040826", "00080060", "CT", "00100010","Anon12345"};//m

            main.updateOverrideAttrs(override2);

            System.out.println( "Result of modifier is -> " + main.doModifier( new File(seedPathFile)) );


        } catch (Exception e) {
            System.err.println("[ERROR] dcmModifier: " + e.getMessage());
            System.exit(2);
        }
    }


}