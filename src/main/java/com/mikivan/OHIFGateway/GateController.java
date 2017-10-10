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

    //import org.dcm4che3.tool.findscu.FindSCU;
    import com.mikivan.service.CFindSCU;
    //import com.mikivan.service.CStoreSCP;
    //import com.mikivan.service.CMoveSCU;


@Controller
@EnableAutoConfiguration
public class GateController {

    //private static final Logger log = LoggerFactory.getLogger(GateController.class);




    @RequestMapping( value = "/mikivan/studies", method = RequestMethod.GET )
    @ResponseBody
    public ResponseEntity<String> getStudyList(
            @RequestParam Map<String, String> queryMap ) {

//        String[] bind   = { "IVAN",    "192.168.121.101", "49049"};//строгий порядок
//        String[] remote = { "WATCHER", "192.168.121.100", "4006"};//строгий порядок
        String[] bind   = { "IVAN",   "192.168.0.74", "4006"};//строгий порядок
        String[] remote = { "PACS01", "192.168.0.35", "4006"};//строгий порядок

        String[] opts   = {};
        //String[] m      = { "StudyDate", "20171004-20171004", "ModalitiesInStudy", "CT"};

        //параметры взяты из метеора
        String[] r      = {"0020000D", "00080020", "00080030", "00080050", "00080090", "00100010", "00100020",
                           "00100030", "00100040", "00200010", "00201206", "00201208", "00081030", "00080060",
                           "00080061"};

//onCoreViewer/Packages/ohif-study-list/server/services/qido/studies.js:20
//PatientName, PatientID, AccessionNumber, StudyDescription, ModalitiesInStudy, StudyDate
//
//limit         игнорируем (а многоли включать данных в ответ)
//includefield  игнорируем (какие еще поля  вернуть, мы вернем необходимый минимум)

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

            CFindSCU main = new CFindSCU(bind, remote, opts, "xslt/mikivan-studies.xsl","STUDY", m, r);

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
        final MediaType mediaType = MediaType.APPLICATION_JSON;
        hdrs.setContentType(mediaType);

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

//        String[] bind   = { "IVAN",    "192.168.121.101", "49049"};//строгий порядок
//        String[] remote = { "WATCHER", "192.168.121.100", "4006"};//строгий порядок
        String[] bind   = { "IVAN",   "192.168.0.74", "4006"};//строгий порядок
        String[] remote = { "PACS01", "192.168.0.35", "4006"};//строгий порядок

        String[] opts = {};
        String[] m    = {"0020000D", studyUID};

        //параметры взяты из метеора
        String[] r = {"00100010", "00100020", "00101010", "00101020", "00101030", "00080050", "00080020",
                      "00080061", "00081030", "00201208", "0020000D", "00080080", "0020000E", "0008103E",
                      "00080060", "00200011", "00080021", "00080031", "00080008", "00080016", "00080018",
                      "00200013", "00200032", "00200037", "00200052", "00201041", "00280002", "00280004",
                      "00280006", "00280010", "00280011", "00280030", "00280034", "00280100", "00280101",
                      "00280102", "00280103", "00280106", "00280107", "00281050", "00281051", "00281052",
                      "00281053", "00281054", "00200062", "00185101", "0008002A", "00280008", "00280009",
                      "00181063", "00181065", "00180050", "00282110", "00282111", "00282112", "00282114",
                      "00180086", "00180010" };


        String toViewMetaData =  "";

        try {

            CFindSCU main = new CFindSCU(bind, remote, opts, "xslt/mikivan-metadata.xsl","IMAGE", m, r);

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
        final MediaType mediaType = MediaType.APPLICATION_JSON;
        hdrs.setContentType(mediaType);

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

        HttpHeaders header = new HttpHeaders();
        header.set("Content-Type", "application/dicom");
        header.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + fileName.replace(" ", "_"));
        header.setContentLength(documentBody.length);

        return new HttpEntity<byte[]>(documentBody, header);
    }




}
