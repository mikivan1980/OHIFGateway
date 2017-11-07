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

        Attributes seedAttributes = null;
        //Attributes metaAttributes = null;
        String tsuid;
        String iuid;

        DicomInputStream inDicomObject = null;

        try {

            inDicomObject = new DicomInputStream(seedFile); // here inDicomObject === inDicomFile
            inDicomObject.setIncludeBulkData(IncludeBulkData.URI);

            seedAttributes = inDicomObject.readDataset(-1, -1);
            //metaAttributes = inDicomObject.readFileMetaInformation();

            tsuid = inDicomObject.getTransferSyntax();
            iuid  = seedAttributes.getString(Tag.AffectedSOPInstanceUID);
        }
        catch(Exception e) {
            System.out.println("this in file is not dicom file!!!! or in file is not find");
            //e.printStackTrace();
            SafeClose.close(inDicomObject);
            return false;
        }
        finally {
            SafeClose.close(inDicomObject);
        }

        DicomOutputStream outDicomObject = null;
        Attributes modified = new Attributes();

        // ()
        System.out.println("[start] seedAttributes.size() = " + seedAttributes.size());
        System.out.println("    overrideAttributes.size() = " + overrideAttributes.size());

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

        System.out.println("              modified.size() = " + modified.size());
        System.out.println("[end]   seedAttributes.size() = " + seedAttributes.size());

        for (int i = 0; i < modified.size(); i++)
            System.out.println( modified.getValue( modified.tags()[i] ) + " -> " +
                      overrideAttributes.getValue( modified.tags()[i] )
            );


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
                System.out.println( "[Error created directory]/> " + readyFile.getParent() );
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


    public void scanDirectory(String scanDirStr){

        ArrayList<File> find = new ArrayList<>();

        try {
            File root = new File(scanDirStr);

            if(root.isDirectory()) {
                find.add(root);
            } else {
                System.out.println("[Error, root is NOT directory]/> " + scanDirStr );
                return;
            }
        } catch (Exception e){
            System.out.println( "[Error of root directory]/> " + scanDirStr + "  | e: " + e.toString() );
            return;
        }

        int item = 0;
        int count = 0;
        int ignorDirs =0;
        while ( item < find.size() ){

            try {

                if( find.get(item).isDirectory() ){

                    for(File test : find.get(item).listFiles()) {

                        if (test.isDirectory()) find.add(test);

                        if (test.isFile()) {

                            //test.isDicom();
                            count++;

                        }
                    }
                };
            } catch ( Exception e ){
                ignorDirs++;
            }

            System.out.print("\r" + item + " | " + find.size() + " | " + count + " | " + ignorDirs );

            item++;
        }
        System.out.println( item + " | " + find.size() + " | " + count + " | " + ignorDirs + "\r");

        return;
    }




    public static void main(String[] args) {

        try {

            String directory = "D://dop//java//OHIFGateway//";
            String filepath  = "_docs//{00080060}{00080020}//[{00100010}]-[ID_{00100020}]//{00080018}.dcm";

            String seedPathFile = "_docs/xslt-for-ohif.txt";//"_docs/test-dicom-file.dcm";


            String[] override = { "StudyDate", "20171004", "00080060", "MR"};//m

            dcmModifier main = new dcmModifier( override, directory, filepath);

            System.out.println( "Result of modifier is -> " + main.doModifier( new File(seedPathFile)) );



            String[] override2 = { "StudyDate", "20040826", "00080060", "CT", "00100010","Anon12345"};//m

            main.updateOverrideAttrs(override2);

            System.out.println( "Result of modifier is -> " + main.doModifier( new File(seedPathFile)) );


            main.scanDirectory("d:\\dop\\");


        } catch (Exception e) {
            System.err.println("[ERROR] dcmModifier: " + e.getMessage());
            System.exit(2);
        }
    }


}