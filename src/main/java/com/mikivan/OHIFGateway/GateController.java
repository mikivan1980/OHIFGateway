package com.mikivan.OHIFGateway;


    //import org.slf4j.Logger;
    //import org.slf4j.LoggerFactory;
    import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
    import org.springframework.http.*;
    import org.springframework.stereotype.Controller;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RequestMethod;
    import org.springframework.web.bind.annotation.RequestParam;
    import org.springframework.web.bind.annotation.ResponseBody;
    import org.springframework.web.bind.annotation.PathVariable;

    import java.io.IOException;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.nio.file.Paths;
    import java.util.ArrayList;
    import java.util.Map;

    import com.mikivan.service.CFindSCU;
    import com.mikivan.service.CStoreSCP;
    import com.mikivan.service.CMoveSCU;


@Controller
@EnableAutoConfiguration
public class GateController {

   private static String[] bindSCU   = new String[]{ "IVAN",    "192.168.0.74",    "49049"};  //строгий порядок
   private static String[] bindSCP   = new String[]{ "IVAN",    "192.168.0.74",    "4006"};   //строгий порядок
   private static String[] remoteSCP = new String[]{ "PACS01",  "192.168.0.35",    "4006"};   //строгий порядок

//    private static String[] bindSCU   = new String[]{"IVAN",    "192.168.121.101", "49049"};  //строгий порядок
//    private static String[] bindSCP   = new String[]{"IVAN",    "192.168.121.101", "4006"};   //строгий порядок
//    private static String[] remoteSCP = new String[]{"WATCHER", "192.168.121.100", "4006"};   //строгий порядок

    private static String[] optsFindSCU  = {};
    private static String[] optsMoveSCU  = {};
    private static String[] optsStoreSCP = {};

    //параметры взяты из метеора
    private static final String[] r_OPTS_FOR_STUDYES = new String[] {
            "0020000D", "00080020", "00080030", "00080050", "00080090", "00100010", "00100020",
            "00100030", "00100040", "00200010", "00201206", "00201208", "00081030", "00080060",
            "00080061"};

    private static final String[] r_OPTS_FOR_IMAGES  = new String[]{
            "00100010", "00100020", "00101010", "00101020", "00101030", "00080050", "00080020",
            "00080061", "00081030", "00201208", "0020000D", "00080080", "0020000E", "0008103E",
            "00080060", "00200011", "00080021", "00080031", "00080008", "00080016", "00080018",
            "00200013", "00200032", "00200037", "00200052", "00201041", "00280002", "00280004",
            "00280006", "00280010", "00280011", "00280030", "00280034", "00280100", "00280101",
            "00280102", "00280103", "00280106", "00280107", "00281050", "00281051", "00281052",
            "00281053", "00281054", "00200062", "00185101", "0008002A", "00280008", "00280009",
            "00181063", "00181065", "00180050", "00282110", "00282111", "00282112", "00282114",
            "00180086", "00180010" };


    //private static final Logger log = LoggerFactory.getLogger(GateController.class);

