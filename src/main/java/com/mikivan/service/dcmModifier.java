package com.mikivan.service;


        import java.io.*;
        import javax.json.*;
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

    private File sourceDirectory;
    private File storageDirectory;
    private File personalDirectory;
    private AttributesFormat patternPathOverrideFile;
    private Attributes overrideAttributes = new Attributes();


    public dcmModifier( File config_json ) throws IOException{

        InputStream in = new FileInputStream( config_json );

        JsonObject object = Json.createReader(in).readObject();

        this.sourceDirectory         = new File(object.getJsonObject("Path").getString("Source"));
        this.storageDirectory        = new File(object.getJsonObject("Path").getString("Destination"));
        this.patternPathOverrideFile = new AttributesFormat( object.getJsonObject("Path").getString("Pattern") );
        this.personalDirectory       = new File(object.getJsonObject("Path").getString("Personal"));

        System.out.println( "[JSON][Source Directory]/>   " + this.sourceDirectory.getAbsolutePath() );
        System.out.println( "[JSON][Storage Directory]/>  " + this.storageDirectory.getAbsolutePath() );
        System.out.println( "[JSON][Pattern Path]/>       " + this.patternPathOverrideFile.toString() );
        System.out.println( "[JSON][Personal Directory]/> " + this.personalDirectory.getAbsolutePath() );


        int n = object.getJsonArray("Attributes").size();
        for(int i = 0; i < n ; i++ ){
            String Tag    = object.getJsonArray("Attributes").getJsonObject(i).getString("Tag");
            String Value  = object.getJsonArray("Attributes").getJsonObject(i).getString("Value");
            String Action = object.getJsonArray("Attributes").getJsonObject(i).getString("Action");

            if(Action.equals("Override")) {

                String[] attr = {Tag, Value};

                CLIUtils.addAttributes(this.overrideAttributes, attr);
            }

            System.out.println("[JSON][Attributes]/> " + Tag + " | " + Value + " | " + Action);

        }

        //System.out.println("[this.overrideAttributes.size()]/> " + this.overrideAttributes.size());

    }


//    public void dcmModifier(String[] overrideAttributesStr, String storageDirectoryStr, String patternPathOverrideFileStr) {
//
//        CLIUtils.addAttributes(this.overrideAttributes, overrideAttributesStr);
//        this.patternPathOverrideFile = new AttributesFormat(patternPathOverrideFileStr);
//        storageDirectory = new File( storageDirectoryStr );
//    }



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
            System.out.println("[ERROR][file is not dicom or in file is not find]/> ");
            //e.printStackTrace();
            SafeClose.close(inDicomObject);
            return false;
        }
        finally {
            SafeClose.close(inDicomObject);
        }

        DicomOutputStream outDicomObject = null;
        Attributes modified = new Attributes();

        int X1 = seedAttributes.size(), Y1 = overrideAttributes.size();

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

        int X2 = modified.size(), Y2 = seedAttributes.size();

        System.out.println("[Sta]/> ( " + X1 + ", " + Y1 + ") -> ( " + X2 + ", " + Y2 + ")");

        for (int i = 0; i < modified.size(); i++)
            System.out.println("[MODIFIED]/> " +
                    modified.getValue( modified.tags()[i] ) + " -> " +
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
                System.out.println( "[ERROR][directory is NOT created!]/> " + readyFile.getParent() );
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


    public void updateOverrideAttributes(String[] overrideAttributesStr) {

        /* //Example
        *
        * dcmModifier main = new dcmModifier( config );
        *
        * String[] override2 = { "StudyDate", "20040826", "00080060", "CT", "00100010","Anon12345"};
        *
        * main.updateOverrideAttributes(override2);
        *
        * System.out.println( "Result of modifier is -> " + main.doModifier( new File(seedPathFile)) );
        *
        * */
        this.overrideAttributes.removePrivateAttributes();
        CLIUtils.addAttributes(this.overrideAttributes, overrideAttributesStr);
    }


    public void scanDirectory(){

        ArrayList<File> find = new ArrayList<>();

        try {
            if(this.sourceDirectory.isDirectory()) {
                find.add(this.sourceDirectory);
            } else {
                System.out.println(
                        "[ERROR][this.sourceDirectory is NOT directory]/> " + this.sourceDirectory.getAbsolutePath());
                return;
            }
        } catch (Exception e){
            System.out.println( "[ERROR][scanDirectory]/> " + e.toString() );
            return;
        }

        int item = 0, count = 0, ignoreDirs = 0;
        while ( item < find.size() ){

            try {

                if( find.get(item).isDirectory() ){

                    for(File test : find.get(item).listFiles()) {

                        if (test.isDirectory()) find.add(test);

                        if (test.isFile()) {

                            //test.isDicom();
                            this.doModifier(test);
                            count++;

                        }
                    }
                };
            } catch ( Exception e ){
                ignoreDirs++;
            }

            System.out.print("\r" + item + " | " + find.size() + " | " + count + " | " + ignoreDirs );

            item++;
        }
        System.out.println( "\n" + item + " | " + find.size() + " | " + count + " | " + ignoreDirs + "\r");
        return;
    }




    public static void main(String[] args) {

        try {
            /*
            * первый аргумент командной строк - путь к файлу config.json
            * проверяем, доступен ли этот файл для чтения, если да - можно сделать валидацию параметров файла (не сделано)
            * если файл конфигурации доступен - доступен - подаем его в конструктор класса new dcmModifier()
            * в конструкторе читаются данные из json конфига и размещаются в соответствующие поля класса.
            *
            * */

            File config = new File(args[0]);

            if( config.exists() ){

                dcmModifier main = new dcmModifier( config );

                main.scanDirectory();

//  For Example-1
//                String seedPathFile = "d:\\dop\\java\\OHIFGateway\\_docs\\dcm_in\\test-dicom-file.dcm";//"_docs/test-dicom-file.dcm";
//                System.out.println( "Result of modifier is -> " + main.doModifier( new File(seedPathFile)) );
//
//  For Example-2
//                String[] override2 = { "StudyDate", "20040826", "00080060", "CT", "00100010","Anon12345"};//m
//                main.updateOverrideAttributes(override2);
//                System.out.println( "Result of modifier is -> " + main.doModifier( new File(seedPathFile)) );

            } else {
                System.out.println("[ERROR][json config file is NOT exists!]/> " + args[0]);
            }


        } catch (Exception e) {
            System.err.println("[ERROR][dcmModifier]/> " + e.getMessage());
            System.exit(2);
        }
    }


}