    private static CStoreSCP CStoreListenerSCP;


//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
    @RequestMapping(value = "/mikivan/c-store-scp/create", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> createStoreSCP( @RequestParam String password) {
        //192.168.121.101:8080/mikivan/c-store-scp/create?password=admin
        String toBrowser = "";

        if(password.equals("admin")){
            toBrowser = "Valid password! ";
            try{

                this.CStoreListenerSCP = new CStoreSCP(bindSCP, optsStoreSCP);

                toBrowser += "Service Store SCP is create and started!";

            } catch (Exception e) {

                toBrowser += "Service Store SCP is NOT create and NOT started!";
                System.err.println("c-store-scp: " + e.getMessage());
                e.printStackTrace();
            }

        } else{
            toBrowser = "No valid password!";
        }

        //ответ на запрос <ip>:8080/mikivan/c-store-scp/create?password=admin
        HttpHeaders hdrs = new HttpHeaders();
        final MediaType mediaType = MediaType.TEXT_PLAIN;
        hdrs.setContentType(mediaType);

        final ResponseEntity<String> rsp = new ResponseEntity<String>(toBrowser, hdrs, HttpStatus.OK);

        return rsp;

    }

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
    @RequestMapping(value = "/mikivan/c-store-scp/stop", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> stopStoreSCP( @RequestParam String password) {
        //192.168.121.101:8080/mikivan/c-store-scp/stop?password=admin
        String toBrowser = "";

        if(password.equals("admin")){
            toBrowser = "Valid password! ";
            try{

                this.CStoreListenerSCP.stop();

                toBrowser += "Service Store SCP is stoped!";

            } catch (Exception e) {

                toBrowser += "Service Store SCP is NOT stoped!";
                System.err.println("c-store-scp: " + e.getMessage());
                e.printStackTrace();
            }

        } else{
            toBrowser = "No valid password!";
        }

        //ответ на запрос <ip>:8080/mikivan/c-store-scp/start?password=admin
        HttpHeaders hdrs = new HttpHeaders();
        final MediaType mediaType = MediaType.TEXT_PLAIN;
        hdrs.setContentType(mediaType);

        final ResponseEntity<String> rsp = new ResponseEntity<String>(toBrowser, hdrs, HttpStatus.OK);

        return rsp;

    }

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
    @RequestMapping(value = "/mikivan/c-store-scp/start", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> startStoreSCP( @RequestParam String password) {
        //192.168.121.101:8080/mikivan/stop?password=admin
        String toBrowser = "";

        if(password.equals("admin")){
            toBrowser = "Valid password! ";
            try{

                this.CStoreListenerSCP.start();


                toBrowser += "Service Store SCP is start!";

            } catch (Exception e) {

                toBrowser += "Service Store SCP is NOT start!";
                System.err.println("c-store-scp: " + e.getMessage());
                e.printStackTrace();
            }

        } else{
            toBrowser = "No valid password!";
        }

        //ответ на запрос <ip>:8080/mikivan/start?password=admin
        HttpHeaders hdrs = new HttpHeaders();
        final MediaType mediaType = MediaType.TEXT_PLAIN;
        hdrs.setContentType(mediaType);

        final ResponseEntity<String> rsp = new ResponseEntity<String>(toBrowser, hdrs, HttpStatus.OK);

        return rsp;

    }

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
    @RequestMapping( value = "/mikivan/studies", method = RequestMethod.GET )
    @ResponseBody
    public ResponseEntity<String> getStudyList(
            @RequestParam Map<String, String> queryMap ) {

//onCoreViewer/Packages/ohif-study-list/server/services/qido/studies.js:20
//PatientName, PatientID, AccessionNumber, StudyDescription, ModalitiesInStudy, StudyDate
//
//limit         игнорируем (а многоли включать данных в ответ)
//includefield  игнорируем (какие еще поля  вернуть, мы вернем необходимый минимум)

        //String[] m      = { "StudyDate", "20171004-20171004", "ModalitiesInStudy", "CT"};
        ArrayList<String> mOptsQuery = new ArrayList<String>();

        for( Map.Entry<String, String> entry : queryMap.entrySet() ){

            if(  (!entry.getKey().equals("limit")) && (!entry.getKey().equals("includefield"))  ){

                mOptsQuery.add(entry.getKey()); mOptsQuery.add(entry.getValue());

                System.out.println("m: " + entry.getKey() + "=" + entry.getValue());
            }
        }

        String[] m = new String[mOptsQuery.size()];

        mOptsQuery.toArray(m);


//http://192.168.0.74:8080/mikivan/studies?PatientName=Mikh*&ModalitiesInStudy=CT&includefield=00081030%2C00080060

        String toViewStudyList =  "";

        try {

            CFindSCU main = new CFindSCU( bindSCU, remoteSCP, optsFindSCU, "xslt/mikivan-studies.xsl","STUDY", m, r_OPTS_FOR_STUDYES);

            // так вместо xml будет бинарный выхлоп
            main.setXML(false);

            String jsonOutput = main.doFind();

            if( jsonOutput == null ){
                System.out.println("jsonOutput is null");
            }
            else {
                System.out.println("jsonOutput is created");

                toViewStudyList =  toViewStudyList + "[" + jsonOutput + "{}]";
            }
        } catch (Exception e) {
            System.err.println("c-find-scu: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }


        //ответ на запрос /mikivan/studylist
        HttpHeaders hdrs = new HttpHeaders();
        hdrs.setContentType(MediaType.APPLICATION_JSON);

        final ResponseEntity<String> rsp = new ResponseEntity<String>(toViewStudyList, hdrs, HttpStatus.OK);

        return rsp;
    }


//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
    @RequestMapping( value = "/mikivan/studies/{studyUID}/metadata", method = RequestMethod.GET )
    @ResponseBody
    public ResponseEntity<String> getStudyMetaData( @PathVariable String studyUID ) {

        //http://192.168.0.74:8080/mikivan/studies/1.2.840.113704.1.111.4156.1367430813.2/metadata

        //log.info("/mikivan/studies/{studyUID}/metadata");
        System.out.println("/mikivan/studies/{studyUID}/metadata = " + "/mikivan/studies/" + studyUID + "/metadata");

        String[] m = {"0020000D", studyUID};

        String toViewMetaData =  "";

        try {

            CFindSCU main = new CFindSCU( bindSCU, remoteSCP, optsFindSCU, "xslt/mikivan-metadata.xsl","IMAGE", m, r_OPTS_FOR_IMAGES);

            String jsonOutput = main.doFind();

            if( jsonOutput == null ){
                System.out.println("jsonOutput is null");
            }
            else {
                System.out.println("jsonOutput is created");

                toViewMetaData = toViewMetaData + "[" + jsonOutput + "{}]";
            }
        } catch (Exception e) {
            System.err.println("findscu: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }


        //готовим ответ на запрос
        HttpHeaders hdrs = new HttpHeaders();
        hdrs.setContentType(MediaType.APPLICATION_JSON);

        final ResponseEntity<String> rsp = new ResponseEntity<String>(toViewMetaData, hdrs, HttpStatus.OK);

        return rsp;
    }


//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
    @RequestMapping(value = "/mikivan/wado", method = RequestMethod.GET)
    @ResponseBody
    public HttpEntity<byte[]> getInstance(
            @RequestParam Map<String, String> queryMap) throws IOException {

        System.out.println("/mikivan/wado");

        final Path path = Paths.get("d:\\dop\\java\\OHIFGateway\\_docs\\test-dicom-file.dcm");
        final String fileName = path.getFileName().toString();

        byte[] documentBody = Files.readAllBytes(path);

        System.out.println("documentBody.length = " + documentBody.length);

        HttpHeaders header = new HttpHeaders();
        header.set(HttpHeaders.CONTENT_TYPE, "application/dicom");
        header.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,"*");
        header.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + fileName.replace(" ", "_"));
        header.setContentLength(documentBody.length);

        return new HttpEntity<byte[]>(documentBody, header);
    }




}
